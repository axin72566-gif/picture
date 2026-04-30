package com.axin.picturebackend.controller;

import com.axin.picturebackend.common.BaseResponse;
import com.axin.picturebackend.common.ResultUtils;
import com.axin.picturebackend.exception.ErrorCode;
import com.axin.picturebackend.exception.ThrowUtils;
import com.axin.picturebackend.model.dto.notice.NoticeQueryRequest;
import com.axin.picturebackend.model.dto.notice.NoticeReadRequest;
import com.axin.picturebackend.model.entity.User;
import com.axin.picturebackend.model.vo.NoticeVO;
import com.axin.picturebackend.service.SysNoticeService;
import com.axin.picturebackend.service.UserService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 消息通知控制器
 */
@RestController
@RequestMapping("/notice")
public class SysNoticeController {

    @Resource
    private SysNoticeService sysNoticeService;

    @Resource
    private UserService userService;

    /**
     * 分页查询通知列表（登录用户只能查自己的通知及广播通知）
     */
    @PostMapping("/list")
    public BaseResponse<Page<NoticeVO>> listNotice(@RequestBody NoticeQueryRequest queryRequest,
                                                   HttpServletRequest request) {
        ThrowUtils.throwIf(queryRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(sysNoticeService.listNotice(queryRequest, loginUser));
    }

    /**
     * 标记通知已读（单条或全部）
     */
    @PostMapping("/read")
    public BaseResponse<Boolean> markRead(@RequestBody NoticeReadRequest readRequest,
                                          HttpServletRequest request) {
        ThrowUtils.throwIf(readRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        sysNoticeService.markRead(readRequest, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 一键全部标记已读
     */
    @PostMapping("/read/all")
    public BaseResponse<Boolean> markAllRead(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        NoticeReadRequest readRequest = new NoticeReadRequest();
        readRequest.setReadAll(true);
        sysNoticeService.markRead(readRequest, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 删除通知（软删除，本人或管理员）
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteNotice(@RequestParam Long noticeId,
                                              HttpServletRequest request) {
        ThrowUtils.throwIf(noticeId == null || noticeId <= 0, ErrorCode.PARAMS_ERROR, "通知ID不合法");
        User loginUser = userService.getLoginUser(request);
        sysNoticeService.deleteNotice(noticeId, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 获取当前用户未读通知数量
     */
    @GetMapping("/unread/count")
    public BaseResponse<Long> countUnread(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(sysNoticeService.countUnread(loginUser));
    }
}
