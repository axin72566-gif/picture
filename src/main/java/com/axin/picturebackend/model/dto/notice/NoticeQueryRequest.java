package com.axin.picturebackend.model.dto.notice;

import com.axin.picturebackend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 查询通知请求 DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class NoticeQueryRequest extends PageRequest implements Serializable {

    /**
     * 阅读状态过滤：0-未读，1-已读，null-全部
     */
    private Integer isRead;

    private static final long serialVersionUID = 1L;
}
