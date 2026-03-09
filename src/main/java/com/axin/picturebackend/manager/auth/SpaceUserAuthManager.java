package com.axin.picturebackend.manager.auth;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.axin.picturebackend.exception.BusinessException;
import com.axin.picturebackend.exception.ErrorCode;
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
import java.util.List;

@Component
public class SpaceUserAuthManager {

	@Resource
	private SpaceUserService spaceUserService;

	@Resource
	private UserService userService;

	public static final SpaceUserAuthConfig SPACE_USER_AUTH_CONFIG;

	static {
		String json = ResourceUtil.readUtf8Str("biz/spaceUserAuthConfig.json");
		SPACE_USER_AUTH_CONFIG = JSONUtil.toBean(json, SpaceUserAuthConfig.class);
	}

	/**
	 * 根据角色获取权限列表
	 */
	public List<String> getPermissionsByRole(String spaceUserRole) {
		if (StrUtil.isBlank(spaceUserRole)) {
			return new ArrayList<>();
		}
		// 找到匹配的角色
		SpaceUserRole role = SPACE_USER_AUTH_CONFIG.getRoles().stream()
				.filter(r -> spaceUserRole.equals(r.getKey()))
				.findFirst()
				.orElse(null);
		if (role == null) {
			return new ArrayList<>();
		}
		return role.getPermissions();
	}

	/**
	 * 获取权限列表
	 *
	 * @param space     空间
	 * @param loginUser 登录用户
	 * @return 权限列表
	 */
	public List<String> getPermissionList(Space space, User loginUser) {
		// 登录用户为空
		if (loginUser == null) {
			return new ArrayList<>();
		}
		// 私有空间或者团队空间
		if (space != null) {
			// 如果是团队空间
			if (space.getSpaceType() == SpaceTypeEnum.TEAM.getValue()) {
				Long spaceId = space.getId();
				Long userId = loginUser.getId();
				QueryWrapper<SpaceUser> queryWrapper = new QueryWrapper<>();
				queryWrapper.eq("spaceId", spaceId)
						.eq("userId", userId);
				SpaceUser spaceUser = spaceUserService.getOne(queryWrapper);
				if (spaceUser == null) {
					throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
				}
				return getPermissionsByRole(spaceUser.getSpaceRole());
			}
			// 私有空间，判断是不是管理员
			if (space.getUserId().equals(loginUser.getId())) {
				return getPermissionsByRole(SpaceRoleEnum.ADMIN.getValue());
			} else {
				throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "私有空间无法操作");
			}
		}
		// 公共图库
		if (userService.isAdmin(loginUser)) {
			// 管理员
			return getPermissionsByRole(SpaceRoleEnum.ADMIN.getValue());
		} else {
			// 不是管理员，浏览者
			return getPermissionsByRole(SpaceRoleEnum.VIEWER.getValue());
		}
	}
}
