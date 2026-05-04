package com.axin.picturebackend.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 秒杀活动视图对象
 */
@Data
public class SeckillActivityVO {

    /**
     * 活动ID
     */
    private Long id;

    /**
     * 活动名称
     */
    private String name;

    /**
     * 总库存
     */
    private Integer totalStock;

    /**
     * 剩余库存（实时）
     */
    private Integer remainStock;

    /**
     * 券面值（元）
     */
    private BigDecimal faceValue;

    /**
     * 秒杀售价（元）
     */
    private BigDecimal salePrice;

    /**
     * 活动开始时间
     */
    private Date startTime;

    /**
     * 活动结束时间
     */
    private Date endTime;

    /**
     * 活动状态描述：未开始 / 进行中 / 已结束 / 已售罄
     */
    private String statusName;
}
