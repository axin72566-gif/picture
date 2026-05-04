package com.axin.picturebackend.model.Enum;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 优惠券状态枚举
 */
@Getter
public enum CouponStatusEnum {

    UNUSED("未使用", "UNUSED"),
    USED("已使用", "USED"),
    EXPIRED("已过期", "EXPIRED");

    private final String text;
    private final String value;

    CouponStatusEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     */
    public static CouponStatusEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (CouponStatusEnum anEnum : CouponStatusEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
