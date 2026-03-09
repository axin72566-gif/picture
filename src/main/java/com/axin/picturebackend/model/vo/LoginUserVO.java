package com.axin.picturebackend.model.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class LoginUserVO implements Serializable {

	private static final long serialVersionUID = -231L;

	private Long id;

	// 账号
	private String userAccount;

	// 昵称
	private String userName;

	// 头像
	private String userAvatar;

	// 简介
	private String userProfile;

	// 角色
	private String userRole;

}
