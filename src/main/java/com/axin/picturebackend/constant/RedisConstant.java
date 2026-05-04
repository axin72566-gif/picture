package com.axin.picturebackend.constant;

public class RedisConstant {

	public static final String PICTURE = "picture:";

	public static final String PICTURE_VIEW_COUNT = "picture:view:";

	public static final String PICTURE_LIKE_COUNT = "picture:like:";

	/**
	 * 图片点赞用户排行榜 (ZSet)
	 */
	public static final String PICTURE_LIKE_TOP_USERS = "picture:like:top:";

	/**
	 * 图片评论计数（今日）
	 */
	public static final String PICTURE_COMMENT_COUNT = "picture:comment:";

	/**
	 * 实时热度排名 ZSET（member=pictureId, score=hotScore）
	 */
	public static final String PICTURE_HOT_TODAY = "picture:hot:today";

	/**
	 * 每日推荐图片快照（JSON List<PictureVO>，TTL 48h）
	 */
	public static final String PICTURE_RECOMMEND_DAILY = "picture:recommend:daily";

	// ==================== Feed 流相关 ====================

	/**
	 * Feed 收件箱（ZSet）：普通用户推模式写入粉丝收件箱
	 * Key: feed:inbox:{userId}，member=pictureId，score=createTime 时间戳
	 */
	public static final String FEED_INBOX = "feed:inbox:";

	/**
	 * Feed 发件箱（ZSet）：大V拉模式，仅写自己的发件箱
	 * Key: feed:outbox:{userId}，member=pictureId，score=createTime 时间戳
	 */
	public static final String FEED_OUTBOX = "feed:outbox:";

	/**
	 * 用户粉丝数缓存（String）：避免判断大V阈值时每次查库
	 * Key: user:fans:count:{userId}，value=粉丝数
	 */
	public static final String USER_FANS_COUNT = "user:fans:count:";
}
