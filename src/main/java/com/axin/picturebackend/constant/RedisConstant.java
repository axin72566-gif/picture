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
}
