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
import com.axin.picturebackend.exception.ThrowUtils;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Sa-Token 自定义权限加载接口实现
 * <p>仅处理 space 账号体系的权限校验，其余体系直接返回空权限</p>
 */
@Component
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
     * 返回指定账号在当前请求下所拥有的权限码集合
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        // 非 space 账号体系，不做权限校验
        if (!"space".equals(loginType)) {
            return Collections.emptyList();
        }
        SpaceUserAuthContext authContext = getAuthContextByRequest();
        // 上下文 id 全为空 → 公共图库，返回管理员权限
        if (ObjUtil.isAllEmpty(authContext.getPictureId(), authContext.getSpaceId(), authContext.getSpaceUserId())) {
            return spaceUserAuthManager.getPermissionsByRole(SpaceRoleEnum.ADMIN.getValue());
        }
        User user = (User) StpKit.SPACE.getSessionByLoginId(loginId).get(UserConstant.USER_LOGIN_STATE);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR, "请先登录");
        Long userId = user.getId();

        // 操作空间成员
        Long spaceUserId = authContext.getSpaceUserId();
        if (ObjUtil.isNotEmpty(spaceUserId)) {
            SpaceUser spaceUser = spaceUserService.getById(spaceUserId);
            ThrowUtils.throwIf(spaceUser == null, ErrorCode.NOT_FOUND_ERROR, "空间成员不存在");
            SpaceUser loginSpaceUser = spaceUserService.getOne(
                    new QueryWrapper<SpaceUser>().eq("userId", userId).eq("spaceId", spaceUser.getSpaceId())
            );
            return spaceUserAuthManager.getPermissionsByRole(loginSpaceUser.getSpaceRole());
        }

        // 操作空间图片
        Long spaceId = authContext.getSpaceId();
        if (ObjUtil.isNotEmpty(spaceId)) {
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            if (space.getSpaceType() == SpaceTypeEnum.TEAM.getValue()) {
                SpaceUser loginSpaceUser = spaceUserService.getOne(
                        new QueryWrapper<SpaceUser>().eq("userId", userId).eq("spaceId", spaceId)
                );
                return spaceUserAuthManager.getPermissionsByRole(loginSpaceUser.getSpaceRole());
            }
            // 私有空间：仅创建者有管理员权限
            if (space.getUserId().equals(userId)) {
                return spaceUserAuthManager.getPermissionsByRole(SpaceRoleEnum.ADMIN.getValue());
            }
        }

        // 操作公共图库图片
        Long pictureId = authContext.getPictureId();
        if (ObjUtil.isNotEmpty(pictureId)) {
            if (pictureId.equals(userId)) {
                return spaceUserAuthManager.getPermissionsByRole(SpaceRoleEnum.ADMIN.getValue());
            }
            return spaceUserAuthManager.getPermissionsByRole(SpaceRoleEnum.VIEWER.getValue());
        }

        return spaceUserAuthManager.getPermissionsByRole(SpaceRoleEnum.ADMIN.getValue());
    }

    /**
     * 返回账号所拥有的角色集合（本项目暂不使用角色校验）
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        return Collections.emptyList();
    }

    /**
     * 从当前请求中解析鉴权上下文对象
     * <p>兼容 GET（参数）和 POST（JSON body）两种请求方式</p>
     */
    private SpaceUserAuthContext getAuthContextByRequest() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String contentType = request.getHeader(Header.CONTENT_TYPE.getValue());
        SpaceUserAuthContext authContext;
        if (ContentType.JSON.getValue().equals(contentType)) {
            authContext = JSONUtil.toBean(ServletUtil.getBody(request), SpaceUserAuthContext.class);
        } else {
            Map<String, String> paramMap = ServletUtil.getParamMap(request);
            authContext = BeanUtil.toBean(paramMap, SpaceUserAuthContext.class);
        }
        // 根据请求路径将通用 id 映射到具体字段
        Long id = authContext.getId();
        if (ObjUtil.isNotNull(id)) {
            String requestUri = request.getRequestURI();
            String partUri = requestUri.replace(contextPath + "/", "");
            String moduleName = StrUtil.subBefore(partUri, "/", false);
            switch (moduleName) {
                case "picture":
                    authContext.setPictureId(id);
                    break;
                case "spaceUser":
                    authContext.setSpaceUserId(id);
                    break;
                case "space":
                    authContext.setSpaceId(id);
                    break;
                default:
                    break;
            }
        }
        return authContext;
    }
}
