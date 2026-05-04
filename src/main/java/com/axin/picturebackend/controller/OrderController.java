package com.axin.picturebackend.controller;

import com.axin.picturebackend.common.BaseResponse;
import com.axin.picturebackend.common.ResultUtils;
import com.axin.picturebackend.exception.ErrorCode;
import com.axin.picturebackend.exception.ThrowUtils;
import com.axin.picturebackend.model.dto.order.OrderCreateRequest;
import com.axin.picturebackend.model.dto.order.OrderPayRequest;
import com.axin.picturebackend.model.entity.User;
import com.axin.picturebackend.model.vo.OrderVO;
import com.axin.picturebackend.service.OrderService;
import com.axin.picturebackend.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 订单控制器
 */
@RestController
@RequestMapping("/order")
public class OrderController {

    @Resource
    private OrderService orderService;

    @Resource
    private UserService userService;

    /**
     * 创建订单
     * POST /order/create
     */
    @PostMapping("/create")
    public BaseResponse<OrderVO> createOrder(@RequestBody OrderCreateRequest request,
                                             HttpServletRequest httpRequest) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(request.getProductType() == null || request.getProductType().isEmpty(),
                ErrorCode.PARAMS_ERROR, "商品类型不能为空");
        User loginUser = userService.getLoginUser(httpRequest);
        OrderVO orderVO = orderService.createOrder(loginUser.getId(), request.getProductType(), request.getCouponId());
        return ResultUtils.success(orderVO);
    }

    /**
     * 模拟支付
     * POST /order/pay
     */
    @PostMapping("/pay")
    public BaseResponse<OrderVO> mockPay(@RequestBody OrderPayRequest request,
                                         HttpServletRequest httpRequest) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(request.getOrderNo() == null || request.getOrderNo().isEmpty(),
                ErrorCode.PARAMS_ERROR, "订单编号不能为空");
        User loginUser = userService.getLoginUser(httpRequest);
        OrderVO orderVO = orderService.mockPay(request.getOrderNo(), loginUser.getId());
        return ResultUtils.success(orderVO);
    }

    /**
     * 查询订单详情
     * GET /order/get?orderNo=xxx
     */
    @GetMapping("/get")
    public BaseResponse<OrderVO> getOrder(@RequestParam String orderNo,
                                          HttpServletRequest httpRequest) {
        ThrowUtils.throwIf(orderNo == null || orderNo.isEmpty(), ErrorCode.PARAMS_ERROR, "订单编号不能为空");
        User loginUser = userService.getLoginUser(httpRequest);
        OrderVO orderVO = orderService.getOrderByNo(orderNo, loginUser.getId());
        return ResultUtils.success(orderVO);
    }

    /**
     * 取消订单
     * POST /order/cancel
     */
    @PostMapping("/cancel")
    public BaseResponse<Boolean> cancelOrder(@RequestBody OrderPayRequest request,
                                             HttpServletRequest httpRequest) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(request.getOrderNo() == null || request.getOrderNo().isEmpty(),
                ErrorCode.PARAMS_ERROR, "订单编号不能为空");
        User loginUser = userService.getLoginUser(httpRequest);
        orderService.cancelOrder(request.getOrderNo(), loginUser.getId());
        return ResultUtils.success(true);
    }
}
