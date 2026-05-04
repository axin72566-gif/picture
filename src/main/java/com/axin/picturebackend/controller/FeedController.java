package com.axin.picturebackend.controller;

import com.axin.picturebackend.common.BaseResponse;
import com.axin.picturebackend.common.ResultUtils;
import com.axin.picturebackend.exception.ErrorCode;
import com.axin.picturebackend.exception.ThrowUtils;
import com.axin.picturebackend.model.dto.feed.FeedQueryRequest;
import com.axin.picturebackend.model.entity.User;
import com.axin.picturebackend.model.vo.FeedPageResult;
import com.axin.picturebackend.service.FeedService;
import com.axin.picturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * Feed 流控制器
 *
 * <p>提供关注者动态 Feed 流查询接口，需要登录后才能访问。
 * 采用游标分页，适合无限滚动场景。
 */
@Slf4j
@RestController
@RequestMapping("/feed")
public class FeedController {

    @Resource
    private FeedService feedService;

    @Resource
    private UserService userService;

    /**
     * 获取 Feed 流列表（关注者最新上传动态）
     *
     * <p>首次请求：cursor 不传（null），从最新数据开始。
     * <p>后续翻页：cursor 传上次响应的 nextCursor，继续向历史翻页。
     *
     * @param request      游标分页参数（cursor=时间戳游标，size=每页数量，默认20，最大50）
     * @param httpRequest  HTTP 请求（用于获取登录用户）
     * @return FeedPageResult（records + nextCursor + hasMore）
     */
    @GetMapping("/list")
    public BaseResponse<FeedPageResult> getFeedList(FeedQueryRequest request,
                                                    HttpServletRequest httpRequest) {
        // 必须登录
        User loginUser = userService.getLoginUser(httpRequest);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);

        // size 合法性校验（Service 层也会截断，这里提前拦截非法值）
        if (request == null) {
            request = new FeedQueryRequest();
        }
        if (request.getSize() != null && request.getSize() <= 0) {
            request.setSize(20);
        }

        FeedPageResult result = feedService.getFeedList(request, loginUser);
        return ResultUtils.success(result);
    }
}
