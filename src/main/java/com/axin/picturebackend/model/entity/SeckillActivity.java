package com.axin.picturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 秒杀活动
 *
 * @TableName seckill_activity
 */
@TableName(value = "seckill_activity")
@Data
public class SeckillActivity implements Serializable {

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /**
     * 活动ID（雪花算法）
     */
    @TableId(type = IdType.ASSIGN_ID)
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
     * 剩余库存
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
