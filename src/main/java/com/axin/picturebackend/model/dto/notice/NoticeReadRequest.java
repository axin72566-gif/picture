package com.axin.picturebackend.model.dto.notice;

import lombok.Data;

import java.io.Serializable;

/**
 * 标记已读请求 DTO
 */
@Data
public class NoticeReadRequest implements Serializable {

    /**
     * 通知ID（单条已读时填写，readAll=true 时可不填）
     */
    private Long id;

    /**
     * 是否全部标记已读
     */
    private Boolean readAll = false;

    private static final long serialVersionUID = 1L;
}
