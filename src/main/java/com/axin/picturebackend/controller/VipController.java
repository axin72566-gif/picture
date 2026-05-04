package com.axin.picturebackend.controller;

import com.axin.picturebackend.common.BaseResponse;
import com.axin.picturebackend.common.ResultUtils;
import com.axin.picturebackend.model.entity.User;
import com.axin.picturebackend.model.vo.UserVipVO;
import com.axin.picturebackend.service.UserService;
import com.axin.picturebackend.service.VipService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 会员控制器
 */
@RestController
@RequestMapping("/vip")
public class VipController {

    @Resource
    private VipService vipService;

    @Resource
    private UserService userService;

    /**
     * 查询当前用户 VIP 信息
     * GET /vip/info
     */
    @GetMapping("/info")
    public BaseResponse<UserVipVO> getMyVipInfo(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        UserVipVO vipVO = vipService.getVipInfo(loginUser.getId());
        return ResultUtils.success(vipVO);
    }

    /**
     * 查询当前用户是否 VIP（快速判断）
     * GET /vip/status
     */
    @GetMapping("/status")
    public BaseResponse<Boolean> getVipStatus(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        boolean isVip = vipService.isVip(loginUser.getId());
        return ResultUtils.success(isVip);
    }
}
