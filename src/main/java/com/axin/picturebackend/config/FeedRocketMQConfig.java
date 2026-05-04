package com.axin.picturebackend.config;

/**
 * Feed 流 RocketMQ 常量配置
 * Topic 和 Tag 统一在此定义，生产者/消费者均引用此类，避免硬编码字符串散落各处。
 */
public final class FeedRocketMQConfig {

    private FeedRocketMQConfig() {
    }

    /**
     * Feed 通知 Topic（需在 RocketMQ Broker 端提前创建，或开启 autoCreateTopicEnable=true）
     */
    public static final String FEED_NOTIFY_TOPIC = "feed-notify-topic";

    /**
     * Feed 通知 Tag，用于消费者侧过滤，避免消费无关消息
     */
    public static final String FEED_NOTIFY_TAG = "FEED_NOTIFY";

    /**
     * 消费者组名称
     */
    public static final String FEED_CONSUMER_GROUP = "feed-consumer-group";
}
