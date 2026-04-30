package com.axin.picturebackend.manager.auth;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.axin.picturebackend.exception.BusinessException;
import com.axin.picturebackend.exception.ErrorCode;
import com.axin.picturebackend.exception.ThrowUtils;
import com.axin.picturebackend.manager.auth.model.SpaceUserAuthConfig;
import com.axin.picturebackend.manager.auth.model.SpaceUserRole;
import com.axin.picturebackend.model.Enum.SpaceRoleEnum;
import com.axin.picturebackend.model.Enum.SpaceTypeEnum;
import com.axin.picturebackend.model.entity.Space;
import com.axin.picturebackend.model.entity.SpaceUser;
import com.axin.picturebackend.model.entity.User;
import com.axin.picturebackend.service.SpaceUserService;
import com.axin.picturebackend.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 空间用户权限管理器
 * <p>负责根据空间类型和用户身份返回对应的权限列表</p>
 */
@Component
public class SpaceUserAuthManager {

    @Resource
    private SpaceUserService spaceUserService;

    @Resource
    private UserService userService;

    /**
     * 空间用户权限配置（静态加载，启动时从 JSON 文件读取一次）
     */
    public static final SpaceUserAuthConfig SPACE_USER_AUTH_CONFIG;

    static {
        String json = ResourceUtil.readUtf8Str("biz/spaceUserAuthConfig.json");
        SPACE_USER_AUTH_CONFIG = JSONUtil.toBean(json, SpaceUserAuthConfig.class);
    }

    /**
     * 根据角色键获取对应的权限列表
     *
     * @param spaceUserRole 角色键
     * @return 权限键列表，角色不存在时返回空列表
     */
    public List<String> getPermissionsByRole(String spaceUserRole) {
        if (StrUtil.isBlank(spaceUserRole)) {
            return Collections.emptyList();
        }
        SpaceUserRole role = SPACE_USER_AUTH_CONFIG.getRoles().stream()
                .filter(r -> spaceUserRole.equals(r.getKey()))
                .findFirst()
                .orElse(null);
        if (role == null) {
            return Collections.emptyList();
        }
        return role.getPermissions();
    }

    /**
     * 根据空间和登录用户获取权限列表
     * <ul>
     *   <li>公共图库：管理员拥有全部权限，普通用户拥有浏览权限</li>
     *   <li>私有空间：仅创建者拥有管理员权限，否则拒绝访问</li>
     *   <li>团队空间：根据用户在空间内的角色获取对应权限</li>
     * </ul>
     *
     * @param space     空间（为 null 表示公共图库）
     * @param loginUser 当前登录用户
     * @return 权限键列表
     */
    public List<String> getPermissionList(Space space, User loginUser) {
        if (loginUser == null) {
            return Collections.emptyList();
        }
        // 私有空间 / 团队空间
        if (space != null) {
            if (space.getSpaceType() == SpaceTypeEnum.TEAM.getValue()) {
                // 团队空间：按成员角色返回权限
                SpaceUser spaceUser = spaceUserService.getOne(
                        new QueryWrapper<SpaceUser>()
                                .eq("spaceId", space.getId())
                                .eq("userId", loginUser.getId())
                );
                ThrowUtils.throwIf(spaceUser == null, ErrorCode.NOT_FOUND_ERROR, "空间用户不存在");
                return getPermissionsByRole(spaceUser.getSpaceRole());
            }
            // 私有空间：仅创建者有权限
            if (space.getUserId().equals(loginUser.getId())) {
                return getPermissionsByRole(SpaceRoleEnum.ADMIN.getValue());
            }
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "私有空间无法操作");
        }
        // 公共图库：管理员全权限，普通用户浏览权限
        String role = userService.isAdmin(loginUser) ? SpaceRoleEnum.ADMIN.getValue() : SpaceRoleEnum.VIEWER.getValue();
        return getPermissionsByRole(role);
    }
}
