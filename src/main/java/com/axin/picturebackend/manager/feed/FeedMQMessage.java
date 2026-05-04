package com.axin.picturebackend.manager.feed;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Feed 流 MQ 消息体
 *
 * <p>图片上传成功后由 PictureServiceImpl 发送到 RocketMQ，
 * FeedMQConsumer 消费后根据 fansCount 判断推模式/拉模式写 Redis ZSet。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedMQMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 上传的图片ID
     */
    private Long pictureId;

    /**
     * 上传者用户ID
     */
    private Long uploaderId;

    /**
     * 上传者昵称（消费时无需再查库）
     */
    private String uploaderName;

    /**
     * 上传者头像 URL
     */
    private String uploaderAvatar;

    /**
     * 图片上传时间戳（毫秒），用作 ZSet score，同时作为 Feed 游标
     */
    private Long createTime;

    /**
     * 发送时快照的粉丝数，消费者直接用于阈值判断，避免重复查库
     */
    private Long fansCount;
}
