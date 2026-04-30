package com.axin.picturebackend.manager.websocket.disruptor;

import cn.hutool.json.JSONUtil;
import com.axin.picturebackend.manager.websocket.PictureEditHandler;
import com.axin.picturebackend.manager.websocket.model.PictureEditMessageTypeEnum;
import com.axin.picturebackend.manager.websocket.model.PictureEditRequestMessage;
import com.axin.picturebackend.manager.websocket.model.PictureEditResponseMessage;
import com.axin.picturebackend.model.entity.User;
import com.axin.picturebackend.service.UserService;
import com.lmax.disruptor.WorkHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.Resource;

/**
 * Disruptor 事件消费处理器
 * <p>根据消息类型分发至 {@link PictureEditHandler} 的对应处理方法</p>
 */
@Slf4j
@Component
public class PictureEditEventWorkHandler implements WorkHandler<PictureEditEvent> {

    @Lazy
    @Resource
    private PictureEditHandler pictureEditHandler;

    @Resource
    private UserService userService;

    @Override
    public void onEvent(PictureEditEvent event) throws Exception {
        PictureEditRequestMessage requestMessage = event.getPictureEditRequestMessage();
        WebSocketSession session = event.getSession();
        User user = event.getUser();
        Long pictureId = event.getPictureId();

        String type = requestMessage.getType();
        PictureEditMessageTypeEnum typeEnum = PictureEditMessageTypeEnum.getEnumByValue(type);
        if (typeEnum == null) {
            sendErrorMessage(session, user, "消息类型错误");
            return;
        }
        switch (typeEnum) {
            case ENTER_EDIT:
                pictureEditHandler.handleEnterEditMessage(requestMessage, session, user, pictureId);
                break;
            case EDIT_ACTION:
                pictureEditHandler.handleEditActionMessage(requestMessage, session, user, pictureId);
                break;
            case EXIT_EDIT:
                pictureEditHandler.handleExitEditMessage(requestMessage, session, user, pictureId);
                break;
            default:
                sendErrorMessage(session, user, "消息类型错误");
                break;
        }
    }

    private void sendErrorMessage(WebSocketSession session, User user, String message) throws Exception {
        PictureEditResponseMessage errorResponse = new PictureEditResponseMessage();
        errorResponse.setType(PictureEditMessageTypeEnum.ERROR.getValue());
        errorResponse.setMessage(message);
        errorResponse.setUser(userService.getUserVO(user));
        session.sendMessage(new TextMessage(JSONUtil.toJsonStr(errorResponse)));
    }
}
