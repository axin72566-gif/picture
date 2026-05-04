package com.axin.picturebackend.service;

import com.axin.picturebackend.model.dto.notice.NoticeQueryRequest;
import com.axin.picturebackend.model.dto.notice.NoticeReadRequest;
import com.axin.picturebackend.model.entity.SysNotice;
import com.axin.picturebackend.model.entity.User;
import com.axin.picturebackend.model.vo.NoticeVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 用户通知 Service
 */
public interface SysNoticeService extends IService<SysNotice> {

    /**
     * 发送通知给指定用户
     *
     * @param toUserId  接收用户ID（0=全体广播）
     * @param title     通知标题
     * @param content   通知内容
     * @param relatedId 关联业务ID（图片ID/评论ID等，无则传 null）
     */
    void sendNotice(Long toUserId, String title, String content, Long relatedId, String type);

    /**
     * 批量发送通知（同一内容发给多个用户，用于系统批量清理场景）
     *
     * @param toUserIds 接收用户ID列表
     * @param title     通知标题
     * @param content   通知内容
     * @param relatedId 关联业务ID
     */
    void sendBatchNotice(List<Long> toUserIds, String title, String content, Long relatedId, String type);

    /**
     * 分页查询当前用户的通知列表（含广播 userId=0 的通知）
     *
     * @param queryRequest 查询条件
     * @param loginUser    当前登录用户
     * @return 分页 NoticeVO 列表
     */
    Page<NoticeVO> listNotice(NoticeQueryRequest queryRequest, User loginUser);

    /**
     * 标记通知已读（单条或全部）
     *
     * @param readRequest 已读请求
     * @param loginUser   当前登录用户
     */
    void markRead(NoticeReadRequest readRequest, User loginUser);

    /**
     * 删除通知（软删除，本人或管理员可删）
     *
     * @param noticeId  通知ID
     * @param loginUser 当前登录用户
     */
    void deleteNotice(Long noticeId, User loginUser);

    /**
     * 获取当前用户未读通知数量
     *
     * @param loginUser 当前登录用户
     * @return 未读数量
     */
    long countUnread(User loginUser);
}
