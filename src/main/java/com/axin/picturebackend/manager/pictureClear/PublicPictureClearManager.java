package com.axin.picturebackend.manager.pictureClear;

import com.axin.picturebackend.exception.BusinessException;
import com.axin.picturebackend.exception.ErrorCode;
import com.axin.picturebackend.config.CosClientConfig;
import com.axin.picturebackend.exception.ThrowUtils;
import com.axin.picturebackend.manager.CosManager;
import com.axin.picturebackend.model.Enum.NoticeTypeEnum;
import com.axin.picturebackend.model.Enum.PictureReviewStatusEnum;
import com.axin.picturebackend.model.entity.Picture;
import com.axin.picturebackend.service.PictureService;
import com.axin.picturebackend.service.SysNoticeService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * 公共图库定时清理管理器
 * <p>分三个级别对公共图库中的低质量/冷门/违规图片进行清理</p>
 */
@Slf4j
@Component
public class PublicPictureClearManager {

    /** 触发三级清理的图片数量阈值 */
    private static final int THIRD_LEVEL_THRESHOLD = 1_000_000;
    /** 二级清理：图片冷门天数（90天） */
    private static final long SECOND_LEVEL_DAYS_MS = 90L * 24 * 60 * 60 * 1000;
    /** 三级清理：图片过时天数（2年） */
    private static final long THIRD_LEVEL_DAYS_MS = 2L * 365 * 24 * 60 * 60 * 1000;
    /** 三级清理：大文件阈值（20 MB） */
    private static final long THIRD_LEVEL_SIZE_BYTES = 20L * 1024 * 1024;

    @Resource
    private PictureService pictureService;

    @Resource
    private CosManager cosManager;

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private SysNoticeService sysNoticeService;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource(name = "backgroundExecutor")
    private Executor backgroundExecutor;

    /**
     * 一级清理：每天凌晨 3 点清理审核拒绝的图片
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void firstLevelClear() {
        log.info("[公共图库清理] 一级清理开始：清理审核拒绝的图片");
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<Picture>()
                .isNull("spaceId")
                .eq("reviewStatus", PictureReviewStatusEnum.REJECT.getValue());
        clearPicture(queryWrapper);
    }

    /**
     * 二级清理：每周一凌晨 4 点清理长期冷门图片（访问量 < 3 且超过 90 天）
     */
    @Scheduled(cron = "0 0 4 * * MON")
    public void secondLevelClear() {
        log.info("[公共图库清理] 二级清理开始：清理长期冷门图片");
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<Picture>()
                .lt("viewCount", 3)
                .lt("createTime", System.currentTimeMillis() - SECOND_LEVEL_DAYS_MS);
        clearPicture(queryWrapper);
    }

    /**
     * 三级清理1：每月 1 日和 15 日清理大文件低质量图片（图片总数 > 100 万时触发）
     * <p>清理条件：文件 > 20MB 且访问量 < 10</p>
     */
    @Scheduled(cron = "0 0 0 1,15 * ?")
    public void thirdLevelClearLargeFiles() {
        if (!isAboveThreshold()) {
            return;
        }
        log.info("[公共图库清理] 三级清理1开始：清理大文件低质量图片");
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<Picture>()
                .isNull("spaceId")
                .gt("picSize", THIRD_LEVEL_SIZE_BYTES)
                .lt("viewCount", 10);
        clearPicture(queryWrapper);
    }

    /**
     * 三级清理2：每月 1 日和 15 日清理过时图片（图片总数 > 100 万时触发）
     * <p>清理条件：超过 2 年且访问量 < 10</p>
     */
    @Scheduled(cron = "0 0 0 1,15 * ?")
    public void thirdLevelClearOldPictures() {
        if (!isAboveThreshold()) {
            return;
        }
        log.info("[公共图库清理] 三级清理2开始：清理过时图片");
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<Picture>()
                .isNull("spaceId")
                .lt("viewCount", 10)
                .lt("createTime", System.currentTimeMillis() - THIRD_LEVEL_DAYS_MS);
        clearPicture(queryWrapper);
    }

    // ==================== 私有方法 ====================

    /**
     * 判断公共图库图片数量是否超过三级清理阈值
     */
    private boolean isAboveThreshold() {
        long count = pictureService.count(new QueryWrapper<Picture>().isNull("spaceId"));
        return count >= THIRD_LEVEL_THRESHOLD;
    }

    /**
     * 执行图片清理：分批处理避免内存溢出
     *
     * @param queryWrapper 查询条件
     */
    private void clearPicture(QueryWrapper<Picture> queryWrapper) {
        long pageSize = 500;
        int totalDeleted = 0;

        while (true) {
            Page<Picture> page = pictureService.page(new Page<>(1, pageSize), queryWrapper);
            List<Picture> pictureList = page.getRecords();
            if (CollectionUtils.isEmpty(pictureList)) {
                break;
            }

            List<Long> ids = pictureList.stream().map(Picture::getId).collect(Collectors.toList());
            Boolean success = transactionTemplate.execute(status -> {
                boolean removed = pictureService.removeByIds(ids);
                ThrowUtils.throwIf(!removed, ErrorCode.OPERATION_ERROR, "定时清理任务执行失败");
                return true;
            });

            if (Boolean.TRUE.equals(success)) {
                // 异步清理 COS 文件并发送通知
                asyncCleanCosAndNotify(pictureList);
            }

            totalDeleted += pictureList.size();
            if (pictureList.size() < pageSize) {
                break;
            }
        }
        log.info("[公共图库清理] 完成，共清理 {} 张图片", totalDeleted);
    }

    /**
     * 异步清理 COS 文件并发送通知
     */
    private void asyncCleanCosAndNotify(List<Picture> pictureList) {
        CompletableFuture.runAsync(() -> {
            // 1. 清理 COS 文件
            for (Picture picture : pictureList) {
                try {
                    String url = picture.getUrl();
                    if (StringUtils.isNotBlank(url)) {
                        String key = url.replace(cosClientConfig.getHost() + "/", "");
                        cosManager.deleteObject(key);
                    }
                    String thumbnailUrl = picture.getThumbnailUrl();
                    if (StringUtils.isNotBlank(thumbnailUrl)) {
                        String thumbKey = thumbnailUrl.replace(cosClientConfig.getHost() + "/", "");
                        cosManager.deleteObject(thumbKey);
                    }
                } catch (Exception e) {
                    log.error("[公共图库清理] 清理 COS 文件失败, pictureId={}, error={}", picture.getId(), e.getMessage());
                }
            }
            // 2. 发送清理通知给图片作者
            sendClearNotices(pictureList);
        }, backgroundExecutor);
    }

    /**
     * 按用户分组，批量发送图片清理系统通知
     */
    private void sendClearNotices(List<Picture> pictureList) {
        try {
            Map<Long, List<Picture>> byUser = pictureList.stream()
                    .collect(Collectors.groupingBy(Picture::getUserId));
            byUser.forEach((userId, pics) -> {
                String picNames = pics.stream()
                        .limit(3)
                        .map(p -> p.getName() == null ? "未命名" : p.getName())
                        .collect(Collectors.joining("、"));
                if (pics.size() > 3) {
                    picNames += " 等" + pics.size() + "张";
                }
                String content = String.format("你的图片「%s」因长期冷门或不符合规范，已被系统自动清理。", picNames);
                sysNoticeService.sendNotice(userId, "系统清理通知", content, null,
                        NoticeTypeEnum.SYSTEM_CLEAR.getValue());
            });
        } catch (Exception e) {
            log.warn("[公共图库清理] 发送清理通知失败, error={}", e.getMessage());
        }
    }
}
