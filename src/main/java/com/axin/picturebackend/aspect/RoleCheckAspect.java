package com.axin.picturebackend.aspect;

import com.axin.picturebackend.annotation.RoleCheck;
import com.axin.picturebackend.exception.BusinessException;
import com.axin.picturebackend.exception.ErrorCode;
import com.axin.picturebackend.model.entity.User;
import com.axin.picturebackend.service.UserService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@Aspect
@Component
public class RoleCheckAspect {

	@Resource
	private UserService userService;

	@Around("@annotation(roleCheck)")
	public Object doAround(ProceedingJoinPoint joinPoint, RoleCheck roleCheck) throws Throwable {
		// 需要的角色
		String mustRole = roleCheck.mustRole();
		// 获取当前的角色
		RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
		HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
		User loginUser = userService.getLoginUser(request);

		// 功能无需权限校验
		if (mustRole.isEmpty()) {
			return joinPoint.proceed();
		}

		// 判断是否登录
		if (loginUser == null) {
			throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "切面拦截未登录");
		}
		// 判断权限
		String userRole = loginUser.getUserRole();
		if (!mustRole.equals(userRole)) {
			throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "权限不符合");
		}
		// 符合功能所需权限，放行
		return joinPoint.proceed();
	}
}
