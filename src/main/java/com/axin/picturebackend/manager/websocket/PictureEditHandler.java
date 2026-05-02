package com.axin.picturebackend.manager.websocket;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.axin.picturebackend.manager.websocket.disruptor.PictureEditEventProducer;
import com.axin.picturebackend.manager.websocket.model.PictureEditActionEnum;
import com.axin.picturebackend.manager.websocket.model.PictureEditMessageTypeEnum;
import com.axin.picturebackend.manager.websocket.model.PictureEditRequestMessage;
import com.axin.picturebackend.manager.websocket.model.PictureEditResponseMessage;
import com.axin.picturebackend.model.entity.User;
import com.axin.picturebackend.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.annotation.Resource;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

/**
 * 图片协同编辑 WebSocket 处理器
 * <p>
 * 基于 Disruptor 异步处理消息，支持进入编辑、执行编辑动作、退出编辑三种消息类型。
 * Long 类型字段序列化为 String 以避免前端 JS 精度丢失。
 * </p>
 */
@Component
public class PictureEditHandler extends TextWebSocketHandler {

    /** ObjectMapper 单例，配置 Long 序列化为 String */
    private static final ObjectMapper OBJECT_MAPPER;

    static {
        OBJECT_MAPPER = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(Long.class, ToStringSerializer.instance);
        module.addSerializer(Long.TYPE, ToStringSerializer.instance);
        OBJECT_MAPPER.registerModule(module);
    }

    @Resource
    private UserService userService;

    @Resource
    private PictureEditEventProducer pictureEditEventProducer;

    @Resource(name = "backgroundExecutor")
    private Executor backgroundExecutor;

    /** key: pictureId, value: 当前正在编辑的用户 ID */
    private final Map<Long, Long> pictureEditingUsers = new ConcurrentHashMap<>();

    /** key: pictureId, value: 该图片的所有 WebSocket 会话 */
    private final Map<Long, Set<WebSocketSession>> pictureSessions = new ConcurrentHashMap<>();

    // ==================== 生命周期 ====================

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        User user = getAttribute(session, "user");
        Long pictureId = getAttribute(session, "pictureId");
        pictureSessions.computeIfAbsent(pictureId, k -> ConcurrentHashMap.newKeySet()).add(session);

        // 通知加入者当前编辑状态
        Long editingUserId = pictureEditingUsers.get(pictureId);
        if (editingUserId != null) {
            User editingUser = userService.getById(editingUserId);
            PictureEditResponseMessage response = buildInfoResponse(
                    PictureEditMessageTypeEnum.ENTER_EDIT, String.format("当前正在编辑的用户：%s", editingUser.getUserName()), null, editingUser);
            session.sendMessage(new TextMessage(OBJECT_MAPPER.writeValueAsString(response)));
        }

        PictureEditResponseMessage response = buildInfoResponse(
                PictureEditMessageTypeEnum.INFO, String.format("%s加入协同编辑", user.getUserName()), null, user);
        broadcastToPicture(pictureId, response, null);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        PictureEditRequestMessage requestMessage = JSONUtil.toBean(message.getPayload(), PictureEditRequestMessage.class);
        User user = getAttribute(session, "user");
        Long pictureId = getAttribute(session, "pictureId");
        pictureEditEventProducer.publishEvent(requestMessage, session, user, pictureId);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        User user = getAttribute(session, "user");
        Long pictureId = getAttribute(session, "pictureId");
        // 自动退出编辑
        handleExitEditMessage(null, session, user, pictureId);
        // 移除会话
        Set<WebSocketSession> sessions = pictureSessions.get(pictureId);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                pictureSessions.remove(pictureId);
            }
        }
        PictureEditResponseMessage response = buildInfoResponse(
                PictureEditMessageTypeEnum.INFO, String.format("%s离开编辑", user.getUserName()), null, user);
        broadcastToPicture(pictureId, response, null);
    }

    // ==================== 业务消息处理（供 Disruptor WorkHandler 调用）====================

    /**
     * 处理进入编辑消息：同一时刻只允许一个用户编辑同一张图片
     */
    public void handleEnterEditMessage(PictureEditRequestMessage requestMessage, WebSocketSession session,
                                       User user, Long pictureId) throws Exception {
        if (pictureEditingUsers.containsKey(pictureId)) {
            // 正在编辑的用户 ID
            Long editingUserId = pictureEditingUsers.get(pictureId);
            User editingUser = userService.getById(editingUserId);
            String message = String.format("当前已有用户正在编辑：%s", editingUser.getUserName());
            PictureEditResponseMessage response = buildInfoResponse(PictureEditMessageTypeEnum.ERROR, message, null, editingUser);
            session.sendMessage(new TextMessage(OBJECT_MAPPER.writeValueAsString(response)));
            return;
        }
        pictureEditingUsers.put(pictureId, user.getId());
        PictureEditResponseMessage response = buildInfoResponse(
                PictureEditMessageTypeEnum.ENTER_EDIT, String.format("%s开始编辑图片", user.getUserName()), null, user);
        broadcastToPicture(pictureId, response, null);
    }

    /**
     * 处理编辑动作消息：仅当前编辑者可执行，广播给其他用户
     */
    public void handleEditActionMessage(PictureEditRequestMessage requestMessage, WebSocketSession session,
                                        User user, Long pictureId) throws Exception {
        Long editingUserId = pictureEditingUsers.get(pictureId);
        if (editingUserId == null || !editingUserId.equals(user.getId())) {
            return;
        }
        PictureEditActionEnum actionEnum = PictureEditActionEnum.getEnumByValue(requestMessage.getEditAction());
        if (actionEnum == null) {
            return;
        }
        PictureEditResponseMessage response = buildInfoResponse(
                PictureEditMessageTypeEnum.EDIT_ACTION,
                String.format("%s执行%s", user.getUserName(), actionEnum.getText()),
                requestMessage.getEditAction(), user);
        // 排除自己，避免重复编辑
        broadcastToPicture(pictureId, response, session);
    }

    /**
     * 处理退出编辑消息：移除编辑状态，广播通知其他用户
     */
    public void handleExitEditMessage(PictureEditRequestMessage requestMessage, WebSocketSession session,
                                      User user, Long pictureId) throws Exception {
        Long editingUserId = pictureEditingUsers.get(pictureId);
        if (editingUserId == null || !editingUserId.equals(user.getId())) {
            return;
        }
        pictureEditingUsers.remove(pictureId);
        PictureEditResponseMessage response = buildInfoResponse(
                PictureEditMessageTypeEnum.EXIT_EDIT, String.format("%s退出编辑图片", user.getUserName()), null, user);
        broadcastToPicture(pictureId, response, null);
    }

    // ==================== 私有工具方法 ====================

    /**
     * 广播消息给指定图片的所有会话（并行发送），可选排除某个 session
     */
    private void broadcastToPicture(Long pictureId, PictureEditResponseMessage responseMessage,
                                    WebSocketSession excludeSession) {
        Set<WebSocketSession> sessionSet = pictureSessions.get(pictureId);
        if (CollUtil.isEmpty(sessionSet)) {
            return;
        }
        String json;
        try {
            json = OBJECT_MAPPER.writeValueAsString(responseMessage);
        } catch (Exception e) {
            return;
        }
        TextMessage textMessage = new TextMessage(json);
        for (WebSocketSession session : sessionSet) {
            if (excludeSession != null && excludeSession.equals(session)) {
                continue;
            }
            if (session.isOpen()) {
                backgroundExecutor.execute(() -> {
                    try {
                        session.sendMessage(textMessage);
                    } catch (Exception ignored) {
                    }
                });
            }
        }
    }

    /**
     * 定时清理已关闭的 WebSocket 会话，防止异常断开时残留
     */
    @Scheduled(fixedRate = 60000)
    public void cleanDeadSessions() {
        for (Map.Entry<Long, Set<WebSocketSession>> entry : pictureSessions.entrySet()) {
            entry.getValue().removeIf(session -> !session.isOpen());
            if (entry.getValue().isEmpty()) {
                pictureSessions.remove(entry.getKey());
            }
        }
    }

    /**
     * 构建通用响应消息
     */
    private PictureEditResponseMessage buildInfoResponse(PictureEditMessageTypeEnum type, String message,
                                                         String editAction, User user) {
        PictureEditResponseMessage response = new PictureEditResponseMessage();
        response.setType(type.getValue());
        response.setMessage(message);
        response.setEditAction(editAction);
        response.setUser(userService.getUserVO(user));
        return response;
    }

    /**
     * 从 session 属性中获取指定类型的值
     */
    @SuppressWarnings("unchecked")
    private <T> T getAttribute(WebSocketSession session, String key) {
        return (T) session.getAttributes().get(key);
    }
}
