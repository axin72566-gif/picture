package com.axin.picturebackend.model.dto.order;

import lombok.Data;

import java.io.Serializable;

/**
 * 创建订单请求
 */
@Data
public class OrderCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 商品类型，如：MONTH_VIP
     */
    private String productType;

    /**
     * 优惠券ID（可选，不传则不使用优惠券）
     */
    private Long couponId;
}

