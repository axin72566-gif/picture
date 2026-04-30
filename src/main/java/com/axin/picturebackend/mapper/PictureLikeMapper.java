package com.axin.picturebackend.mapper;

import com.axin.picturebackend.model.entity.PictureLike;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 图片点赞关联 Mapper
 */
@Mapper
public interface PictureLikeMapper extends BaseMapper<PictureLike> {

    /**
     * 批量查询当前用户对图片列表的点赞状态
     *
     * @param userId     用户ID
     * @param pictureIds 图片ID列表
     * @return 已点赞的图片ID列表
     */
    List<Long> selectLikedPictureIds(@Param("userId") Long userId,
                                     @Param("pictureIds") List<Long> pictureIds);
}
