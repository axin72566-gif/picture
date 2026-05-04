package com.axin.picturebackend.model.dto.seckill;

import lombok.Data;

import java.io.Serializable;

/**
 * 秒杀购买请求
 */
@Data
public class SeckillBuyRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 秒杀活动ID
     */
    private Long activityId;
}
