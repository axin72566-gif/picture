package com.axin.picturebackend.model.dto.comment;

import lombok.Data;

import java.io.Serializable;

/**
 * 发布评论请求
 */
@Data
public class CommentAddRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 所属图片ID
     */
    private Long pictureId;

    /**
     * 父评论ID，0 或 null 表示一级评论
     */
    private Long parentId;

    /**
     * 评论内容
     */
    private String content;
}
