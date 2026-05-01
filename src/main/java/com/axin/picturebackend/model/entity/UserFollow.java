package com.axin.picturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户关注关联
 * @TableName user_follow
 */
@TableName(value = "user_follow")
@Data
public class UserFollow implements Serializable {

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 发起关注的用户ID
     */
    private Long userId;

    /**
     * 被关注的用户ID
     */
    private Long followUserId;

    /**
     * 关注时间
     */
    private Date createTime;
}
