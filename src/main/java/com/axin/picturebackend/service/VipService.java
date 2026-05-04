package com.axin.picturebackend.service;

import com.axin.picturebackend.model.entity.User;
import com.axin.picturebackend.model.entity.UserVip;
import com.axin.picturebackend.model.vo.UserVipVO;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 用户会员 Service
 */
public interface VipService extends IService<UserVip> {

    /**
     * 开通或续费VIP（购买成功后调用）
     *
     * @param userId  用户ID
     * @param days    增加的有效天数
     */
    void openOrRenewVip(Long userId, int days);

    /**
     * 查询用户VIP信息（含是否有效、到期时间、剩余天数）
     *
     * @param userId 用户ID
     * @return VIP信息VO
     */
    UserVipVO getVipInfo(Long userId);

    /**
     * 判断用户VIP是否有效（实时查库）
     *
     * @param userId 用户ID
     * @return true=VIP有效
     */
    boolean isVip(Long userId);
}
