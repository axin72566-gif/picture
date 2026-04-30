package com.axin.picturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * 图片点赞请求
 */
@Data
public class PictureLikeRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 图片id
     */
    private Long pictureId;
}
