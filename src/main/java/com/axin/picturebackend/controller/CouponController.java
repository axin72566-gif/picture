package com.axin.picturebackend.controller;

import com.axin.picturebackend.common.BaseResponse;
import com.axin.picturebackend.common.ResultUtils;
import com.axin.picturebackend.exception.ErrorCode;
import com.axin.picturebackend.exception.ThrowUtils;
import com.axin.picturebackend.model.entity.User;
import com.axin.picturebackend.model.vo.CouponVO;
import com.axin.picturebackend.service.CouponService;
import com.axin.picturebackend.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 优惠券控制器
 */
@RestController
@RequestMapping("/coupon")
public class CouponController {

    @Resource
    private CouponService couponService;

    @Resource
    private UserService userService;

    /**
     * 查询我的优惠券列表
     * GET /coupon/my
     */
    @GetMapping("/my")
    public BaseResponse<List<CouponVO>> listMyCoupons(HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        List<CouponVO> coupons = couponService.listMyCoupons(loginUser.getId());
        return ResultUtils.success(coupons);
    }

    /**
     * 查询单张有效优惠券详情（下单前确认抵扣信息）
     * GET /coupon/valid?couponId=xxx
     */
    @GetMapping("/valid")
    public BaseResponse<CouponVO> getValidCoupon(@RequestParam Long couponId,
                                                  HttpServletRequest httpRequest) {
        ThrowUtils.throwIf(couponId == null, ErrorCode.PARAMS_ERROR, "优惠券ID不能为空");
        User loginUser = userService.getLoginUser(httpRequest);
        CouponVO couponVO = couponService.getValidCoupon(couponId, loginUser.getId());
        return ResultUtils.success(couponVO);
    }
}
