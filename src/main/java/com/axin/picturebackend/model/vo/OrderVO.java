package com.axin.picturebackend.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 订单视图对象
 */
@Data
public class OrderVO {

    /** 订单ID */
    private Long id;

    /** 订单编号 */
    private String orderNo;

    /** 商品类型 */
    private String productType;

    /** 商品类型描述 */
    private String productTypeName;

    /** 订单金额（实付金额） */
    private BigDecimal amount;

    /** 原价（未抵扣前） */
    private BigDecimal originalAmount;

    /** 优惠券抵扣金额 */
    private BigDecimal couponDiscount;

    /** 使用的优惠券ID */
    private Long couponId;

    /** 订单状态 */
    private String status;

    /** 订单状态描述 */
    private String statusName;

    /** 支付时间 */
    private Date payTime;

    /** 创建时间 */
    private Date createTime;
}
