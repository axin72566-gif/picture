package com.axin.picturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 订单
 *
 * @TableName orders
 */
@TableName(value = "orders")
@Data
public class Orders implements Serializable {

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /**
     * 订单ID（雪花算法）
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 订单编号（系统生成唯一）
     */
    private String orderNo;

    /**
     * 下单用户ID
     */
    private Long userId;

    /**
     * 商品类型：MONTH_VIP
     */
    private String productType;

    /**
     * 订单金额（实付金额）
     */
    private BigDecimal amount;

    /**
     * 原价（未抵扣前）
     */
    private BigDecimal originalAmount;

    /**
     * 优惠券抵扣金额
     */
    private BigDecimal couponDiscount;

    /**
     * 使用的优惠券ID（可为空）
     */
    private Long couponId;

    /**
     * 订单状态：PENDING-待支付 PAID-已支付 CANCELLED-已取消
     */
    private String status;

    /**
     * 支付时间
     */
    private Date payTime;

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
