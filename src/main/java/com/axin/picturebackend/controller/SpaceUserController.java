package com.axin.picturebackend.controller;

import com.axin.picturebackend.annotation.RoleCheck;
import com.axin.picturebackend.common.BaseResponse;
import com.axin.picturebackend.common.DeleteRequest;
import com.axin.picturebackend.common.ResultUtils;
import com.axin.picturebackend.constant.UserConstant;
import com.axin.picturebackend.manager.auth.SaSpaceCheckPermission;
import com.axin.picturebackend.manager.auth.model.SpaceUserPermissionConstant;
import com.axin.picturebackend.model.Enum.SpaceLevelEnum;
import com.axin.picturebackend.model.dto.space.*;
import com.axin.picturebackend.model.dto.spaceuser.SpaceUserAddRequest;
import com.axin.picturebackend.model.dto.spaceuser.SpaceUserEditRequest;
import com.axin.picturebackend.model.dto.spaceuser.SpaceUserQueryRequest;
import com.axin.picturebackend.model.entity.Space;
import com.axin.picturebackend.model.entity.SpaceUser;
import com.axin.picturebackend.model.vo.SpaceUserVO;
import com.axin.picturebackend.model.vo.SpaceVO;
import com.axin.picturebackend.service.SpaceService;
import com.axin.picturebackend.service.SpaceUserService;
import com.axin.picturebackend.service.UserService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/spaceUser")
public class SpaceUserController {

	@Resource
	private SpaceUserService spaceUserService;

	@Resource
	private UserService userService;

	/**
	 * 添加空间成员
	 *
	 * @param spaceUserAddRequest 添加空间成员请求
	 * @param request             请求
	 * @return 创建结果
	 */
	@PostMapping("/add")
	@SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
	public BaseResponse<Long> addSpaceUser(@RequestBody SpaceUserAddRequest spaceUserAddRequest, HttpServletRequest request) {
		return ResultUtils.success(spaceUserService.addSpaceUser(spaceUserAddRequest, userService.getLoginUser(request)));
	}

	/**
	 * 删除空间成员
	 *
	 * @param deleteRequest 删除条件
	 * @param request       请求
	 * @return 删除结果
	 */
	@PostMapping("/delete")
	@SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
	public BaseResponse<Boolean> deleteSpace(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
		return ResultUtils.success(spaceUserService.deleteSpace(deleteRequest, userService.getLoginUser(request)));
	}

	/**
	 * 获取空间成员信息
	 *
	 * @param spaceUserQueryRequest 查询条件
	 * @param request               请求
	 * @return 空间
	 */
	@GetMapping("/get")
	@SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
	public BaseResponse<SpaceUser> getSpaceUser(@RequestBody SpaceUserQueryRequest spaceUserQueryRequest, HttpServletRequest request) {
		return ResultUtils.success(spaceUserService.getSpaceUser(spaceUserQueryRequest, userService.getLoginUser(request)));
	}

	/**
	 * 编辑空间成员
	 *
	 * @param spaceUserEditRequest 编辑空间成员请求
	 * @param request              请求
	 * @return 编辑结果
	 */
	@PostMapping("/edit")
	@SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
	public BaseResponse<Boolean> editSpace(@RequestBody SpaceUserEditRequest spaceUserEditRequest, HttpServletRequest request) {
		return ResultUtils.success(spaceUserService.editSpaceUser(spaceUserEditRequest, userService.getLoginUser(request)));
	}

	/**
	 * 获取当前用户空间列表
	 *
	 * @param request 请求
	 * @return 空间列表
	 */
	@PostMapping("/list/my")
	public BaseResponse<List<SpaceUserVO>> listMySpaces(HttpServletRequest request) {
		return ResultUtils.success(spaceUserService.listMySpaces(userService.getLoginUser(request)));
	}

	/**
	 * 获取空间成员列表
	 *
	 * @param spaceUserQueryRequest 查询条件
	 * @param request               请求
	 * @return 列表
	 */
	@PostMapping("/list")
	@SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
	public BaseResponse<List<SpaceUserVO>> listSpaceUser(@RequestBody SpaceUserQueryRequest spaceUserQueryRequest, HttpServletRequest request) {
		Page<SpaceUserVO> spaceUserVOPage = spaceUserService.listSpaceUser(spaceUserQueryRequest, userService.getLoginUser(request));
		List<SpaceUserVO> records = spaceUserVOPage.getRecords();
		return ResultUtils.success(records);
	}
}
