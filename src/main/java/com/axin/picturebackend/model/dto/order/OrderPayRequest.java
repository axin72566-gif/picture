package com.axin.picturebackend.model.dto.order;

import lombok.Data;

import java.io.Serializable;

/**
 * 模拟支付请求
 */
@Data
public class OrderPayRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 订单编号
     */
    private String orderNo;
}
