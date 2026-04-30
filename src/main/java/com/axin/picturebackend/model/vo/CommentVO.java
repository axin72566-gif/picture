package com.axin.picturebackend.model.vo;

import com.axin.picturebackend.model.entity.Comment;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 评论响应 VO
 */
@Data
public class CommentVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 评论ID
     */
    private Long id;

    /**
     * 所属图片ID
     */
    private Long pictureId;

    /**
     * 父评论ID，0 表示一级评论
     */
    private Long parentId;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 评论者信息（脱敏）
     */
    private UserVO user;

    /**
     * 子评论列表（树形结构，内存组装）
     */
    private List<CommentVO> children = new ArrayList<>();

    /**
     * Comment 实体 -> CommentVO（不含 user 和 children，需调用方填充）
     */
    public static CommentVO objToVo(Comment comment) {
        if (comment == null) {
            return null;
        }
        CommentVO vo = new CommentVO();
        vo.setId(comment.getId());
        vo.setPictureId(comment.getPictureId());
        vo.setParentId(comment.getParentId());
        vo.setContent(comment.getContent());
        vo.setCreateTime(comment.getCreateTime());
        return vo;
    }
}
