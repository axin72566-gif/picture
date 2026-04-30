package com.axin.picturebackend.mapper;

import com.axin.picturebackend.model.entity.Comment;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 图片评论 Mapper
 */
@Mapper
public interface CommentMapper extends BaseMapper<Comment> {

}
