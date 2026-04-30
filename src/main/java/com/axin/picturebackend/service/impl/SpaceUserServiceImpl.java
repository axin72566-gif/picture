package com.axin.picturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.axin.picturebackend.common.DeleteRequest;
import com.axin.picturebackend.exception.BusinessException;
import com.axin.picturebackend.exception.ErrorCode;
import com.axin.picturebackend.model.Enum.SpaceRoleEnum;
import com.axin.picturebackend.model.dto.spaceuser.SpaceUserAddRequest;
import com.axin.picturebackend.model.dto.spaceuser.SpaceUserEditRequest;
import com.axin.picturebackend.model.dto.spaceuser.SpaceUserQueryRequest;
import com.axin.picturebackend.model.entity.Space;
import com.axin.picturebackend.model.entity.SpaceUser;
import com.axin.picturebackend.model.entity.User;
import com.axin.picturebackend.model.vo.SpaceUserVO;
import com.axin.picturebackend.model.vo.SpaceVO;
import com.axin.picturebackend.model.vo.UserVO;
import com.axin.picturebackend.service.SpaceService;
import com.axin.picturebackend.service.SpaceUserService;
import com.axin.picturebackend.service.UserService;
import com.axin.picturebackend.mapper.SpaceUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 空间用户关联 Service 实现
 */
@Service
public class SpaceUserServiceImpl extends ServiceImpl<SpaceUserMapper, SpaceUser>
        implements SpaceUserService {

    @Resource
    private SpaceService spaceService;
    @Resource
    private UserService userService;
    @Resource
    private TransactionTemplate transactionTemplate;

    // ====== 增删改 ======

    /**
     * 添加空间成员
     */
    @Override
    public Long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest, User loginUser) {
        if (spaceUserAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        SpaceUser spaceUser = new SpaceUser();
        BeanUtil.copyProperties(spaceUserAddRequest, spaceUser);
        validSpaceUser(spaceUser);
        if (!this.save(spaceUser)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "添加失败");
        }
        return spaceUser.getId();
    }

    /**
     * 删除空间成员
     */
    @Override
    public Boolean deleteSpaceUser(DeleteRequest deleteRequest, User loginUser) {
        if (deleteRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "删除参数为空");
        }
        Long id = deleteRequest.getId();
        if (this.getById(id) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "空间成员不存在");
        }
        if (!this.removeById(id)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "删除失败");
        }
        return true;
    }

    /**
     * 编辑空间成员角色
     */
    @Override
    public Boolean editSpaceUser(SpaceUserEditRequest spaceUserEditRequest, User loginUser) {
        if (spaceUserEditRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        Long id = spaceUserEditRequest.getId();
        if (this.getById(id) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "空间成员不存在");
        }
        SpaceUser updateSpaceUser = new SpaceUser();
        BeanUtil.copyProperties(spaceUserEditRequest, updateSpaceUser);
        if (!this.updateById(updateSpaceUser)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新失败");
        }
        return true;
    }

    // ====== 校验 ======

    /**
     * 校验空间用户（空间/用户存在性、角色合法性）
     */
    @Override
    public void validSpaceUser(SpaceUser spaceUser) {
        if (spaceUser == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        Long spaceId = spaceUser.getSpaceId();
        if (spaceService.getById(spaceId) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        }
        Long userId = spaceUser.getUserId();
        if (userService.getById(userId) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }
        String spaceRole = spaceUser.getSpaceRole();
        if (!SpaceRoleEnum.getAllValues().contains(spaceRole)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间角色错误");
        }
    }

    // ====== 查询 ======

    /**
     * 获取空间成员视图（含空间信息和用户信息）
     */
    @Override
    public SpaceUserVO getSpaceUserVO(SpaceUser spaceUser, User loginUser) {
        if (spaceUser == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        SpaceUserVO spaceUserVO = SpaceUserVO.objToVo(spaceUser);
        SpaceVO spaceVO = spaceService.getSpaceVOById(spaceUser.getSpaceId(), loginUser);
        spaceUserVO.setSpace(spaceVO);
        UserVO userVO = userService.getUserVO(userService.getById(spaceUser.getUserId()));
        spaceUserVO.setUser(userVO);
        return spaceUserVO;
    }

    /**
     * 批量获取空间成员视图
     */
    @Override
    public List<SpaceUserVO> listSpaceUserVO(List<SpaceUser> spaceUserList, User loginUser) {
        if (spaceUserList == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        return spaceUserList.stream()
                .map(spaceUser -> {
                    SpaceUserVO vo = SpaceUserVO.objToVo(spaceUser);
                    SpaceVO spaceVO = spaceService.getSpaceVOById(spaceUser.getSpaceId(), loginUser);
                    vo.setSpace(spaceVO);
                    vo.setUser(userService.getUserVO(userService.getById(spaceUser.getUserId())));
                    return vo;
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取单个空间成员记录
     */
    @Override
    public SpaceUser getSpaceUser(SpaceUserQueryRequest spaceUserQueryRequest, User loginUser) {
        if (spaceUserQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        SpaceUser one = this.getOne(getQueryWrapper(spaceUserQueryRequest));
        if (one == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "空间成员不存在");
        }
        return one;
    }

    /**
     * 获取当前用户参与的所有团队空间，无结果时返回空列表
     */
    @Override
    public List<SpaceUserVO> listMySpaces(User loginUser) {
        Long userId = loginUser.getId();
        List<SpaceUser> spaceUserList = this.list(new QueryWrapper<SpaceUser>().eq("userId", userId));
        if (spaceUserList.isEmpty()) {
            return Collections.emptyList();
        }
        return listSpaceUserVO(spaceUserList, loginUser);
    }

    /**
     * 分页查询空间成员
     */
    @Override
    public Page<SpaceUserVO> listSpaceUser(SpaceUserQueryRequest spaceUserQueryRequest, User loginUser) {
        if (spaceUserQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        long current = spaceUserQueryRequest.getCurrent();
        long pageSize = spaceUserQueryRequest.getPageSize();
        QueryWrapper<SpaceUser> queryWrapper = getQueryWrapper(spaceUserQueryRequest);
        Page<SpaceUser> spaceUserPage = this.page(new Page<>(current, pageSize), queryWrapper);
        List<SpaceUserVO> spaceUserVOS = listSpaceUserVO(spaceUserPage.getRecords(), loginUser);
        Page<SpaceUserVO> spaceUserVOPage = new Page<>(current, pageSize, spaceUserPage.getTotal());
        spaceUserVOPage.setRecords(spaceUserVOS);
        return spaceUserVOPage;
    }

    /**
     * 构建查询条件
     */
    @Override
    public QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest) {
        if (spaceUserQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        Long id = spaceUserQueryRequest.getId();
        Long spaceId = spaceUserQueryRequest.getSpaceId();
        Long userId = spaceUserQueryRequest.getUserId();
        String spaceRole = spaceUserQueryRequest.getSpaceRole();
        String sortField = spaceUserQueryRequest.getSortField();
        String sortOrder = spaceUserQueryRequest.getSortOrder();
        QueryWrapper<SpaceUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(id != null, "id", id);
        queryWrapper.eq(spaceId != null, "spaceId", spaceId);
        queryWrapper.eq(userId != null, "userId", userId);
        queryWrapper.eq(spaceRole != null, "spaceRole", spaceRole);
        queryWrapper.orderBy(StringUtils.isNotBlank(sortField), "ascend".equals(sortOrder), sortField);
        return queryWrapper;
    }
}
