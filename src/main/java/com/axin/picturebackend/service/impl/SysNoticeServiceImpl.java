package com.axin.picturebackend.service.impl;

import com.axin.picturebackend.constant.UserConstant;
import com.axin.picturebackend.exception.BusinessException;
import com.axin.picturebackend.exception.ErrorCode;
import com.axin.picturebackend.exception.ThrowUtils;
import com.axin.picturebackend.mapper.SysNoticeMapper;
import com.axin.picturebackend.model.dto.notice.NoticeQueryRequest;
import com.axin.picturebackend.model.dto.notice.NoticeReadRequest;
import com.axin.picturebackend.model.entity.SysNotice;
import com.axin.picturebackend.model.entity.User;
import com.axin.picturebackend.model.vo.NoticeVO;
import com.axin.picturebackend.service.SysNoticeService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户通知 Service 实现
 */
@Service
@Slf4j
public class SysNoticeServiceImpl extends ServiceImpl<SysNoticeMapper, SysNotice>
        implements SysNoticeService {

    // ==================== 发送通知 ====================

    @Override
    public void sendNotice(Long toUserId, String title, String content, Long relatedId) {
        if (toUserId == null || !StringUtils.hasText(content)) {
            log.warn("发送通知参数不合法, toUserId={}, content={}", toUserId, content);
            return;
        }
        try {
            SysNotice notice = new SysNotice();
            notice.setUserId(toUserId);
            notice.setTitle(title);
            notice.setContent(content);
            notice.setRelatedId(relatedId);
            notice.setIsRead(0);
            notice.setIsDeleted(0);
            this.save(notice);
        } catch (Exception e) {
            log.warn("发送通知失败, toUserId={}, content={}, error={}", toUserId, content, e.getMessage());
        }
    }

    @Override
    public void sendBatchNotice(List<Long> toUserIds, String title, String content, Long relatedId) {
        if (CollectionUtils.isEmpty(toUserIds) || !StringUtils.hasText(content)) {
            log.warn("批量发送通知参数不合法");
            return;
        }
        try {
            List<SysNotice> notices = toUserIds.stream().map(userId -> {
                SysNotice notice = new SysNotice();
                notice.setUserId(userId);
                notice.setTitle(title);
                notice.setContent(content);
                notice.setRelatedId(relatedId);
                notice.setIsRead(0);
                notice.setIsDeleted(0);
                return notice;
            }).collect(Collectors.toList());
            this.saveBatch(notices, 100);
        } catch (Exception e) {
            log.warn("批量发送通知失败, count={}, error={}", toUserIds.size(), e.getMessage());
        }
    }

    // ==================== 查询通知 ====================

    @Override
    public Page<NoticeVO> listNotice(NoticeQueryRequest queryRequest, User loginUser) {
        ThrowUtils.throwIf(queryRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);

        long current = queryRequest.getCurrent();
        long pageSize = queryRequest.getPageSize();
        Integer isRead = queryRequest.getIsRead();
        Long loginUserId = loginUser.getId();

        // 查询当前用户通知 + 全体广播（userId=0）
        QueryWrapper<SysNotice> queryWrapper = new QueryWrapper<>();
        queryWrapper.and(qw -> qw.eq("userId", loginUserId).or().eq("userId", 0L));
        queryWrapper.eq("isDeleted", 0);
        if (isRead != null) {
            queryWrapper.eq("isRead", isRead);
        }
        queryWrapper.orderByDesc("createTime");

        // 分页查询
        Page<SysNotice> page = this.page(new Page<>(current, pageSize), queryWrapper);

        // 转 VO
        Page<NoticeVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<NoticeVO> voList = page.getRecords().stream()
                .map(NoticeVO::of)
                .collect(Collectors.toList());
        voPage.setRecords(voList);
        return voPage;
    }

    // ==================== 标记已读 ====================

    @Override
    public void markRead(NoticeReadRequest readRequest, User loginUser) {
        ThrowUtils.throwIf(readRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);

        Boolean readAll = readRequest.getReadAll();
        Long noticeId = readRequest.getId();
        Long loginUserId = loginUser.getId();

        if (Boolean.TRUE.equals(readAll)) {
            // 全部标记已读（含广播通知）
            UpdateWrapper<SysNotice> updateWrapper = new UpdateWrapper<>();
            updateWrapper.and(uw -> uw.eq("userId", loginUserId).or().eq("userId", 0L));
            updateWrapper.eq("isRead", 0);
            updateWrapper.eq("isDeleted", 0);
            updateWrapper.set("isRead", 1);
            updateWrapper.set("readTime", new Date());
            this.update(updateWrapper);
        } else {
            // 单条标记已读
            ThrowUtils.throwIf(noticeId == null || noticeId <= 0, ErrorCode.PARAMS_ERROR, "通知ID不合法");
            SysNotice notice = this.getById(noticeId);
            ThrowUtils.throwIf(notice == null || notice.getIsDeleted() == 1, ErrorCode.NOT_FOUND_ERROR, "通知不存在");
            // 校验权限：只能标记自己的通知或广播通知为已读
            boolean isMine = notice.getUserId().equals(loginUserId) || notice.getUserId() == 0L;
            ThrowUtils.throwIf(!isMine, ErrorCode.NO_AUTH_ERROR, "无权操作该通知");
            if (notice.getIsRead() == 1) {
                return;
            }
            UpdateWrapper<SysNotice> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("id", noticeId);
            updateWrapper.set("isRead", 1);
            updateWrapper.set("readTime", new Date());
            this.update(updateWrapper);
        }
    }

    // ==================== 删除通知 ====================

    @Override
    public void deleteNotice(Long noticeId, User loginUser) {
        ThrowUtils.throwIf(noticeId == null || noticeId <= 0, ErrorCode.PARAMS_ERROR, "通知ID不合法");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);

        SysNotice notice = this.getById(noticeId);
        ThrowUtils.throwIf(notice == null || notice.getIsDeleted() == 1, ErrorCode.NOT_FOUND_ERROR, "通知不存在");

        // 权限：本人或管理员可删
        boolean isAdmin = UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole());
        boolean isMine = notice.getUserId().equals(loginUser.getId());
        ThrowUtils.throwIf(!isMine && !isAdmin, ErrorCode.NO_AUTH_ERROR, "无权删除该通知");

        // 软删除
        UpdateWrapper<SysNotice> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", noticeId);
        updateWrapper.set("isDeleted", 1);
        updateWrapper.set("updateTime", new Date());
        boolean success = this.update(updateWrapper);
        ThrowUtils.throwIf(!success, ErrorCode.OPERATION_ERROR, "删除失败");
    }

    // ==================== 未读数量 ====================

    @Override
    public long countUnread(User loginUser) {
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        QueryWrapper<SysNotice> queryWrapper = new QueryWrapper<>();
        queryWrapper.and(qw -> qw.eq("userId", loginUser.getId()).or().eq("userId", 0L));
        queryWrapper.eq("isRead", 0);
        queryWrapper.eq("isDeleted", 0);
        return this.count(queryWrapper);
    }
}
