package com.axin.picturebackend.controller;

import com.axin.picturebackend.annotation.RoleCheck;
import com.axin.picturebackend.common.BaseResponse;
import com.axin.picturebackend.common.DeleteRequest;
import com.axin.picturebackend.common.ResultUtils;
import com.axin.picturebackend.constant.UserConstant;
import com.axin.picturebackend.model.dto.user.*;
import com.axin.picturebackend.model.entity.User;
import com.axin.picturebackend.model.vo.LoginUserVO;
import com.axin.picturebackend.model.vo.UserVO;
import com.axin.picturebackend.service.UserService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import static com.axin.picturebackend.constant.UserConstant.ADMIN_ROLE;

@RestController
@RequestMapping("/user")
public class UserController {

	@Resource
	private UserService userService;

	/**
	 * 用户注册
	 *
	 * @param userRegisterRequest 用户注册请求
	 * @return 用户id
	 */
	@PostMapping("/register")
	public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
		return ResultUtils.success(userService.userRegister(userRegisterRequest));
	}

	/**
	 * 用户登录
	 *
	 * @param userLoginRequest 用户登录请求
	 * @return 登录用户
	 */
	@PostMapping("/login")
	public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
		return ResultUtils.success(userService.userLogin(userLoginRequest, request));
	}

	/**
	 * 获取登录用户
	 *
	 * @param request 请求
	 * @return 登录用户
	 */
	@GetMapping("/get/login")
	@RoleCheck
	public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
		User loginUser = userService.getLoginUser(request);
		return ResultUtils.success(userService.getLoginUserVO(loginUser));
	}

	/**
	 * 用户注销
	 *
	 * @param request 请求
	 * @return 是否注销成功
	 */
	@PostMapping("/logout")
	@RoleCheck
	public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
		return ResultUtils.success(userService.userLogout(request));
	}

	/**
	 * 删除用户 管理员
	 *
	 * @param deleteRequest 删除请求
	 * @return 是否删除成功
	 */
	@PostMapping("/delete")
	@RoleCheck(mustRole = UserConstant.ADMIN_ROLE)
	public BaseResponse<Boolean> updateUser(@RequestBody DeleteRequest deleteRequest) {
		return ResultUtils.success(userService.deleteUser(deleteRequest));
	}

	/**
	 * 获取用户列表 管理员
	 *
	 * @param userQueryRequest 用户查询请求
	 * @return 用户列表
	 */
	@GetMapping("/list/page/vo")
	@RoleCheck(mustRole = UserConstant.ADMIN_ROLE)
	public BaseResponse<Page<UserVO>> listUser(@RequestBody UserQueryRequest userQueryRequest) {
		return ResultUtils.success(userService.listPageUserVO(userQueryRequest));
	}

	/**
	 * 获取用户 管理员 废弃
	 *
	 * @param id id
	 * @return 用户
	 */
	@Deprecated
	@GetMapping("/get")
	@RoleCheck(mustRole = ADMIN_ROLE)
	public BaseResponse<User> getUserById(Long id) {
		return ResultUtils.success(userService.getUserById(id));
	}

	/**
	 * 获取用户VO 管理员 废弃
	 *
	 * @param id id
	 * @return 用户VO
	 */
	@Deprecated
	@GetMapping("/get/vo")
	@RoleCheck(mustRole = ADMIN_ROLE)
	public BaseResponse<UserVO> getUserVOById(Long id) {
		return ResultUtils.success(userService.getUserVOById(id));
	}

	/**
	 * 添加用户 管理员 废弃
	 *
	 * @param userAddRequest 用户添加请求
	 * @return 是否添加成功
	 */
	@Deprecated
	@PostMapping("/add")
	@RoleCheck(mustRole = ADMIN_ROLE)
	public BaseResponse<Boolean> updateUser(@RequestBody UserAddRequest userAddRequest) {
		return ResultUtils.success(userService.addUser(userAddRequest));
	}

	/**
	 * 更新用户 管理员 废弃
	 *
	 * @param userUpdateRequest 修改用户请求
	 * @return 修改结果
	 */
	@Deprecated
	@PostMapping("/update")
	@RoleCheck(mustRole = UserConstant.ADMIN_ROLE)
	public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {
		return ResultUtils.success(userService.updateUser(userUpdateRequest));
	}

}