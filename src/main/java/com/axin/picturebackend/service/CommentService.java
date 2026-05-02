package com.axin.picturebackend.service;

import com.axin.picturebackend.model.dto.comment.CommentAddRequest;
import com.axin.picturebackend.model.dto.comment.CommentQueryRequest;
import com.axin.picturebackend.model.entity.Comment;
import com.axin.picturebackend.model.entity.User;
import com.axin.picturebackend.model.vo.CommentVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 图片评论 Service
 */
public interface CommentService extends IService<Comment> {

    /**
     * 发布评论
     *
     * @param commentAddRequest 发布评论请求
     * @param loginUser         登录用户
     * @return 评论ID
     */
    Long addComment(CommentAddRequest commentAddRequest, User loginUser);

    /**
     * 删除评论（本人或管理员，级联删除子评论）
     *
     * @param commentId 评论ID
     * @param loginUser 登录用户
     */
    void deleteComment(Long commentId, User loginUser);

    /**
     * 查询图片评论树（一级评论分页 + 子孙评论全量）
     *
     * @param commentQueryRequest 查询请求
     * @param loginUser           登录用户（可为 null，公共图片可查看）
     * @return 树形评论列表（仅一级，children 内嵌子评论）
     */
    List<CommentVO> listCommentTree(CommentQueryRequest commentQueryRequest, User loginUser);

    /**
     * 获取图片评论数（Redis 优先，DB fallback）
     *
     * @param pictureId 图片ID
     * @return 评论数
     */
    long getCommentCount(Long pictureId);
}
