package com.axin.picturebackend.model.Enum;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 订单状态枚举
 */
@Getter
public enum OrderStatusEnum {

    PENDING("待支付", "PENDING"),
    PAID("已支付", "PAID"),
    CANCELLED("已取消", "CANCELLED");

    private final String text;
    private final String value;

    OrderStatusEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     */
    public static OrderStatusEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (OrderStatusEnum anEnum : OrderStatusEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
