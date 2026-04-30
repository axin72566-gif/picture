package com.axin.picturebackend.manager.pictureClear;

import com.axin.picturebackend.constant.PictureConstant;
import com.axin.picturebackend.manager.CosManager;
import com.axin.picturebackend.model.Enum.PictureReviewStatusEnum;
import com.axin.picturebackend.model.Enum.SpaceTypeEnum;
import com.axin.picturebackend.model.entity.Picture;
import com.axin.picturebackend.model.entity.Space;
import com.axin.picturebackend.model.entity.SpaceUser;
import com.axin.picturebackend.service.PictureService;
import com.axin.picturebackend.service.SpaceService;
import com.axin.picturebackend.service.SpaceUserService;
import com.axin.picturebackend.service.SysNoticeService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 团队空间定时清理管理器
 *
 * <p>分两个级别对团队空间中的图片进行清理：</p>
 * <ul>
 *   <li>一级：清理审核拒绝的图片 — 每天凌晨 2:30 执行，删除所有团队空间中被拒绝的图片</li>
 *   <li>二级：超容量淘汰 — 每天凌晨 3:30 执行，对空间使用率超过 90% 的团队空间按创建时间升序淘汰最旧的图片</li>
 * </ul>
 * <p>清理后会通知空间所有者及全体团队成员。</p>
 */
@Slf4j
@Component
public class TeamPictureClearManager {

    /** 超容量触发比例：空间使用率超过此比例时触发二级清理（90%） */
    private static final double OVERFLOW_RATIO = 0.9;

    /** 超容量清理目标比例：清理到 80% 以下 */
    private static final double TARGET_RATIO = 0.8;

    @Resource
    private SpaceService spaceService;

    @Resource
    private SpaceUserService spaceUserService;

    @Resource
    private PictureService pictureService;

    @Resource
    private CosManager cosManager;

    @Resource
    private SysNoticeService sysNoticeService;

    // ==================== 定时任务 ====================

    /**
     * 一级清理：每天凌晨 2:30，清理所有团队空间中审核拒绝的图片
     */
    @Scheduled(cron = "0 30 2 * * ?")
    public void firstLevelClear() {
        log.info("[团队空间清理] 一级清理开始：清理审核拒绝的图片");
        List<Long> teamSpaceIds = listTeamSpaceIds();
        if (teamSpaceIds.isEmpty()) {
            log.info("[团队空间清理] 无团队空间，跳过");
            return;
        }
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<Picture>()
                .in("spaceId", teamSpaceIds)
                .eq("reviewStatus", PictureReviewStatusEnum.REJECT.getValue());
        clearPictures(queryWrapper, "审核拒绝图片");
    }

    /**
     * 二级清理：每天凌晨 3:30，对使用率 >= 90% 的团队空间进行超容量淘汰
     * <p>按创建时间升序删除最旧的图片，直到使用率降至 80% 以下</p>
     */
    @Scheduled(cron = "0 30 3 * * ?")
    public void secondLevelClear() {
        log.info("[团队空间清理] 二级清理开始：超容量淘汰");
        List<Space> overflowSpaces = listOverflowTeamSpaces();
        if (overflowSpaces.isEmpty()) {
            log.info("[团队空间清理] 无超容量团队空间，跳过");
            return;
        }
        log.info("[团队空间清理] 发现 {} 个超容量团队空间，开始逐一处理", overflowSpaces.size());
        for (Space space : overflowSpaces) {
            evictOldestPictures(space);
        }
        log.info("[团队空间清理] 二级清理完成");
    }

    // ==================== 私有方法 ====================

    /**
     * 查询所有团队空间的 ID 列表
     */
    private List<Long> listTeamSpaceIds() {
        return spaceService.list(
                new QueryWrapper<Space>()
                        .eq("spaceType", SpaceTypeEnum.TEAM.getValue())
                        .select("id")
        ).stream().map(Space::getId).collect(Collectors.toList());
    }

    /**
     * 查询使用率 >= 90% 的团队空间列表
     */
    private List<Space> listOverflowTeamSpaces() {
        List<Space> allTeamSpaces = spaceService.list(
                new QueryWrapper<Space>().eq("spaceType", SpaceTypeEnum.TEAM.getValue())
        );
        return allTeamSpaces.stream()
                .filter(space -> space.getMaxCount() > 0
                        && (double) space.getTotalCount() / space.getMaxCount() >= OVERFLOW_RATIO)
                .collect(Collectors.toList());
    }

    /**
     * 执行图片清理：删除记录 → 更新空间额度 → 删除 COS 文件 → 通知团队成员
     *
     * @param queryWrapper 查询条件
     * @param label        清理类型描述（用于日志）
     */
    private void clearPictures(QueryWrapper<Picture> queryWrapper, String label) {
        List<Picture> pictureList = pictureService.list(queryWrapper);
        if (pictureList.isEmpty()) {
            log.info("[团队空间清理-{}] 无符合条件的图片，跳过", label);
            return;
        }
        List<Long> ids = pictureList.stream().map(Picture::getId).collect(Collectors.toList());
        if (!pictureService.removeByIds(ids)) {
            log.warn("[团队空间清理-{}] 删除图片记录失败", label);
            return;
        }
        // 按空间分组，批量更新空间额度、删除 COS 文件、发送通知
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
                    // 通知空间所有者及团队成员
                    String content = buildNoticeContent(pics, label);
                    sendTeamNotice(space, content);
                });
        log.info("[团队空间清理-{}] 完成，共清理 {} 张图片", label, pictureList.size());
    }

    /**
     * 对单个超容量团队空间按创建时间升序淘汰最旧图片，直到使用率降至 80% 以下
     *
     * @param space 目标空间
     */
    private void evictOldestPictures(Space space) {
        long targetCount = (long) (space.getMaxCount() * TARGET_RATIO);
        long toDelete = space.getTotalCount() - targetCount;
        if (toDelete <= 0) {
            return;
        }
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
        if (!pictureService.removeByIds(ids)) {
            log.warn("[团队空间清理] 空间 {} 超容量淘汰失败", space.getId());
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
        // 通知团队所有成员
        String content = buildNoticeContent(oldestPictures, "超容量淘汰");
        sendTeamNotice(space, content);
        log.info("[团队空间清理] 空间 {} 淘汰了 {} 张旧图片", space.getId(), oldestPictures.size());
    }

    /**
     * 发送清理通知给空间所有者及全体团队成员
     *
     * @param space   空间对象
     * @param content 通知内容
     */
    private void sendTeamNotice(Space space, String content) {
        try {
            // 收集所有成员 ID
            List<Long> memberIds = spaceUserService.list(
                    new QueryWrapper<SpaceUser>().eq("spaceId", space.getId()).select("userId")
            ).stream().map(SpaceUser::getUserId).distinct().collect(Collectors.toList());
            // 确保空间所有者也在列表中
            if (!memberIds.contains(space.getUserId())) {
                memberIds.add(space.getUserId());
            }
            sysNoticeService.sendBatchNotice(memberIds, "团队空间清理通知", content, null);
        } catch (Exception e) {
            log.warn("[团队空间清理] 发送通知失败，spaceId={}, error={}", space.getId(), e.getMessage());
        }
    }

    /**
     * 构建通知内容字符串
     *
     * @param pics  被清理的图片列表
     * @param label 清理类型描述
     * @return 通知内容
     */
    private String buildNoticeContent(List<Picture> pics, String label) {
        String picNames = pics.stream()
                .limit(3)
                .map(p -> p.getName() == null ? "未命名" : p.getName())
                .collect(Collectors.joining("、"));
        if (pics.size() > 3) {
            picNames += " 等" + pics.size() + "张";
        }
        return String.format("你所在的团队空间因【%s】，共 %d 张图片已被系统自动清理。（涉及图片：%s）",
                label, pics.size(), picNames);
    }
}
