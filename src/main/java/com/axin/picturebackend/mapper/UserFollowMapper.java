package com.axin.picturebackend.mapper;

import com.axin.picturebackend.model.entity.UserFollow;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
* @author axin
* @description 针对表【user_follow(用户关注关联表)】的数据库操作Mapper
* @createDate 2026-05-01
* @Entity com.axin.picturebackend.model.entity.UserFollow
*/
public interface UserFollowMapper extends BaseMapper<UserFollow> {

    /**
     * 查询指定用户的全量粉丝ID列表（followUserId = userId 的所有 userId）
     *
     * <p>用于推模式：消费者查出粉丝列表，批量写入各粉丝的 Redis 收件箱。
     *
     * @param followUserId 被关注者（上传图片的用户）ID
     * @return 粉丝用户ID列表
     */
    List<Long> selectFansIdList(@Param("followUserId") Long followUserId);
}
