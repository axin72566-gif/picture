package com.axin.picturebackend.service.impl;

import cn.hutool.core.util.IdUtil;
import com.axin.picturebackend.exception.BusinessException;
import com.axin.picturebackend.exception.ErrorCode;
import com.axin.picturebackend.mapper.OrdersMapper;
import com.axin.picturebackend.model.Enum.OrderStatusEnum;
import com.axin.picturebackend.model.Enum.ProductTypeEnum;
import com.axin.picturebackend.model.entity.Orders;
import com.axin.picturebackend.model.vo.OrderVO;
import com.axin.picturebackend.service.CouponService;
import com.axin.picturebackend.service.OrderService;
import com.axin.picturebackend.service.VipService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 订单 Service 实现
 */
@Slf4j
@Service
public class OrderServiceImpl extends ServiceImpl<OrdersMapper, Orders>
        implements OrderService {

    @Lazy
    @Resource
    private VipService vipService;

    @Lazy
    @Resource
    private CouponService couponService;

    /**
     * 创建订单（不使用优惠券）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO createOrder(Long userId, String productType) {
        return createOrder(userId, productType, null);
    }

    /**
     * 创建订单（支持优惠券抵扣）
     * 1. 校验商品类型合法
     * 2. 若传入 couponId，校验并锁定优惠券，计算抵扣后金额
     * 3. 若存在未支付订单（同商品+同券）则直接返回（防重复下单）
     * 4. 插入新订单，状态为 PENDING
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO createOrder(Long userId, String productType, Long couponId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        ProductTypeEnum productTypeEnum = ProductTypeEnum.getEnumByValue(productType);
        if (productTypeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "商品类型不存在");
        }

        // 防重复下单：同一用户同一商品类型已有待支付订单时直接返回
        Orders pending = this.getOne(
                new LambdaQueryWrapper<Orders>()
                        .eq(Orders::getUserId, userId)
                        .eq(Orders::getProductType, productType)
                        .eq(Orders::getStatus, OrderStatusEnum.PENDING.getValue())
                        .orderByDesc(Orders::getCreateTime)
                        .last("LIMIT 1")
        );
        if (pending != null) {
            return convertToVO(pending, productTypeEnum);
        }

        // 计算优惠券抵扣
        BigDecimal originalPrice = productTypeEnum.getPrice();
        BigDecimal couponDiscount = BigDecimal.ZERO;
        if (couponId != null) {
            // lockCoupon 内部完成校验 + 乐观锁 UNUSED->USED
            BigDecimal faceValue = couponService.lockCoupon(couponId, userId);
            couponDiscount = faceValue;
        }
        // 实付金额 = 原价 - 优惠券面值，最低 0
        BigDecimal actualAmount = originalPrice.subtract(couponDiscount).max(BigDecimal.ZERO);

        // 创建新订单
        Orders order = new Orders();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setProductType(productType);
        order.setOriginalAmount(originalPrice);
        order.setCouponDiscount(couponDiscount);
        order.setCouponId(couponId);
        order.setAmount(actualAmount);
        order.setStatus(OrderStatusEnum.PENDING.getValue());
        this.save(order);

        log.info("用户[{}] 创建订单成功，订单号：{}，原价：{}，优惠券抵扣：{}，实付：{}",
                userId, order.getOrderNo(), originalPrice, couponDiscount, actualAmount);
        return convertToVO(order, productTypeEnum);
    }

    /**
     * 模拟支付
     * 1. 查询订单，校验归属和状态
     * 2. 更新订单状态为 PAID，记录支付时间
     * 3. 调用 VipService 开通/续费 VIP
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO mockPay(String orderNo, Long userId) {
        Orders order = getAndCheckOrder(orderNo, userId);
        if (!OrderStatusEnum.PENDING.getValue().equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "订单状态不允许支付（已支付或已取消）");
        }

        // 更新订单状态
        order.setStatus(OrderStatusEnum.PAID.getValue());
        order.setPayTime(new Date());
        this.updateById(order);

        // 触发 VIP 开通/续费
        ProductTypeEnum productTypeEnum = ProductTypeEnum.getEnumByValue(order.getProductType());
        if (productTypeEnum != null) {
            vipService.openOrRenewVip(userId, productTypeEnum.getDays());
        }

        log.info("用户[{}] 订单[{}] 模拟支付成功", userId, orderNo);
        return convertToVO(order, productTypeEnum);
    }

    /**
     * 查询订单详情
     */
    @Override
    public OrderVO getOrderByNo(String orderNo, Long userId) {
        Orders order = getAndCheckOrder(orderNo, userId);
        ProductTypeEnum productTypeEnum = ProductTypeEnum.getEnumByValue(order.getProductType());
        return convertToVO(order, productTypeEnum);
    }

    /**
     * 取消订单（仅 PENDING 状态可取消）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(String orderNo, Long userId) {
        Orders order = getAndCheckOrder(orderNo, userId);
        if (!OrderStatusEnum.PENDING.getValue().equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "只有待支付的订单才能取消");
        }
        order.setStatus(OrderStatusEnum.CANCELLED.getValue());
        this.updateById(order);
        log.info("用户[{}] 订单[{}] 已取消", userId, orderNo);
    }

    // ====== private 工具方法 ======

    /**
     * 查询并校验订单归属
     */
    private Orders getAndCheckOrder(String orderNo, Long userId) {
        if (orderNo == null || orderNo.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "订单编号不能为空");
        }
        Orders order = this.getOne(
                new LambdaQueryWrapper<Orders>().eq(Orders::getOrderNo, orderNo)
        );
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权操作此订单");
        }
        return order;
    }

    /**
     * 生成唯一订单编号：ORD + 时间戳毫秒 + 6位随机数
     */
    private String generateOrderNo() {
        return "ORD" + System.currentTimeMillis() + IdUtil.randomUUID().substring(0, 6).toUpperCase();
    }

    /**
     * 实体转 VO
     */
    private OrderVO convertToVO(Orders order, ProductTypeEnum productTypeEnum) {
        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setProductType(order.getProductType());
        vo.setProductTypeName(productTypeEnum != null ? productTypeEnum.getText() : order.getProductType());
        vo.setAmount(order.getAmount());
        vo.setOriginalAmount(order.getOriginalAmount());
        vo.setCouponDiscount(order.getCouponDiscount());
        vo.setCouponId(order.getCouponId());
        vo.setStatus(order.getStatus());
        OrderStatusEnum statusEnum = OrderStatusEnum.getEnumByValue(order.getStatus());
        vo.setStatusName(statusEnum != null ? statusEnum.getText() : order.getStatus());
        vo.setPayTime(order.getPayTime());
        vo.setCreateTime(order.getCreateTime());
        return vo;
    }
}
