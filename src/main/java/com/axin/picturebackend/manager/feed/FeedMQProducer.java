package com.axin.picturebackend.manager.feed;

import com.axin.picturebackend.config.FeedRocketMQConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * Feed 流 RocketMQ 消息生产者
 *
 * <p>在图片上传成功后（事务提交后）异步发送消息，失败时只打 warn 日志，
 * 不影响图片上传主流程。
 */
@Slf4j
@Component
public class FeedMQProducer {

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    /**
     * 发送 Feed 通知消息
     *
     * <p>使用 Topic:Tag 格式的目的地，消费者端可按 Tag 过滤。
     * 使用 syncSend 保证消息可靠投递，失败时捕获异常降级，不影响调用方。
     *
     * @param message Feed 消息体
     */
    public void sendFeedNotify(FeedMQMessage message) {
        String destination = FeedRocketMQConfig.FEED_NOTIFY_TOPIC
                + ":" + FeedRocketMQConfig.FEED_NOTIFY_TAG;
        try {
            rocketMQTemplate.syncSend(
                    destination,
                    MessageBuilder.withPayload(message).build()
            );
            log.debug("[FeedMQ] 发送成功 pictureId={}, uploaderId={}",
                    message.getPictureId(), message.getUploaderId());
        } catch (Exception e) {
            // 不影响主流程，仅打 warn 日志
            log.warn("[FeedMQ] 发送失败 pictureId={}, uploaderId={}, error={}",
                    message.getPictureId(), message.getUploaderId(), e.getMessage());
        }
    }
}
