package com.axin.picturebackend.controller;

import com.axin.picturebackend.annotation.RoleCheck;
import com.axin.picturebackend.common.BaseResponse;
import com.axin.picturebackend.common.DeleteRequest;
import com.axin.picturebackend.common.ResultUtils;
import com.axin.picturebackend.constant.UserConstant;
import com.axin.picturebackend.exception.ErrorCode;
import com.axin.picturebackend.exception.ThrowUtils;
import com.axin.picturebackend.model.Enum.SpaceLevelEnum;
import com.axin.picturebackend.model.dto.space.*;
import com.axin.picturebackend.model.entity.Space;
import com.axin.picturebackend.model.entity.User;
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

/**
 * 空间控制器
 */
@RestController
@RequestMapping("/space")
public class SpaceController {

    @Resource
    private SpaceService spaceService;

    @Resource
    private UserService userService;

    // ==================== 空间管理 ====================

    /**
     * 创建空间（登录用户）
     */
    @PostMapping("/add")
    @RoleCheck
    public BaseResponse<Long> addSpace(@RequestBody SpaceAddRequest spaceAddRequest,
                                       HttpServletRequest request) {
        ThrowUtils.throwIf(spaceAddRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(spaceService.addSpace(spaceAddRequest, loginUser));
    }

    /**
     * 编辑空间（登录用户）
     */
    @PostMapping("/edit")
    @RoleCheck
    public BaseResponse<Boolean> editSpace(@RequestBody SpaceEditRequest spaceEditRequest,
                                           HttpServletRequest request) {
        ThrowUtils.throwIf(spaceEditRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(spaceService.editSpace(spaceEditRequest, loginUser));
    }

    /**
     * 删除空间（登录用户）
     */
    @PostMapping("/delete")
    @RoleCheck
    public BaseResponse<Boolean> deleteSpace(@RequestBody DeleteRequest deleteRequest,
                                             HttpServletRequest request) {
        ThrowUtils.throwIf(deleteRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(spaceService.deleteSpace(deleteRequest, loginUser));
    }

    /**
     * 修改空间（管理员）
     */
    @PostMapping("/update")
    @RoleCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateSpace(@RequestBody SpaceUpdateRequest spaceUpdateRequest) {
        ThrowUtils.throwIf(spaceUpdateRequest == null, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(spaceService.updateSpace(spaceUpdateRequest));
    }

    // ==================== 空间查询 ====================

    /**
     * 获取空间VO（登录用户）
     */
    @GetMapping("/get/vo")
    @RoleCheck
    public BaseResponse<SpaceVO> getSpaceVOById(@RequestParam Long id,
                                                HttpServletRequest request) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR, "空间ID不合法");
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(spaceService.getSpaceVOById(id, loginUser));
    }

    /**
     * 获取空间原始信息（管理员）
     */
    @GetMapping("/get")
    @RoleCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Space> getSpaceById(@RequestParam Long id,
                                            HttpServletRequest request) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(spaceService.getSpaceById(id, loginUser));
    }

    /**
     * 分页获取空间VO列表（登录用户）
     */
    @PostMapping("/list/page/vo")
    @RoleCheck
    public BaseResponse<Page<SpaceVO>> listPageSpaceVO(@RequestBody SpaceQueryRequest spaceQueryRequest,
                                                       HttpServletRequest request) {
        ThrowUtils.throwIf(spaceQueryRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(spaceService.listPageSpaceVO(spaceQueryRequest, loginUser));
    }

    /**
     * 分页获取空间列表（管理员）
     */
    @PostMapping("/list/page")
    @RoleCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Space>> listPageSpace(@RequestBody SpaceQueryRequest spaceQueryRequest,
                                                   HttpServletRequest request) {
        ThrowUtils.throwIf(spaceQueryRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(spaceService.listPageSpace(spaceQueryRequest, loginUser));
    }

    // ==================== 枚举数据 ====================

    /**
     * 获取所有空间等级枚举（公开）
     */
    @GetMapping("/list/level")
    public BaseResponse<List<SpaceLevel>> listSpaceLevel() {
        List<SpaceLevel> spaceLevelList = Arrays.stream(SpaceLevelEnum.values())
                .map(e -> new SpaceLevel(e.getValue(), e.getText(), e.getMaxCount(), e.getMaxSize()))
                .collect(Collectors.toList());
        return ResultUtils.success(spaceLevelList);
    }
}
