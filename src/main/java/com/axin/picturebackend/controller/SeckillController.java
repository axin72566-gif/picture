package com.axin.picturebackend.controller;

import com.axin.picturebackend.common.BaseResponse;
import com.axin.picturebackend.common.ResultUtils;
import com.axin.picturebackend.exception.ErrorCode;
import com.axin.picturebackend.exception.ThrowUtils;
import com.axin.picturebackend.model.dto.seckill.SeckillBuyRequest;
import com.axin.picturebackend.model.entity.User;
import com.axin.picturebackend.model.vo.SeckillActivityVO;
import com.axin.picturebackend.service.SeckillService;
import com.axin.picturebackend.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 秒杀控制器
 */
@RestController
@RequestMapping("/seckill")
public class SeckillController {

    @Resource
    private SeckillService seckillService;

    @Resource
    private UserService userService;

    /**
     * 用户抢购优惠券
     * POST /seckill/buy
     */
    @PostMapping("/buy")
    public BaseResponse<String> buyCoupon(@RequestBody SeckillBuyRequest request,
                                          HttpServletRequest httpRequest) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(request.getActivityId() == null, ErrorCode.PARAMS_ERROR, "活动ID不能为空");
        User loginUser = userService.getLoginUser(httpRequest);
        String result = seckillService.buyCoupon(loginUser.getId(), request.getActivityId());
        return ResultUtils.success(result);
    }

    /**
     * 查询当前有效的秒杀活动列表
     * GET /seckill/list
     */
    @GetMapping("/list")
    public BaseResponse<List<SeckillActivityVO>> listActiveActivities() {
        List<SeckillActivityVO> activities = seckillService.listActiveActivities();
        return ResultUtils.success(activities);
    }

    /**
     * 管理员初始化活动库存到 Redis（新增活动或服务重启后调用）
     * POST /seckill/initStock?activityId=xxx
     */
    @PostMapping("/initStock")
    public BaseResponse<Boolean> initStock(@RequestParam Long activityId,
                                           HttpServletRequest httpRequest) {
        ThrowUtils.throwIf(activityId == null, ErrorCode.PARAMS_ERROR, "活动ID不能为空");
        User loginUser = userService.getLoginUser(httpRequest);
        ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR);
        seckillService.initStockToRedis(activityId);
        return ResultUtils.success(true);
    }
}
