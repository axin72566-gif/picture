package com.axin.picturebackend.service.impl;

import com.axin.picturebackend.exception.BusinessException;
import com.axin.picturebackend.exception.ErrorCode;
import com.axin.picturebackend.mapper.UserFollowMapper;
import com.axin.picturebackend.model.entity.User;
import com.axin.picturebackend.model.entity.UserFollow;
import com.axin.picturebackend.service.SysNoticeService;
import com.axin.picturebackend.service.UserFollowService;
import com.axin.picturebackend.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户关注 Service 实现
 */
@Service
@Slf4j
public class UserFollowServiceImpl extends ServiceImpl<UserFollowMapper, UserFollow>
        implements UserFollowService {

    @Lazy
    @Resource
    private UserService userService;

    @Lazy
    @Resource
    private SysNoticeService sysNoticeService;

    @Override
    public boolean doFollow(Long followUserId, User loginUser) {
        if (followUserId == null || followUserId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "被关注用户ID不合法");
        }
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "未登录");
        }
        Long userId = loginUser.getId();

        // 不能关注自己
        if (userId.equals(followUserId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能关注自己");
        }

        // 检查被关注用户是否存在
        User followUser = userService.getById(followUserId);
        if (followUser == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "被关注用户不存在");
        }

        // 查询关注关系是否存在
        QueryWrapper<UserFollow> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        queryWrapper.eq("followUserId", followUserId);
        UserFollow existFollow = this.getOne(queryWrapper);

        if (existFollow != null) {
            // 已关注 → 取消关注
            boolean removed = this.remove(queryWrapper);
            if (!removed) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "取消关注失败");
            }
            return false;
        } else {
            // 未关注 → 关注
            UserFollow userFollow = new UserFollow();
            userFollow.setUserId(userId);
            userFollow.setFollowUserId(followUserId);
            userFollow.setCreateTime(new Date());
            boolean saved = this.save(userFollow);
            if (!saved) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "关注失败");
            }

            // 发送关注通知
            try {
                String noticeContent = String.format("用户「%s」关注了你",
                        loginUser.getUserName() == null ? loginUser.getUserAccount() : loginUser.getUserName());
                sysNoticeService.sendNotice(followUserId, "收到新关注", noticeContent, userId);
            } catch (Exception e) {
                log.warn("发送关注通知失败, followUserId={}, error={}", followUserId, e.getMessage());
            }
            return true;
        }
    }

    @Override
    public boolean isFollowed(Long userId, Long followUserId) {
        if (userId == null || followUserId == null) {
            return false;
        }
        QueryWrapper<UserFollow> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        queryWrapper.eq("followUserId", followUserId);
        return this.count(queryWrapper) > 0;
    }

    @Override
    public List<User> listFollowing(Long userId) {
        if (userId == null || userId <= 0) {
            return Collections.emptyList();
        }
        QueryWrapper<UserFollow> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        List<UserFollow> followList = this.list(queryWrapper);
        if (followList.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> followUserIds = followList.stream()
                .map(UserFollow::getFollowUserId)
                .collect(Collectors.toList());
        return userService.listByIds(followUserIds);
    }
}
