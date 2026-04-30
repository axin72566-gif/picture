package com.axin.picturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 用户通知主表
 * @TableName sys_notice
 */
@TableName(value ="sys_notice")
@Data
public class SysNotice {
    /**
     * 主键ID，自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 接收通知的用户ID（系统公告填0表示全体用户）
     */
    private Long userId;

    /**
     * 通知标题（系统公告必填，互动通知可选）
     */
    private String title;

    /**
     * 通知内容（如：XXX点赞了你的文章）
     */
    private String content;

    /**
     * 关联业务ID（如点赞的文章ID、评论ID）
     */
    private Long relatedId;

    /**
     * 阅读状态：0-未读 1-已读
     */
    private Integer isRead;

    /**
     * 阅读时间（标记已读时更新）
     */
    private Date readTime;

    /**
     * 通知创建时间
     */
    private Date createTime;

    /**
     * 删除时间（软删除时更新）
     */
    private Date updateTime;

    /**
     * 删除状态：0-正常 1-已删除（软删除）
     */
    private Integer isDeleted;
}