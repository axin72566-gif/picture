package com.axin.picturebackend.config;

/**
 * 秒杀 RocketMQ 常量配置
 * Topic 和 Tag 统一在此定义，生产者/消费者均引用此类，避免硬编码字符串散落各处。
 */
public final class SeckillRocketMQConfig {

    private SeckillRocketMQConfig() {
    }

    /**
     * 秒杀发券 Topic
     */
    public static final String SECKILL_COUPON_TOPIC = "seckill-coupon-topic";

    /**
     * 秒杀发券 Tag
     */
    public static final String SECKILL_COUPON_TAG = "SECKILL_COUPON";

    /**
     * 消费者组名称
     */
    public static final String SECKILL_CONSUMER_GROUP = "seckill-consumer-group";
}
