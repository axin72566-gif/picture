package com.axin.picturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 用户优惠券
 *
 * @TableName coupon
 */
@TableName(value = "coupon")
@Data
public class Coupon implements Serializable {

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /**
     * 优惠券ID（雪花算法）
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 所属用户ID
     */
    private Long userId;

    /**
     * 来源秒杀活动ID
     */
    private Long activityId;

    /**
     * 优惠券编号（系统唯一）
     */
    private String couponNo;

    /**
     * 券面值（元）
     */
    private BigDecimal faceValue;

    /**
     * 状态：UNUSED-未使用 USED-已使用 EXPIRED-已过期
     */
    private String status;

    /**
     * 使用时间
     */
    private Date useTime;

    /**
     * 过期时间（默认发放后30天）
     */
    private Date expireTime;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 逻辑删除
     */
    @TableLogic
    private Integer isDelete;
}
