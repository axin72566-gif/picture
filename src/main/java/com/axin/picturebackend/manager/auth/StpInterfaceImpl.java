package com.axin.picturebackend.manager.auth;

import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import cn.hutool.json.JSONUtil;
import com.axin.picturebackend.constant.UserConstant;
import com.axin.picturebackend.exception.BusinessException;
import com.axin.picturebackend.exception.ErrorCode;
import com.axin.picturebackend.manager.auth.model.SpaceUserAuthContext;
import com.axin.picturebackend.model.Enum.SpaceRoleEnum;
import com.axin.picturebackend.model.Enum.SpaceTypeEnum;
import com.axin.picturebackend.model.entity.Space;
import com.axin.picturebackend.model.entity.SpaceUser;
import com.axin.picturebackend.model.entity.User;
import com.axin.picturebackend.service.SpaceService;
import com.axin.picturebackend.service.SpaceUserService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 自定义权限加载接口实现类
 */
@Component    // 保证此类被 SpringBoot 扫描，完成 Sa-Token 的自定义权限验证扩展
public class StpInterfaceImpl implements StpInterface {

	@Value("${server.servlet.context-path}")
	private String contextPath;
	@Resource
	private SpaceUserAuthManager spaceUserAuthManager;
	@Resource
	private SpaceUserService spaceUserService;
	@Resource
	private SpaceService spaceService;

	/**
	 * 返回一个账号所拥有的权限码集合
	 */
	@Override
	public List<String> getPermissionList(Object loginId, String loginType) {
		// 如果不是 Space 账号体系，直接返回空权限，不需要sa-token校验
		if (!loginType.equals("space")) {
			return new ArrayList<>();
		}
		// 获取上下文请求参数
		SpaceUserAuthContext authRequest = getAuthContextByRequest();
		// 上下文中所有字段都是空
		if (ObjUtil.isAllEmpty(authRequest.getPictureId(), authRequest.getSpaceId(), authRequest.getSpaceUserId())) {
			// 说明就是查询公共图库，返回管理员权限
			return spaceUserAuthManager.getPermissionsByRole(SpaceRoleEnum.ADMIN.getValue());
		}
		// 获取用户
		User user = (User) StpKit.SPACE.getSessionByLoginId(loginId).get(UserConstant.USER_LOGIN_STATE);
		if (user == null) {
			// 未登录
			throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
		}
		Long userId = user.getId();
		// 如果说有 spaceUserId，说明想操作团队空间的成员
		Long spaceUserId = authRequest.getSpaceUserId();
		if (ObjUtil.isNotEmpty(spaceUserId)) {
			// 返回登录用户在团队空间中的权限
			SpaceUser spaceUser = spaceUserService.getById(spaceUserId);
			if (spaceUser == null) {
				// 找不到这个空间用户
				throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
			}
			// 获取当前登录用户的权限
			QueryWrapper<SpaceUser> queryWrapper = new QueryWrapper<>();
			queryWrapper.eq("userId", userId)
					.eq("spaceId", spaceUser.getSpaceId());
			SpaceUser loginSpaceUser = spaceUserService.getOne(queryWrapper);
			return spaceUserAuthManager.getPermissionsByRole(loginSpaceUser.getSpaceRole());
		}
		// 如果没有spaceUserId，有pictureId，spaceId，就是想操作空间里面的图片
		Long spaceId = authRequest.getSpaceId();
		if (ObjUtil.isNotEmpty(spaceId)) {
			Space space = spaceService.getById(spaceId);
			if (space == null) {
				// 找不到这个空间
				throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
			}
			// 如果是团队空间
			if (space.getSpaceType() == SpaceTypeEnum.TEAM.getValue()) {
				// 获取当前登录用户的权限
				QueryWrapper<SpaceUser> queryWrapper = new QueryWrapper<>();
				queryWrapper.eq("userId", userId)
						.eq("spaceId", spaceId);
				SpaceUser loginSpaceUser = spaceUserService.getOne(queryWrapper);
				return spaceUserAuthManager.getPermissionsByRole(loginSpaceUser.getSpaceRole());
			}
			// 如果是私人空间，判断是不是空间创建者
			if (space.getUserId().equals(userId)) {
				// 返回管理员权限
				return spaceUserAuthManager.getPermissionsByRole(SpaceRoleEnum.ADMIN.getValue());
			}
		}
		// 只有 pictureId，说明想操作公共图库的图片，判断是不是创建者
		Long pictureId = authRequest.getPictureId();
		if (ObjUtil.isNotEmpty(pictureId)) {
			if (pictureId.equals(userId)) {
				// 创建者
				return spaceUserAuthManager.getPermissionsByRole(SpaceRoleEnum.ADMIN.getValue());
			}
			// 不是创建者，那就是浏览者
			return spaceUserAuthManager.getPermissionsByRole(SpaceRoleEnum.VIEWER.getValue());
		}
		// 什么都没有，管理员
		return spaceUserAuthManager.getPermissionsByRole(SpaceRoleEnum.ADMIN.getValue());
	}

	/**
	 * 返回一个账号所拥有的角色标识集合 (权限与角色可分开校验)
	 */
	@Override
	public List<String> getRoleList(Object loginId, String loginType) {
		return new ArrayList<>();
	}

	/**
	 * 从请求中获取上下文对象
	 */
	private SpaceUserAuthContext getAuthContextByRequest() {
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
		String contentType = request.getHeader(Header.CONTENT_TYPE.getValue());
		SpaceUserAuthContext authRequest;
		// 兼容 get 和 post 操作
		if (ContentType.JSON.getValue().equals(contentType)) {
			// post 请求
			String body = ServletUtil.getBody(request);
			authRequest = JSONUtil.toBean(body, SpaceUserAuthContext.class);
		} else {
			// get 请求
			Map<String, String> paramMap = ServletUtil.getParamMap(request);
			authRequest = BeanUtil.toBean(paramMap, SpaceUserAuthContext.class);
		}
		// 根据请求路径区分 id 字段的含义
		Long id = authRequest.getId();
		if (ObjUtil.isNotNull(id)) {
			String requestUri = request.getRequestURI();
			String partUri = requestUri.replace(contextPath + "/", "");
			String moduleName = StrUtil.subBefore(partUri, "/", false);
			switch (moduleName) {
				case "picture":
					authRequest.setPictureId(id);
					break;
				case "spaceUser":
					authRequest.setSpaceUserId(id);
					break;
				case "space":
					authRequest.setSpaceId(id);
					break;
				default:
			}
		}
		return authRequest;
	}

}
