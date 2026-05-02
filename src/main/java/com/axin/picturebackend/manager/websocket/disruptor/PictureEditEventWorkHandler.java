package com.axin.picturebackend.manager.websocket.disruptor;

import cn.hutool.json.JSONUtil;
import com.axin.picturebackend.manager.websocket.PictureEditHandler;
import com.axin.picturebackend.manager.websocket.model.PictureEditMessageTypeEnum;
import com.axin.picturebackend.manager.websocket.model.PictureEditRequestMessage;
import com.axin.picturebackend.manager.websocket.model.PictureEditResponseMessage;
import com.axin.picturebackend.model.entity.User;
import com.axin.picturebackend.service.UserService;
import com.lmax.disruptor.EventHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * Disruptor 事件消费处理器（EventHandler 模式，按 pictureId 哈希分区保证同图有序）
 * <p>根据消息类型分发至 {@link PictureEditHandler} 的对应处理方法</p>
 */
@Slf4j
public class PictureEditEventWorkHandler implements EventHandler<PictureEditEvent> {

    private final PictureEditHandler pictureEditHandler;

    private final UserService userService;

    private final int partitionIndex;

    private final int partitionCount;

    public PictureEditEventWorkHandler(PictureEditHandler pictureEditHandler, UserService userService,
                                       int partitionIndex, int partitionCount) {
        this.pictureEditHandler = pictureEditHandler;
        this.userService = userService;
        this.partitionIndex = partitionIndex;
        this.partitionCount = partitionCount;
    }

    @Override
    public void onEvent(PictureEditEvent event, long sequence, boolean endOfBatch) throws Exception {
        // 按 pictureId 哈希分区，只处理属于自己分区的消息，保证同图事件顺序
        Long pictureId = event.getPictureId();
        if (pictureId == null || Math.abs(pictureId.hashCode()) % partitionCount != partitionIndex) {
            return;
        }
        PictureEditRequestMessage requestMessage = event.getPictureEditRequestMessage();
        WebSocketSession session = event.getSession();
        User user = event.getUser();

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
