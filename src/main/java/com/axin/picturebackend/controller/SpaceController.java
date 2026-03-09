package com.axin.picturebackend.controller;

import com.axin.picturebackend.annotation.RoleCheck;
import com.axin.picturebackend.common.BaseResponse;
import com.axin.picturebackend.common.DeleteRequest;
import com.axin.picturebackend.common.ResultUtils;
import com.axin.picturebackend.constant.UserConstant;
import com.axin.picturebackend.model.Enum.SpaceLevelEnum;
import com.axin.picturebackend.model.dto.space.*;
import com.axin.picturebackend.model.entity.Space;
import com.axin.picturebackend.model.vo.SpaceVO;
import com.axin.picturebackend.service.SpaceService;
import com.axin.picturebackend.service.UserService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/space")
public class SpaceController {

	@Resource
	private SpaceService spaceService;
	@Resource
	private UserService userService;

	/**
	 * 创建空间
	 *
	 * @param spaceAddRequest 创建空间请求
	 * @param request         请求
	 * @return 创建结果
	 */
	@PostMapping("/add")
	@RoleCheck
	public BaseResponse<Long> addSpace(@RequestBody SpaceAddRequest spaceAddRequest, HttpServletRequest request) {
		return ResultUtils.success(spaceService.addSpace(spaceAddRequest, userService.getLoginUser(request)));
	}

	/**
	 * 修改空间 管理员
	 *
	 * @param spaceUpdateRequest 修改空间请求
	 * @return 修改结果
	 */
	@PostMapping("/update")
	@RoleCheck(mustRole = UserConstant.ADMIN_ROLE)
	public BaseResponse<Boolean> updateSpace(@RequestBody SpaceUpdateRequest spaceUpdateRequest) {
		return ResultUtils.success(spaceService.updateSpace(spaceUpdateRequest));
	}

	/**
	 * 删除空间
	 *
	 * @param deleteRequest 删除条件
	 * @param request       请求
	 * @return 删除结果
	 */
	@PostMapping("/delete")
	@RoleCheck
	public BaseResponse<Boolean> deleteSpace(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
		return ResultUtils.success(spaceService.deleteSpace(deleteRequest, userService.getLoginUser(request)));
	}

	/**
	 * 获取空间
	 *
	 * @param id      id
	 * @param request 请求
	 * @return 空间
	 */
	@GetMapping("/get")
	@RoleCheck(mustRole = UserConstant.ADMIN_ROLE)
	public BaseResponse<Space> getSpaceById(Long id, HttpServletRequest request) {
		return ResultUtils.success(spaceService.getSpaceById(id, userService.getLoginUser(request)));
	}

	/**
	 * 获取空间
	 *
	 * @param id      id
	 * @param request 请求
	 * @return 空间
	 */
	@GetMapping("/get/vo")
	@RoleCheck
	public BaseResponse<SpaceVO> getSpaceVOById(Long id, HttpServletRequest request) {
		return ResultUtils.success(spaceService.getSpaceVOById(id, userService.getLoginUser(request)));
	}

	/**
	 * 获取空间列表
	 *
	 * @param spaceQueryRequest 查询条件
	 * @param request           请求
	 * @return 空间列表
	 */
	@PostMapping("/list/page")
	@RoleCheck(mustRole = UserConstant.ADMIN_ROLE)
	public BaseResponse<Page<Space>> listPageSpace(@RequestBody SpaceQueryRequest spaceQueryRequest, HttpServletRequest request) {
		return ResultUtils.success(spaceService.listPageSpace(spaceQueryRequest, userService.getLoginUser(request)));
	}

	/**
	 * 获取空间列表
	 *
	 * @param spaceQueryRequest 查询条件
	 * @param request           请求
	 * @return 空间列表视图
	 */
	@PostMapping("/list/page/vo")
	@RoleCheck
	public BaseResponse<Page<SpaceVO>> listPageSpaceVO(@RequestBody SpaceQueryRequest spaceQueryRequest, HttpServletRequest request) {
		return ResultUtils.success(spaceService.listPageSpaceVO(spaceQueryRequest, userService.getLoginUser(request)));
	}

	/**
	 * 编辑空间
	 *
	 * @param spaceEditRequest 编辑空间请求
	 * @param request          请求
	 * @return 编辑结果
	 */
	@PostMapping("/edit")
	@RoleCheck
	public BaseResponse<Boolean> editSpace(@RequestBody SpaceEditRequest spaceEditRequest, HttpServletRequest request) {
		return ResultUtils.success(spaceService.editSpace(spaceEditRequest, userService.getLoginUser(request)));
	}

	/**
	 * 获取空间等级
	 *
	 * @return 空间等级
	 */
	@GetMapping("/list/level")
	public BaseResponse<List<SpaceLevel>> listSpaceLevel() {
		List<SpaceLevel> spaceLevelList = Arrays.stream(SpaceLevelEnum.values()) // 获取所有枚举
				.map(spaceLevelEnum -> new SpaceLevel(
						spaceLevelEnum.getValue(),
						spaceLevelEnum.getText(),
						spaceLevelEnum.getMaxCount(),
						spaceLevelEnum.getMaxSize()))
				.collect(Collectors.toList());
		return ResultUtils.success(spaceLevelList);
	}

}
