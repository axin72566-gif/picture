package com.axin.picturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.axin.picturebackend.common.DeleteRequest;
import com.axin.picturebackend.exception.BusinessException;
import com.axin.picturebackend.exception.ErrorCode;
import com.axin.picturebackend.exception.ThrowUtils;
import com.axin.picturebackend.manager.CosManager;
import com.axin.picturebackend.manager.auth.SpaceUserAuthManager;
import com.axin.picturebackend.model.Enum.SpaceLevelEnum;
import com.axin.picturebackend.model.Enum.SpaceRoleEnum;
import com.axin.picturebackend.model.Enum.SpaceTypeEnum;
import com.axin.picturebackend.model.dto.space.SpaceAddRequest;
import com.axin.picturebackend.model.dto.space.SpaceEditRequest;
import com.axin.picturebackend.model.dto.space.SpaceQueryRequest;
import com.axin.picturebackend.model.dto.space.SpaceUpdateRequest;
import com.axin.picturebackend.model.entity.Space;
import com.axin.picturebackend.model.entity.SpaceUser;
import com.axin.picturebackend.model.entity.User;
import com.axin.picturebackend.model.vo.SpaceVO;
import com.axin.picturebackend.model.vo.UserVO;
import com.axin.picturebackend.service.SpaceService;
import com.axin.picturebackend.service.SpaceUserService;
import com.axin.picturebackend.service.UserService;
import com.axin.picturebackend.mapper.SpaceMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 空间 Service 实现
 */
@Service
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space>
        implements SpaceService {

    @Resource
    private TransactionTemplate transactionTemplate;
    @Resource
    private UserService userService;
    @Lazy
    @Resource
    private SpaceUserService spaceUserService;
    @Lazy
    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;
    @Resource
    private CosManager cosManager;

    // ====== 校验 / 填充 ======

    /**
     * 校验空间名称和级别
     *
     * @param space 空间
     * @param add   true=创建，false=修改
     */
    @Override
    public void validSpace(Space space, boolean add) {
        if (space == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间为空");
        }
        String spaceName = space.getSpaceName();
        Integer spaceLevel = space.getSpaceLevel();
        Integer spaceType = space.getSpaceType();
        if (add) {
            // 创建时：名称和级别必填
            if (spaceName == null || spaceLevel == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称和空间级别不能为空");
            }
            if (spaceName.length() > 20) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称过长");
            }
        } else {
            // 修改时：名称不为空且长度合法
            if (spaceName == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称不能为空");
            }
            if (spaceName.length() > 20) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称过长");
            }
            if (spaceLevel == null || SpaceLevelEnum.getEnumByValue(spaceLevel) == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间级别不存在");
            }
        }
        if (spaceType == null || (spaceType != 0 && spaceType != 1)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间类型错误");
        }
    }

    /**
     * 根据空间级别枚举自动填充额度（maxCount / maxSize）
     */
    @Override
    public void fillSpaceParam(Space space) {
        if (space == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间为空");
        }
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(space.getSpaceLevel());
        if (spaceLevelEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间级别不存在");
        }
        space.setMaxCount(spaceLevelEnum.getMaxCount());
        space.setMaxSize(spaceLevelEnum.getMaxSize());
    }

    // ====== 增删改 ======

    /**
     * 创建空间（对用户ID加锁，防重复建同类型空间；团队空间自动加入创建者）
     */
    @Override
    public Long addSpace(SpaceAddRequest spaceAddRequest, User loginUser) {
        if (spaceAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间创建参数为空");
        }
        Space newSpace = new Space();
        BeanUtil.copyProperties(spaceAddRequest, newSpace);
        // 默认值填充
        if (newSpace.getSpaceName() == null) {
            newSpace.setSpaceName("默认空间");
        }
        if (newSpace.getSpaceLevel() == null) {
            newSpace.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
        }
        if (newSpace.getSpaceType() == null) {
            newSpace.setSpaceType(SpaceTypeEnum.PRIVATE.getValue());
        }
        validSpace(newSpace, true);
        fillSpaceParam(newSpace);
        newSpace.setUserId(loginUser.getId());
        // 非普通版空间需要管理员权限
        if (newSpace.getSpaceLevel() != SpaceLevelEnum.COMMON.getValue() && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "用户无权限创建旗舰版空间");
        }
        // 对用户ID加锁，防止并发创建同类型空间
        String lock = String.valueOf(loginUser.getId()).intern();
        synchronized (lock) {
            return transactionTemplate.execute(transactionStatus -> {
                Long userId = loginUser.getId();
                Long spaceType = Long.valueOf(newSpace.getSpaceType());
                long existCount = this.count(new QueryWrapper<Space>()
                        .eq("userId", userId)
                        .eq("spaceType", spaceType));
                if (existCount > 0) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "用户已存在此类型空间");
                }
                boolean save = this.save(newSpace);
                if (!save) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "创建空间失败");
                }
                // 团队空间：创建者自动成为管理员成员
                if (newSpace.getSpaceType() == SpaceTypeEnum.TEAM.getValue()) {
                    SpaceUser spaceUser = new SpaceUser();
                    spaceUser.setSpaceId(newSpace.getId());
                    spaceUser.setUserId(loginUser.getId());
                    spaceUser.setSpaceRole(SpaceRoleEnum.ADMIN.getValue());
                    if (!spaceUserService.save(spaceUser)) {
                        throw new BusinessException(ErrorCode.OPERATION_ERROR, "创建空间失败");
                    }
                }
                return newSpace.getId();
            });
        }
    }

    /**
     * 管理员更新空间（重新填充额度）
     */
    @Override
    public Boolean updateSpace(SpaceUpdateRequest spaceUpdateRequest) {
        if (spaceUpdateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "修改空间参数为空");
        }
        Long id = spaceUpdateRequest.getId();
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间id错误");
        }
        if (this.getById(id) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        }
        Space newSpace = new Space();
        BeanUtil.copyProperties(spaceUpdateRequest, newSpace);
        validSpace(newSpace, false);
        fillSpaceParam(newSpace);
        if (!this.updateById(newSpace)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "修改空间失败");
        }
        return true;
    }

    /**
     * 用户编辑空间名称（仅空间创建者可操作）
     */
    @Override
    public Boolean editSpace(SpaceEditRequest spaceEditRequest, User loginUser) {
        if (spaceEditRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "编辑空间参数为空");
        }
        Long id = spaceEditRequest.getId();
        Space space = this.getById(id);
        if (space == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        }
        if (!loginUser.getId().equals(space.getUserId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限编辑空间");
        }
        Space updateSpace = new Space();
        updateSpace.setId(id);
        updateSpace.setSpaceName(spaceEditRequest.getSpaceName());
        if (!this.updateById(updateSpace)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "编辑空间失败");
        }
        return true;
    }

    /**
     * 删除空间
     */
    @Override
    public boolean deleteSpace(DeleteRequest deleteRequest, User loginUser) {
        if (deleteRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "删除空间参数为空");
        }
        Long id = deleteRequest.getId();
        if (this.getById(id) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        }
        if (!this.removeById(id)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "删除空间失败");
        }
        return true;
    }

    // ====== 查询 ======

    /**
     * 根据ID获取空间实体
     */
    @Override
    public Space getSpaceById(Long id, User loginUser) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间id错误");
        }
        Space space = this.getById(id);
        if (space == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        }
        return space;
    }

    /**
     * 根据ID获取空间视图（含用户信息和权限列表）
     */
    @Override
    public SpaceVO getSpaceVOById(Long id, User loginUser) {
        Space space = getSpaceById(id, loginUser);
        SpaceVO spaceVO = SpaceVO.objToVo(space);
        Long userId = space.getUserId();
        ThrowUtils.throwIf(userId == null, ErrorCode.NOT_FOUND_ERROR, "用户id不存在");
        User user = userService.getById(userId);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        spaceVO.setUser(userVO);
        spaceVO.setPermissionList(spaceUserAuthManager.getPermissionList(space, loginUser));
        return spaceVO;
    }

    /**
     * 构建空间查询条件
     */
    @Override
    public QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest) {
        if (spaceQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "查询条件为空");
        }
        Long id = spaceQueryRequest.getId();
        Long userId = spaceQueryRequest.getUserId();
        String spaceName = spaceQueryRequest.getSpaceName();
        Integer spaceLevel = spaceQueryRequest.getSpaceLevel();
        Integer spaceType = spaceQueryRequest.getSpaceType();
        String sortField = spaceQueryRequest.getSortField();
        String sortOrder = spaceQueryRequest.getSortOrder();
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(id != null && id > 0, "id", id);
        queryWrapper.eq(userId != null && userId > 0, "userId", userId);
        queryWrapper.like(StringUtils.isNotBlank(spaceName), "spaceName", spaceName);
        queryWrapper.eq(spaceLevel != null, "spaceLevel", spaceLevel);
        queryWrapper.eq(spaceType != null, "spaceType", spaceType);
        queryWrapper.orderBy(StringUtils.isNotBlank(sortField), "ascend".equals(sortOrder), sortField);
        return queryWrapper;
    }

    /**
     * 分页查询空间实体
     */
    @Override
    public Page<Space> listPageSpace(SpaceQueryRequest spaceQueryRequest, User loginUser) {
        ThrowUtils.throwIf(spaceQueryRequest == null, ErrorCode.PARAMS_ERROR, "查询条件为空");
        int current = spaceQueryRequest.getCurrent();
        int pageSize = spaceQueryRequest.getPageSize();
        QueryWrapper<Space> queryWrapper = getQueryWrapper(spaceQueryRequest);
        Page<Space> spacePage = this.page(new Page<>(current, pageSize), queryWrapper);
        if (spacePage == null || spacePage.getTotal() <= 0) {
            return new Page<>();
        }
        return spacePage;
    }

    /**
     * 分页查询空间视图（含关联用户信息）
     */
    @Override
    public Page<SpaceVO> listPageSpaceVO(SpaceQueryRequest spaceQueryRequest, User loginUser) {
        Page<Space> spacePage = listPageSpace(spaceQueryRequest, loginUser);
        if (spacePage == null || spacePage.getTotal() <= 0) {
            return new Page<>();
        }
        List<Space> spaceList = spacePage.getRecords();
        List<Long> userIdList = spaceList.stream().map(Space::getUserId).collect(Collectors.toList());
        Map<Long, User> userMap = userService.listByIds(userIdList).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        List<SpaceVO> spaceVOList = spaceList.stream().map(space -> {
            SpaceVO spaceVO = SpaceVO.objToVo(space);
            User user = userMap.get(space.getUserId());
            if (user != null) {
                UserVO userVO = new UserVO();
                BeanUtils.copyProperties(user, userVO);
                spaceVO.setUser(userVO);
            }
            return spaceVO;
        }).collect(Collectors.toList());
        Page<SpaceVO> spaceVOPage = new Page<>(spacePage.getCurrent(), spacePage.getSize(), spacePage.getTotal());
        spaceVOPage.setRecords(spaceVOList);
        return spaceVOPage;
    }
}
