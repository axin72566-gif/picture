package com.axin.picturebackend.controller;

import com.axin.picturebackend.annotation.RoleCheck;
import com.axin.picturebackend.common.BaseResponse;
import com.axin.picturebackend.common.ResultUtils;
import com.axin.picturebackend.exception.ErrorCode;
import com.axin.picturebackend.exception.ThrowUtils;
import com.axin.picturebackend.model.dto.user.UserFollowRequest;
import com.axin.picturebackend.model.entity.User;
import com.axin.picturebackend.model.vo.UserVO;
import com.axin.picturebackend.service.UserFollowService;
import com.axin.picturebackend.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 用户关注控制器
 */
@RestController
@RequestMapping("/user_follow")
public class UserFollowController {

    @Resource
    private UserFollowService userFollowService;

    @Resource
    private UserService userService;

    /**
     * 关注 / 取消关注
     *
     * @param userFollowRequest 关注请求
     * @param request           HTTP请求
     * @return true=关注成功, false=取消关注
     */
    @PostMapping("/do")
    @RoleCheck
    public BaseResponse<Boolean> doFollow(@RequestBody UserFollowRequest userFollowRequest,
                                          HttpServletRequest request) {
        ThrowUtils.throwIf(userFollowRequest == null || userFollowRequest.getFollowUserId() == null,
                ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        boolean result = userFollowService.doFollow(userFollowRequest.getFollowUserId(), loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 判断是否已关注
     *
     * @param followUserId 被关注用户ID
     * @param request      HTTP请求
     * @return true=已关注
     */
    @GetMapping("/is_follow")
    @RoleCheck
    public BaseResponse<Boolean> isFollowed(@RequestParam Long followUserId, HttpServletRequest request) {
        ThrowUtils.throwIf(followUserId == null || followUserId <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        boolean result = userFollowService.isFollowed(loginUser.getId(), followUserId);
        return ResultUtils.success(result);
    }

    /**
     * 获取我的关注列表
     *
     * @param request HTTP请求
     * @return 关注的用户列表
     */
    @GetMapping("/list/my")
    @RoleCheck
    public BaseResponse<List<UserVO>> listMyFollowing(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        List<User> userList = userFollowService.listFollowing(loginUser.getId());
        return ResultUtils.success(userService.listUserVO(userList));
    }
}
