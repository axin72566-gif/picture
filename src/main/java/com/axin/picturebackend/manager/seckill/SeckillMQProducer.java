package com.axin.picturebackend.manager.seckill;

import com.axin.picturebackend.config.SeckillRocketMQConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 秒杀 RocketMQ 消息生产者
 *
 * <p>Redis 预扣库存成功后发送 MQ 消息，Consumer 异步落库写券。
 * 使用 syncSend 保证消息可靠投递，失败时捕获异常记录日志。
 */
@Slf4j
@Component
public class SeckillMQProducer {

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    /**
     * 发送秒杀成功消息
     *
     * @param message 秒杀 MQ 消息体
     */
    public void sendSeckillMessage(SeckillMQMessage message) {
        String destination = SeckillRocketMQConfig.SECKILL_COUPON_TOPIC
                + ":" + SeckillRocketMQConfig.SECKILL_COUPON_TAG;
        try {
            rocketMQTemplate.syncSend(
                    destination,
                    MessageBuilder.withPayload(message).build()
            );
            log.debug("[SeckillMQ] 发送成功 userId={}, activityId={}, couponNo={}",
                    message.getUserId(), message.getActivityId(), message.getCouponNo());
        } catch (Exception e) {
            log.warn("[SeckillMQ] 发送失败 userId={}, activityId={}, couponNo={}, error={}",
                    message.getUserId(), message.getActivityId(), message.getCouponNo(), e.getMessage());
        }
    }
}
