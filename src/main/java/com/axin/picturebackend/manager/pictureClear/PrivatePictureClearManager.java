package com.axin.picturebackend.manager.pictureClear;

import com.axin.picturebackend.constant.PictureConstant;
import com.axin.picturebackend.manager.CosManager;
import com.axin.picturebackend.model.Enum.SpaceTypeEnum;
import com.axin.picturebackend.model.entity.Picture;
import com.axin.picturebackend.model.entity.Space;
import com.axin.picturebackend.service.PictureService;
import com.axin.picturebackend.service.SpaceService;
import com.axin.picturebackend.service.SysNoticeService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 私有空间定时清理管理器
 *
 * <p>分两个级别对私有空间中的图片进行清理：</p>
 * <ul>
 *   <li>一级：超容量淘汰 — 空间图片数量超过上限时，按创建时间升序删除最旧的图片，直到数量降至阈值以下</li>
 *   <li>二级：冷门清理 — 清理访问量极低且体积很小、长时间未被访问的低价值图片</li>
 * </ul>
 */
@Slf4j
@Component
public class PrivatePictureClearManager {

    /** 超容量淘汰触发比例：空间使用率超过此比例时触发一级清理（90%） */
    private static final double OVERFLOW_RATIO = 0.9;

    /** 二级清理：图片冷门天数（180天） */
    private static final long COLD_DAYS_MS = 180L * 24 * 60 * 60 * 1000;

    /** 二级清理：冷门访问量上限 */
    private static final long COLD_VIEW_THRESHOLD = 2L;

    /** 二级清理：小文件阈值（1 MB），仅清理小文件，避免误删用户珍贵大图 */
    private static final long SMALL_FILE_BYTES =(long) 1024 * 1024;

    @Resource
    private SpaceService spaceService;

    @Resource
    private PictureService pictureService;

    @Resource
    private CosManager cosManager;

    @Resource
    private SysNoticeService sysNoticeService;

    // ==================== 定时任务 ====================

    /**
     * 一级清理：每天凌晨 2 点，对容量使用率 >= 90% 的私有空间进行超容量淘汰
     * <p>按创建时间升序删除最旧的图片，直到使用率降至 80% 以下</p>
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void firstLevelClear() {
        log.info("[私有空间清理] 一级清理开始：超容量淘汰");
        List<Space> overflowSpaces = listOverflowSpaces();
        if (overflowSpaces.isEmpty()) {
            log.info("[私有空间清理] 无超容量空间，跳过");
            return;
        }
        log.info("[私有空间清理] 发现 {} 个超容量空间，开始逐一处理", overflowSpaces.size());
        for (Space space : overflowSpaces) {
            evictOldestPictures(space);
        }
        log.info("[私有空间清理] 一级清理完成");
    }

    /**
     * 二级清理：每周日凌晨 3 点，清理私有空间中长期冷门的小图片
     * <p>条件：访问量 &lt; 2 且创建时间超过 180 天 且文件体积 &lt; 1MB</p>
     */
    @Scheduled(cron = "0 0 3 * * SUN")
    public void secondLevelClear() {
        log.info("[私有空间清理] 二级清理开始：清理冷门小图片");
        // 查询所有私有空间 ID
        List<Long> privateSpaceIds = listPrivateSpaceIds();
        if (privateSpaceIds.isEmpty()) {
            log.info("[私有空间清理] 无私有空间，跳过");
            return;
        }
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<Picture>()
                .in("spaceId", privateSpaceIds)
                .lt("viewCount", COLD_VIEW_THRESHOLD)
                .lt("createTime", System.currentTimeMillis() - COLD_DAYS_MS)
                .lt("picSize", SMALL_FILE_BYTES);
        clearPictures(queryWrapper, "冷门小图片");
    }

    // ==================== 私有方法 ====================

    /**
     * 查询使用率 >= 90% 的私有空间列表
     */
    private List<Space> listOverflowSpaces() {
        List<Space> allPrivateSpaces = spaceService.list(
                new QueryWrapper<Space>().eq("spaceType", SpaceTypeEnum.PRIVATE.getValue())
        );
        return allPrivateSpaces.stream()
                .filter(space -> space.getMaxCount() > 0
                        && (double) space.getTotalCount() / space.getMaxCount() >= OVERFLOW_RATIO)
                .collect(Collectors.toList());
    }

    /**
     * 查询所有私有空间的 ID 列表
     */
    private List<Long> listPrivateSpaceIds() {
        return spaceService.list(
                new QueryWrapper<Space>()
                        .eq("spaceType", SpaceTypeEnum.PRIVATE.getValue())
                        .select("id")
        ).stream().map(Space::getId).collect(Collectors.toList());
    }

    /**
     * 对单个超容量私有空间按创建时间升序淘汰最旧图片，直到使用率降至 80% 以下
     *
     * @param space 目标空间
     */
    private void evictOldestPictures(Space space) {
        long targetCount = (long) (space.getMaxCount() * 0.8);
        long toDelete = space.getTotalCount() - targetCount;
        if (toDelete <= 0) {
            return;
        }
        // 按创建时间升序取最旧的 toDelete 张图片
        List<Picture> oldestPictures = pictureService.list(
                new QueryWrapper<Picture>()
                        .eq("spaceId", space.getId())
                        .orderByAsc("createTime")
                        .last("LIMIT " + toDelete)
        );
        if (oldestPictures.isEmpty()) {
            return;
        }
        List<Long> ids = oldestPictures.stream().map(Picture::getId).collect(Collectors.toList());
        boolean removed = pictureService.removeByIds(ids);
        if (!removed) {
            log.warn("[私有空间清理] 空间 {} 超容量淘汰失败", space.getId());
            return;
        }
        // 更新空间额度
        long deletedSize = oldestPictures.stream()
                .mapToLong(p -> p.getPicSize() == null ? 0 : p.getPicSize())
                .sum();
        space.setTotalCount(space.getTotalCount() - oldestPictures.size());
        space.setTotalSize(space.getTotalSize() - deletedSize);
        spaceService.updateById(space);
        // 删除 COS 文件
        oldestPictures.forEach(p ->
                cosManager.deleteObject(String.format(PictureConstant.SPACE_PICTURE, space.getId()))
        );
        // 通知空间所有者
        sendOwnerNotice(space, oldestPictures,
                "你的私有空间已超过容量上限，系统自动清理了最旧的 %d 张图片，请及时整理空间。");
        log.info("[私有空间清理] 空间 {} 淘汰了 {} 张旧图片", space.getId(), oldestPictures.size());
    }

    /**
     * 执行图片清理：删除记录 → 更新空间额度 → 删除 COS 文件 → 发送通知
     *
     * @param queryWrapper 查询条件
     * @param label        清理类型描述（用于日志）
     */
    private void clearPictures(QueryWrapper<Picture> queryWrapper, String label) {
        List<Picture> pictureList = pictureService.list(queryWrapper);
        if (pictureList.isEmpty()) {
            log.info("[私有空间清理-{}] 无符合条件的图片，跳过", label);
            return;
        }
        List<Long> ids = pictureList.stream().map(Picture::getId).collect(Collectors.toList());
        if (!pictureService.removeByIds(ids)) {
            log.warn("[私有空间清理-{}] 删除图片记录失败", label);
            return;
        }
        // 按空间分组，批量更新空间额度
        pictureList.stream()
                .collect(Collectors.groupingBy(Picture::getSpaceId))
                .forEach((spaceId, pics) -> {
                    Space space = spaceService.getById(spaceId);
                    if (space == null) {
                        return;
                    }
                    long deletedSize = pics.stream()
                            .mapToLong(p -> p.getPicSize() == null ? 0 : p.getPicSize())
                            .sum();
                    space.setTotalCount(space.getTotalCount() - pics.size());
                    space.setTotalSize(space.getTotalSize() - deletedSize);
                    spaceService.updateById(space);
                    // 删除 COS 文件
                    pics.forEach(p ->
                            cosManager.deleteObject(String.format(PictureConstant.SPACE_PICTURE, spaceId))
                    );
                    // 通知空间所有者
                    sendOwnerNotice(space, pics,
                            "你的私有空间中有 %d 张长期未访问的冷门图片已被系统自动清理，如有疑问请联系客服。");
                });
        log.info("[私有空间清理-{}] 完成，共清理 {} 张图片", label, pictureList.size());
    }

    /**
     * 发送清理通知给空间所有者
     *
     * @param space    空间对象
     * @param pics     被清理的图片列表
     * @param template 通知内容模板，含一个 %d（图片数量）占位符
     */
    private void sendOwnerNotice(Space space, List<Picture> pics, String template) {
        try {
            String picNames = pics.stream()
                    .limit(3)
                    .map(p -> p.getName() == null ? "未命名" : p.getName())
                    .collect(Collectors.joining("、"));
            if (pics.size() > 3) {
                picNames += " 等" + pics.size() + "张";
            }
            String content = String.format(template, pics.size())
                    + "（涉及图片：" + picNames + "）";
            sysNoticeService.sendNotice(space.getUserId(), "系统清理通知", content, null);
        } catch (Exception e) {
            log.warn("[私有空间清理] 发送清理通知失败，spaceId={}, error={}", space.getId(), e.getMessage());
        }
    }
}
