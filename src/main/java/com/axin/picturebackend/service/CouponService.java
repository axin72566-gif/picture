package com.axin.picturebackend.service;

import com.axin.picturebackend.model.entity.Coupon;
import com.axin.picturebackend.model.vo.CouponVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 优惠券 Service 接口
 */
public interface CouponService extends IService<Coupon> {

    /**
     * 查询当前用户的优惠券列表
     *
     * @param userId 用户ID
     * @return 优惠券列表
     */
    List<CouponVO> listMyCoupons(Long userId);

    /**
     * 校验并锁定优惠券（下单时调用，将 UNUSED → USED，乐观锁更新）
     * 失败时抛出 BusinessException
     *
     * @param couponId 优惠券ID
     * @param userId   操作用户ID（校验归属）
     * @return 券面值
     */
    java.math.BigDecimal lockCoupon(Long couponId, Long userId);

    /**
     * 根据ID查询有效的（UNUSED 且未过期）优惠券，用于下单前展示
     *
     * @param couponId 优惠券ID
     * @param userId   用户ID
     * @return 优惠券VO
     */
    CouponVO getValidCoupon(Long couponId, Long userId);
}
