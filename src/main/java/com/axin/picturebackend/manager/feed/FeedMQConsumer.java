package com.axin.picturebackend.manager.feed;

import com.axin.picturebackend.config.FeedRocketMQConfig;
import com.axin.picturebackend.constant.FeedConstant;
import com.axin.picturebackend.constant.RedisConstant;
import com.axin.picturebackend.mapper.UserFollowMapper;
import com.axin.picturebackend.model.Enum.NoticeTypeEnum;
import com.axin.picturebackend.service.SysNoticeService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * Feed 流 RocketMQ 消息消费者
 *
 * <p>消费图片上传事件，根据上传者粉丝数选择推/拉策略：
 * <ul>
 *   <li><b>推模式（普通用户，粉丝数 &lt; {@link FeedConstant#BIG_V_THRESHOLD}）</b>：
 *       批量写入每个粉丝的 Redis 收件箱 ZSet（feed:inbox:{fansId}）</li>
 *   <li><b>拉模式（大V，粉丝数 &ge; {@link FeedConstant#BIG_V_THRESHOLD}）</b>：
 *       仅写上传者自己的发件箱 ZSet（feed:outbox:{uploaderId}），
 *       粉丝查询 Feed 时由 FeedService 合并拉取</li>
 * </ul>
 *
 * <p>Redis ZSet 天然幂等（重复 ZADD 只更新 score），消费重试安全。
 */
@Slf4j
@Component
@RocketMQMessageListener(
        topic = FeedRocketMQConfig.FEED_NOTIFY_TOPIC,
        selectorExpression = FeedRocketMQConfig.FEED_NOTIFY_TAG,
        consumerGroup = FeedRocketMQConfig.FEED_CONSUMER_GROUP,
        consumeThreadNumber = 4
)
public class FeedMQConsumer implements RocketMQListener<FeedMQMessage> {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private UserFollowMapper userFollowMapper;

    @Lazy
    @Resource
    private SysNoticeService sysNoticeService;

    @Override
    public void onMessage(FeedMQMessage message) {
        Long pictureId = message.getPictureId();
        Long uploaderId = message.getUploaderId();
        Long createTime = message.getCreateTime();
        Long fansCount = message.getFansCount();

        log.info("[FeedMQ] 消费消息 pictureId={}, uploaderId={}, fansCount={}",
                pictureId, uploaderId, fansCount);

        if (fansCount == null || fansCount < FeedConstant.BIG_V_THRESHOLD) {
            // ====== 推模式：批量写入粉丝收件箱 + DB通知 ======
            doPushMode(uploaderId, pictureId, createTime, message.getUploaderName());
        } else {
            // ====== 拉模式：只写上传者发件箱 ======
            doPullMode(uploaderId, pictureId, createTime);
        }
    }

    /**
     * 推模式：查询全量粉丝，分批写入各粉丝收件箱 ZSet
     */
    private void doPushMode(Long uploaderId, Long pictureId, Long createTime, String uploaderName) {
        List<Long> fansIds = userFollowMapper.selectFansIdList(uploaderId);
        if (fansIds == null || fansIds.isEmpty()) {
            log.debug("[FeedMQ] 推模式 uploaderId={} 无粉丝，跳过", uploaderId);
            return;
        }

        String member = String.valueOf(pictureId);
        double score = createTime.doubleValue();

        // 分批处理，防止单次处理粉丝过多导致内存压力
        int batchSize = FeedConstant.PUSH_BATCH_SIZE;
        List<List<Long>> batches = partition(fansIds, batchSize);

        ZSetOperations<String, String> zSetOps = stringRedisTemplate.opsForZSet();
        int totalWritten = 0;
        String displayName = uploaderName != null ? uploaderName : "";
        for (List<Long> batch : batches) {
            for (Long fansId : batch) {
                String inboxKey = RedisConstant.FEED_INBOX + fansId;
                // 写入收件箱
                zSetOps.add(inboxKey, member, score);
                // 控制收件箱容量，保留最新 INBOX_MAX_SIZE 条（score 最大=最新）
                long maxSize = FeedConstant.INBOX_MAX_SIZE;
                // ZREMRANGEBYRANK key 0 -(maxSize+1)，删除最旧超出部分
                zSetOps.removeRange(inboxKey, 0, -(maxSize + 2));
            }
            totalWritten += batch.size();

            // 写入 DB 通知（最佳努力，失败不阻塞 Redis）
            try {
                sysNoticeService.sendBatchNotice(
                        batch,
                        "关注更新",
                        String.format("你关注的「%s」发布了新图片", displayName),
                        pictureId,
                        NoticeTypeEnum.PICTURE_UPLOAD.getValue()
                );
            } catch (Exception e) {
                log.error("[FeedMQ] 写入DB通知失败 uploaderId={}, pictureId={}, batchSize={}",
                        uploaderId, pictureId, batch.size(), e);
            }
        }
        log.info("[FeedMQ] 推模式完成 uploaderId={}, pictureId={}, 写入粉丝数={}",
                uploaderId, pictureId, totalWritten);
    }

    /**
     * 拉模式：只写大V自己的发件箱 ZSet，粉丝查询时主动合并
     */
    private void doPullMode(Long uploaderId, Long pictureId, Long createTime) {
        String outboxKey = RedisConstant.FEED_OUTBOX + uploaderId;
        String member = String.valueOf(pictureId);
        double score = createTime.doubleValue();

        ZSetOperations<String, String> zSetOps = stringRedisTemplate.opsForZSet();
        zSetOps.add(outboxKey, member, score);
        // 控制发件箱容量，保留最新 OUTBOX_MAX_SIZE 条
        long maxSize = FeedConstant.OUTBOX_MAX_SIZE;
        zSetOps.removeRange(outboxKey, 0, -(maxSize + 2));

        log.info("[FeedMQ] 拉模式完成 uploaderId={}, pictureId={}", uploaderId, pictureId);
    }

    /**
     * 将 List 按 batchSize 分批
     */
    private static <T> List<List<T>> partition(List<T> list, int batchSize) {
        List<List<T>> result = new ArrayList<>();
        int size = list.size();
        for (int i = 0; i < size; i += batchSize) {
            result.add(list.subList(i, Math.min(i + batchSize, size)));
        }
        return result;
    }
}
