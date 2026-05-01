package com.axin.picturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户关注请求
 */
@Data
public class UserFollowRequest implements Serializable {

    /**
     * 被关注用户ID
     */
    private Long followUserId;

    private static final long serialVersionUID = 1L;
}
