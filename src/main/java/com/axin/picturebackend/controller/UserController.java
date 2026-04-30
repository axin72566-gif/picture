package com.axin.picturebackend.controller;

import com.axin.picturebackend.annotation.RoleCheck;
import com.axin.picturebackend.common.BaseResponse;
import com.axin.picturebackend.common.DeleteRequest;
import com.axin.picturebackend.common.ResultUtils;
import com.axin.picturebackend.constant.UserConstant;
import com.axin.picturebackend.exception.ErrorCode;
import com.axin.picturebackend.exception.ThrowUtils;
import com.axin.picturebackend.model.dto.user.*;
import com.axin.picturebackend.model.entity.User;
import com.axin.picturebackend.model.vo.LoginUserVO;
import com.axin.picturebackend.model.vo.UserVO;
import com.axin.picturebackend.service.UserService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    // ==================== 认证 ====================

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        ThrowUtils.throwIf(userRegisterRequest == null, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(userService.userRegister(userRegisterRequest));
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest,
                                               HttpServletRequest request) {
        ThrowUtils.throwIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(userService.userLogin(userLoginRequest, request));
    }

    /**
     * 用户注销
     */
    @PostMapping("/logout")
    @RoleCheck
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        return ResultUtils.success(userService.userLogout(request));
    }

    /**
     * 获取当前登录用户
     */
    @GetMapping("/get/login")
    @RoleCheck
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(userService.getLoginUserVO(loginUser));
    }

    /**
     * 修改当前用户信息
     */
    @PostMapping("/update/my")
    @RoleCheck
    public BaseResponse<Boolean> editUser(@RequestBody UserEditRequest userEditRequest) {
        ThrowUtils.throwIf(userEditRequest == null, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(userService.editUser(userEditRequest));
    }

    // ==================== 管理员接口 ====================

    /**
     * 删除用户（管理员）
     */
    @PostMapping("/delete")
    @RoleCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest) {
        ThrowUtils.throwIf(deleteRequest == null, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(userService.deleteUser(deleteRequest));
    }

    /**
     * 分页获取用户列表（管理员）
     */
    @PostMapping("/list/page/vo")
    @RoleCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<UserVO>> listUser(@RequestBody UserQueryRequest userQueryRequest) {
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(userService.listPageUserVO(userQueryRequest));
    }

    // ==================== 废弃接口 ====================

    /**
     * 获取用户原始信息（管理员）
     *
     * @deprecated 请使用 {@link #listUser} 获取用户列表
     */
    @Deprecated
    @GetMapping("/get")
    @RoleCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(@RequestParam Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(userService.getUserById(id));
    }

    /**
     * 获取用户VO（管理员）
     *
     * @deprecated 请使用 {@link #listUser} 获取用户列表
     */
    @Deprecated
    @GetMapping("/get/vo")
    @RoleCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<UserVO> getUserVOById(@RequestParam Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(userService.getUserVOById(id));
    }

    /**
     * 添加用户（管理员）
     *
     * @deprecated 已废弃
     */
    @Deprecated
    @PostMapping("/add")
    @RoleCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> addUser(@RequestBody UserAddRequest userAddRequest) {
        ThrowUtils.throwIf(userAddRequest == null, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(userService.addUser(userAddRequest));
    }

    /**
     * 更新用户（管理员）
     *
     * @deprecated 已废弃
     */
    @Deprecated
    @PostMapping("/update")
    @RoleCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {
        ThrowUtils.throwIf(userUpdateRequest == null, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(userService.updateUser(userUpdateRequest));
    }
}
