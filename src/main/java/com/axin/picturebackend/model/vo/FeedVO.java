package com.axin.picturebackend.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * Feed 流条目视图对象
 * 粉丝拉取 Feed 流时返回的单条动态数据
 */
@Data
public class FeedVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 图片ID
     */
    private Long pictureId;

    /**
     * 缩略图 URL（展示用）
     */
    private String thumbnailUrl;

    /**
     * 图片名称
     */
    private String picName;

    /**
     * 图片原始 URL（可选，点击详情时用）
     */
    private String url;

    /**
     * 上传者（作者）用户ID
     */
    private Long authorId;

    /**
     * 上传者昵称
     */
    private String authorName;

    /**
     * 上传者头像 URL
     */
    private String authorAvatar;

    /**
     * 图片上传时间戳（毫秒），同时作为游标值（cursor）
     */
    private Long createTime;
}
