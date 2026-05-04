package com.axin.picturebackend.service;

import com.axin.picturebackend.model.entity.Orders;
import com.axin.picturebackend.model.vo.OrderVO;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 订单 Service
 */
public interface OrderService extends IService<Orders> {

    /**
     * 创建订单
     *
     * @param userId      下单用户ID
     * @param productType 商品类型（如 MONTH_VIP）
     * @return 订单VO（含订单编号和金额）
     */
    OrderVO createOrder(Long userId, String productType);

    /**
     * 创建订单（支持优惠券抵扣）
     *
     * @param userId      下单用户ID
     * @param productType 商品类型（如 MONTH_VIP）
     * @param couponId    优惠券ID（可为 null，不使用优惠券）
     * @return 订单VO（含订单编号和抵扣后金额）
     */
    OrderVO createOrder(Long userId, String productType, Long couponId);

    /**
     * 模拟支付：将订单状态改为已支付，并触发 VIP 开通/续费
     *
     * @param orderNo 订单编号
     * @param userId  当前用户ID（用于权限校验）
     * @return 支付后的订单VO
     */
    OrderVO mockPay(String orderNo, Long userId);

    /**
     * 查询单个订单详情
     *
     * @param orderNo 订单编号
     * @param userId  当前用户ID（非管理员只能查自己的订单）
     * @return 订单VO
     */
    OrderVO getOrderByNo(String orderNo, Long userId);

    /**
     * 取消订单（仅 PENDING 状态可取消）
     *
     * @param orderNo 订单编号
     * @param userId  当前用户ID
     */
    void cancelOrder(String orderNo, Long userId);
}
