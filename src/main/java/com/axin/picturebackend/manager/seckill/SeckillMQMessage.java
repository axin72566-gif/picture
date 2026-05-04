package com.axin.picturebackend.manager.seckill;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 秒杀 MQ 消息体
 *
 * <p>用户秒杀成功后由 SeckillServiceImpl 发送到 RocketMQ，
 * SeckillMQConsumer 消费后异步写入 coupon 表并更新活动剩余库存。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeckillMQMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 抢购用户ID
     */
    private Long userId;

    /**
     * 秒杀活动ID
     */
    private Long activityId;

    /**
     * 预生成的优惠券编号（唯一）
     */
    private String couponNo;

    /**
     * 券面值（元）
     */
    private BigDecimal faceValue;

    /**
     * 消息发送时间戳（毫秒）
     */
    private long timestamp;
}
