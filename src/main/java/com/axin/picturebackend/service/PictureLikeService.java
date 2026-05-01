package com.axin.picturebackend.service;

import com.axin.picturebackend.model.entity.PictureLike;
import com.axin.picturebackend.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Set;

/**
 * 图片点赞 Service 接口
 */
public interface PictureLikeService extends IService<PictureLike> {

    /**
     * 点赞 / 取消点赞
     *
     * @param pictureId 图片ID
     * @param loginUser 当前登录用户
     * @return true=点赞成功, false=取消点赞
     */
    boolean doLike(Long pictureId, User loginUser);

    /**
     * 判断用户是否已点赞某图片
     *
     * @param pictureId 图片ID
     * @param userId    用户ID
     * @return true=已点赞
     */
    boolean isLiked(Long pictureId, Long userId);

    /**
     * 批量判断用户对图片列表的点赞状态
     *
     * @param pictureIds 图片ID列表
     * @param userId     用户ID
     * @return 已点赞的图片ID集合
     */
    Set<Long> batchIsLiked(List<Long> pictureIds, Long userId);

    /**
     * 获取图片点赞数（优先从 Redis 读取）
     *
     * @param pictureId 图片ID
     * @param dbCount   数据库中的点赞数（fallback）
     * @return 点赞数
     */
    long getLikeCount(Long pictureId, Long dbCount);

    /**
     * 获取最近点赞的10位用户ID
     * @param pictureId 图片ID
     * @param limit 限制数量
     * @return 用户ID列表
     */
    List<Long> listTopLikedUserIds(Long pictureId, int limit);
}
