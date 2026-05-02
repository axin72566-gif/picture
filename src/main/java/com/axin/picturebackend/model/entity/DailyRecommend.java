package com.axin.picturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 每日推荐图片持久化
 * @TableName daily_recommend
 */
@TableName(value = "daily_recommend")
@Data
public class DailyRecommend {

    /**
     * 主键ID，自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 推荐日期（展示日期，非生成日期）
     */
    private Date recommendDate;

    /**
     * 推荐图片ID列表（JSON数组）
     */
    private String pictureIds;

    /**
     * 完整PictureVO JSON数据
     */
    private String pictureData;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}
