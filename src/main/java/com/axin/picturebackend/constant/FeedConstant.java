package com.axin.picturebackend.constant;

/**
 * Feed 流业务常量
 */
public final class FeedConstant {

    private FeedConstant() {
    }

    /**
     * 大V粉丝数阈值：粉丝数 >= 此值时采用拉模式（不主动推收件箱），避免写扩散
     */
    public static final long BIG_V_THRESHOLD = 1000L;

    /**
     * 收件箱（Inbox）最大保留条数（普通用户推模式）
     * 超出后自动删除最旧的条目
     */
    public static final long INBOX_MAX_SIZE = 500L;

    /**
     * 发件箱（Outbox）最大保留条数（大V拉模式）
     */
    public static final long OUTBOX_MAX_SIZE = 1000L;

    /**
     * Feed 流每页默认大小
     */
    public static final int FEED_DEFAULT_PAGE_SIZE = 20;

    /**
     * Feed 流每页最大大小，防止恶意请求
     */
    public static final int FEED_MAX_PAGE_SIZE = 50;

    /**
     * 推模式批量写 Redis 时每批处理的粉丝数，防止单次操作耗时过长
     */
    public static final int PUSH_BATCH_SIZE = 1000;
}
