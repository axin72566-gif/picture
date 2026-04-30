package com.axin.picturebackend.manager.websocket;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.axin.picturebackend.manager.auth.SpaceUserAuthManager;
import com.axin.picturebackend.manager.auth.model.SpaceUserPermissionConstant;
import com.axin.picturebackend.model.Enum.SpaceTypeEnum;
import com.axin.picturebackend.model.entity.Picture;
import com.axin.picturebackend.model.entity.Space;
import com.axin.picturebackend.model.entity.User;
import com.axin.picturebackend.service.PictureService;
import com.axin.picturebackend.service.SpaceService;
import com.axin.picturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * WebSocket 握手拦截器
 * <p>
 * 握手时依次校验：pictureId 参数 → 用户登录 → 图片存在 → 空间存在且为团队空间 → 编辑权限
 * 校验通过后将 user、userId、pictureId 写入 session 属性供后续处理使用。
 * </p>
 */
@Slf4j
@Component
public class WsHandshakeInterceptor implements HandshakeInterceptor {

    @Resource
    private UserService userService;

    @Resource
    private PictureService pictureService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest)) {
            return true;
        }
        HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();
        String pictureIdStr = servletRequest.getParameter("pictureId");
        if (StrUtil.isBlank(pictureIdStr)) {
            log.error("WebSocket 握手拒绝：缺少 pictureId 参数");
            return false;
        }
        User loginUser = userService.getLoginUser(servletRequest);
        if (ObjUtil.isEmpty(loginUser)) {
            log.error("WebSocket 握手拒绝：用户未登录");
            return false;
        }
        Picture picture = pictureService.getById(pictureIdStr);
        if (picture == null) {
            log.error("WebSocket 握手拒绝：图片不存在，pictureId={}", pictureIdStr);
            return false;
        }
        // 有所属空间时，校验空间合法性
        Long spaceId = picture.getSpaceId();
        Space space = null;
        if (spaceId != null) {
            space = spaceService.getById(spaceId);
            if (space == null) {
                log.error("WebSocket 握手拒绝：空间不存在，spaceId={}", spaceId);
                return false;
            }
        }
        // 校验编辑权限
        List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);
        if (!permissionList.contains(SpaceUserPermissionConstant.PICTURE_EDIT)) {
            log.error("WebSocket 握手拒绝：用户无图片编辑权限，userId={}", loginUser.getId());
            return false;
        }
        attributes.put("user", loginUser);
        attributes.put("userId", loginUser.getId());
        attributes.put("pictureId", Long.valueOf(pictureIdStr));
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // 握手成功后无需额外处理
    }
}
