package com.axin.picturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserLoginRequest implements Serializable {

	private static final long serialVersionUID = 3191241716373120793L;

	// 用户名
	private String userAccount;

	// 密码
	private String userPassword;
}
