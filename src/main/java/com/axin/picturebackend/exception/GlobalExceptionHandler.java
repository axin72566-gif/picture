package com.axin.picturebackend.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import com.axin.picturebackend.common.BaseResponse;
import com.axin.picturebackend.common.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

	/**
	 * 业务异常
	 *
	 * @param e 业务异常
	 * @return BaseResponse
	 */
	@ExceptionHandler(BusinessException.class)
	public BaseResponse<?> businessExceptionHandler(BusinessException e) {
		log.error("BusinessException", e);
		return ResultUtils.error(e.getCode(), e.getMessage());
	}

	/**
	 * 运行时异常
	 *
	 * @param e 运行时异常
	 * @return BaseResponse
	 */
	@ExceptionHandler(RuntimeException.class)
	public BaseResponse<?> runtimeExceptionHandler(RuntimeException e) {
		log.error("RuntimeException", e);
		return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "系统错误");
	}

	/**
	 * 登录异常
	 *
	 * @param e 登录异常
	 * @return BaseResponse
	 */
	@ExceptionHandler(NotLoginException.class)
	public BaseResponse<?> notLoginException(NotLoginException e) {
		log.error("NotLoginException", e);
		return ResultUtils.error(ErrorCode.NOT_LOGIN_ERROR, e.getMessage());
	}

	/**
	 * 权限异常
	 *
	 * @param e 权限异常
	 * @return BaseResponse
	 */
	@ExceptionHandler(NotPermissionException.class)
	public BaseResponse<?> notPermissionExceptionHandler(NotPermissionException e) {
		log.error("NotPermissionException", e);
		return ResultUtils.error(ErrorCode.NO_AUTH_ERROR, e.getMessage());
	}

}


