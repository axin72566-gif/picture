package com.axin.picturebackend.model.Enum;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 商品类型枚举
 */
@Getter
public enum ProductTypeEnum {

    MONTH_VIP("月卡VIP", "MONTH_VIP", new BigDecimal("9.90"), 30);

    private final String text;
    private final String value;
    /** 价格 */
    private final BigDecimal price;
    /** 有效天数 */
    private final int days;

    ProductTypeEnum(String text, String value, BigDecimal price, int days) {
        this.text = text;
        this.value = value;
        this.price = price;
        this.days = days;
    }

    /**
     * 根据 value 获取枚举
     */
    public static ProductTypeEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (ProductTypeEnum anEnum : ProductTypeEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
