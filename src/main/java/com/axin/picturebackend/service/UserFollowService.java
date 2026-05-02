package com.axin.picturebackend.service;

import com.axin.picturebackend.model.entity.User;
import com.axin.picturebackend.model.entity.UserFollow;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 用户关注 Service 接口
 */
public interface UserFollowService extends IService<UserFollow> {

    /**
     * 关注 / 取消关注
     *
     * @param followUserId 被关注用户ID
     * @param loginUser    当前登录用户
     * @return true=关注成功, false=取消关注
     */
    boolean doFollow(Long followUserId, User loginUser);

    /**
     * 判断用户是否已关注某人
     *
     * @param userId       用户ID
     * @param followUserId 被关注用户ID
     * @return true=已关注
     */
    boolean isFollowed(Long userId, Long followUserId);

    /**
     * 获取用户关注列表
     *
     * @param userId 用户ID
     * @return 关注的用户列表
     */
    List<User> listFollowing(Long userId);

    /**
     * 统计用户的粉丝数量
     *
     * @param userId 用户ID
     * @return 粉丝数
     */
    long countFollowers(Long userId);
}
