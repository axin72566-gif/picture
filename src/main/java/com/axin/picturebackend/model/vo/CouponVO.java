package com.axin.picturebackend.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 用户优惠券视图对象
 */
@Data
public class CouponVO {

    /**
     * 优惠券ID
     */
    private Long id;

    /**
     * 优惠券编号
     */
    private String couponNo;

    /**
     * 券面值（元）
     */
    private BigDecimal faceValue;

    /**
     * 状态：UNUSED / USED / EXPIRED
     */
    private String status;

    /**
     * 状态描述：未使用 / 已使用 / 已过期
     */
    private String statusName;

    /**
     * 活动名称
     */
    private String activityName;

    /**
     * 过期时间
     */
    private Date expireTime;

    /**
     * 使用时间
     */
    private Date useTime;

    /**
     * 发放时间
     */
    private Date createTime;
}
