package com.axin.picturebackend.model.vo;

import lombok.Data;

import java.util.Date;

/**
 * VIP信息视图对象
 */
@Data
public class UserVipVO {

    /** 是否为有效VIP */
    private Boolean isVip;

    /** VIP等级（null表示非VIP） */
    private Integer vipLevel;

    /** VIP到期时间 */
    private Date expireTime;

    /** 剩余天数（-1表示非VIP） */
    private Long remainDays;
}
