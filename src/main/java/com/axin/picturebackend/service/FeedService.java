package com.axin.picturebackend.service;

import com.axin.picturebackend.model.dto.feed.FeedQueryRequest;
import com.axin.picturebackend.model.entity.User;
import com.axin.picturebackend.model.vo.FeedPageResult;

/**
 * Feed 流服务接口
 */
public interface FeedService {

    /**
     * 获取当前用户的 Feed 流列表（推拉合并、游标分页）
     *
     * <p>合并策略：
     * <ol>
     *   <li>从用户收件箱（feed:inbox:{userId}）读取普通关注者推来的动态</li>
     *   <li>从该用户关注的所有大V发件箱（feed:outbox:{bigVId}）读取大V动态</li>
     *   <li>内存中合并、去重、按时间戳倒序，截取 size 条返回</li>
     * </ol>
     *
     * @param request   游标分页请求（cursor=时间戳游标，size=每页数量）
     * @param loginUser 当前登录用户
     * @return Feed 分页结果（含 nextCursor 用于翻页）
     */
    FeedPageResult getFeedList(FeedQueryRequest request, User loginUser);
}
