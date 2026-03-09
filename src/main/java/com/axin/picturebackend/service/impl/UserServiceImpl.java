package com.axin.picturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.axin.picturebackend.common.DeleteRequest;
import com.axin.picturebackend.exception.BusinessException;
import com.axin.picturebackend.exception.ErrorCode;
import com.axin.picturebackend.manager.auth.StpKit;
import com.axin.picturebackend.model.Enum.UserRoleEnum;
import com.axin.picturebackend.model.dto.user.*;
import com.axin.picturebackend.model.vo.LoginUserVO;
import com.axin.picturebackend.model.vo.UserVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.axin.picturebackend.model.entity.User;
import com.axin.picturebackend.service.UserService;
import com.axin.picturebackend.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.servlet.http.HttpServletRequest;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static com.axin.picturebackend.constant.UserConstant.USER_LOGIN_STATE;

/**
* @author kdkt1
* @description 针对表【user(用户)】的数据库操作Service实现
* @createDate 2026-01-19 20:21:40
*/
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{

	/**
	 * 用户注册
	 * @param userRegisterRequest 用户注册请求
	 * @return 用户id
	 */
	@Override
	public Long userRegister(UserRegisterRequest userRegisterRequest) {
		if (userRegisterRequest == null) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR, "注册参数为空");
		}
		String userAccount = userRegisterRequest.getUserAccount();
		String password = userRegisterRequest.getUserPassword();
		String checkPassword = userRegisterRequest.getCheckPassword();
		if (userAccount == null || userAccount.length() < 4) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
		}
		if (password == null || password.length() < 8) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
		}
		if (checkPassword == null || checkPassword.length() < 8) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
		}
		if (!password.equals(checkPassword)) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
		}
		// 检查用户是否重复
		QueryWrapper<User> queryWrapper = new QueryWrapper<>();
		queryWrapper.eq("userAccount", userAccount);
		long count = this.count(queryWrapper);
		if (count > 0) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户已存在");
		}
		// 加密
		String encryptPassword = getEncryptPassword(password);
		User user = new User();
		user.setUserAccount(userAccount);
		user.setUserPassword(encryptPassword);
		user.setUserName("佚名");
		user.setUserRole(UserRoleEnum.USER.getValue());
		boolean save = this.save(user);
		if (!save) {
			throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败");
		}
		return user.getId();
	}

	/**
	 * 用户登录
	 * @param userLoginRequest 用户登录请求
	 * @return 登录用户
	 */
	@Override
	public LoginUserVO userLogin(UserLoginRequest userLoginRequest, HttpServletRequest request) {
		if (userLoginRequest == null) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR, "登录参数为空");
		}
		String userAccount = userLoginRequest.getUserAccount();
		String userPassword = userLoginRequest.getUserPassword();
		String encryptPassword = getEncryptPassword(userPassword);
		if (userAccount == null || userAccount.length() < 4) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
		}
		if (userPassword == null || userPassword.length() < 8) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
		}
		QueryWrapper<User> queryWrapper = new QueryWrapper<>();
		queryWrapper.eq("userAccount", userAccount);
		queryWrapper.eq("userPassword", encryptPassword);
		User user = this.getOne(queryWrapper);
		if (user == null) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
		}
		// 存储登录态
		request.getSession().setAttribute(USER_LOGIN_STATE, user);
		StpKit.SPACE.login(user.getId());
		StpKit.SPACE.getSession().set(USER_LOGIN_STATE, user);
		return getLoginUserVO(user);
	}

	/**
	 * 获取登录用户 脱敏
	 * @param user  用户
	 * @return 登录用户
	 */
	@Override
	public LoginUserVO getLoginUserVO(User user) {
		LoginUserVO loginUserVO = new LoginUserVO();
		BeanUtil.copyProperties(user, loginUserVO);
		return loginUserVO;
	}

	/**
	 * 获取当前登录用户
	 * @param request 请求
	 * @return 当前登录用户
	 */
	@Override
	public User getLoginUser(HttpServletRequest request) {
		return (User) request.getSession().getAttribute(USER_LOGIN_STATE);
	}

	/**
	 * 用户登出
	 * @param request 请求
	 * @return 登出成功
	 */
	@Override
	public boolean userLogout(HttpServletRequest request) {
		if (request.getSession().getAttribute(USER_LOGIN_STATE) == null) {
			throw new BusinessException(ErrorCode.OPERATION_ERROR, "未登录");
		}
		request.getSession().removeAttribute(USER_LOGIN_STATE);
		return true;
	}

	/**
	 * 获取用户信息（脱敏）
	 * @param user  用户
	 * @return 用户信息
	 */
	@Override
	public UserVO getUserVO(User user) {
		if (user == null) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户脱敏传入用户不存在");
		}
		UserVO userVO = new UserVO();
		BeanUtil.copyProperties(user, userVO);
		return userVO;
	}

	/**
	 * 获取用户列表（脱敏）
	 * @param userList 用户列表
	 * @return 用户列表
	 */
	@Override
	public List<UserVO> listUserVO(List<User> userList) {
		if (userList == null) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户列表脱敏，传入用户列表为空");
		}
		return userList.stream().map(this::getUserVO).collect(Collectors.toList());
	}

	/**
	 * 获取查询条件
	 * @param userQueryRequest 查询条件
	 * @return 查询条件
	 */
	@Override
	public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
		if (userQueryRequest == null) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR, "查询条件为空");
		}
		Long id = userQueryRequest.getId();
		String userName = userQueryRequest.getUserName();
		String userAccount = userQueryRequest.getUserAccount();
		String userProfile = userQueryRequest.getUserProfile();
		String userRole = userQueryRequest.getUserRole();
		String sortField = userQueryRequest.getSortField();
		String sortOrder = userQueryRequest.getSortOrder();
		QueryWrapper<User> queryWrapper = new QueryWrapper<>();
		queryWrapper.eq(id != null, "id", id);
		queryWrapper.like(StringUtils.isNotBlank(userName), "userName", userName);
		queryWrapper.like(StringUtils.isNotBlank(userAccount), "userAccount", userAccount);
		queryWrapper.like(StringUtils.isNotBlank(userProfile), "userProfile", userProfile);
		queryWrapper.eq(StringUtils.isNotBlank(userRole), "userRole", userRole);
		queryWrapper.orderBy(StringUtils.isNotBlank(sortField), sortOrder.equals("ascend"), sortField);
		return queryWrapper;
	}

	/**
	 * 添加用户
	 * @param userAddRequest 添加用户请求
	 * @return 添加用户结果
	 */
	@Override
	public Boolean addUser(UserAddRequest userAddRequest) {
		if (userAddRequest == null) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR, "添加用户参数为空");
		}
		String userName = userAddRequest.getUserName();
		String userAccount = userAddRequest.getUserAccount();
		String userRole = userAddRequest.getUserRole();
		if (userName == null) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户昵称为空");
		}
		if (userAccount == null || userAccount.length() < 4) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
		}
		if (userRole == null || UserRoleEnum.getEnumByValue(userRole) == null) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户角色错误");
		}
		User user = new User();
		BeanUtil.copyProperties(userAddRequest, user);
		String encryptPassword = getEncryptPassword("12345678");
		user.setUserPassword(encryptPassword);
		user.setEditTime(new Date());
		user.setCreateTime(new Date());
		user.setUpdateTime(new Date());
		boolean save = this.save(user);
		if (!save) {
			throw new BusinessException(ErrorCode.OPERATION_ERROR, "添加用户失败");
		}
		return true;
	}

	/**
	 * 获取用户
	 * @param id  id
	 * @return  用户
	 */
	@Override
	public User getUserById(Long id) {
		if (id == null) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户id为空");
		}
		User user = this.getById(id);
		if (user == null) {
			throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
		}
		return user;
	}

	/**
	 * 获取用户信息（脱敏）
	 * @param id  id
	 * @return 用户信息
	 */
	@Override
	public UserVO getUserVOById(Long id) {
		User user = getUserById(id);
		return getUserVO(user);
	}

	/**
	 * 删除用户
	 * @param deleteRequest 删除条件
	 * @return 删除结果
	 */
	@Override
	public Boolean deleteUser(DeleteRequest deleteRequest) {
		if (deleteRequest == null) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR);
		}
		// 判断有没有
		Long id = deleteRequest.getId();
		User user = this.getById(id);
		if (user == null) {
			throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
		}
		return this.removeById(deleteRequest.getId());
	}

	/**
	 * 修改用户
	 * @param  userUpdateRequest 修改用户请求
	 * @return 修改用户结果
	 */
	@Override
	public Boolean updateUser(UserUpdateRequest userUpdateRequest) {
		if (userUpdateRequest == null || userUpdateRequest.getId() <= 0) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR);
		}
		User user = new User();
		BeanUtil.copyProperties(userUpdateRequest, user);
		boolean update = this.updateById(user);
		if (!update) {
			throw new BusinessException(ErrorCode.OPERATION_ERROR, "修改用户失败");
		}
		return true;
	}

	/**
	 * 获取用户列表（分页）
	 * @param userQueryRequest 查询条件
	 * @return 用户列表
	 */
	@Override
	public Page<UserVO> listPageUserVO(UserQueryRequest userQueryRequest) {
		// 参数校验
		if (userQueryRequest == null) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR, "查询条件为空");
		}
		// 构造查询条件
		QueryWrapper<User> queryWrapper = getQueryWrapper(userQueryRequest);
		// 分页查询
		long current = userQueryRequest.getCurrent();
		long pageSize = userQueryRequest.getPageSize();
		Page<User> userPage = this.page(new Page<>(current, pageSize), queryWrapper);
		if (userPage == null) {
			throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户分页列表为空");
		}
		// 转换成 VO
		List<User> userList = userPage.getRecords();
		List<UserVO> userVOS = this.listUserVO(userList);
		Page<UserVO> userVOPage = new Page<>(current, pageSize, userPage.getTotal());
		userVOPage.setRecords(userVOS);
		return userVOPage;
	}

	/**
	 * 判断是否是管理员
	 * @param user   用户
	 * @return 是否是管理员
	 */
	@Override
	public boolean isAdmin(User user) {
		if (user == null) {
			return false;
		}
		return UserRoleEnum.ADMIN.getValue().equals(user.getUserRole());
	}

	/**
	 * 获取加密密码
	 * @param userPassword 密码
	 * @return 加密密码
	 */
	@Override
	public String getEncryptPassword(String userPassword) {
		// 盐值，混淆密码
		final String SALT = "axin";
		return DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
	}
}