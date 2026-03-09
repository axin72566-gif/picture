package com.axin.picturebackend.service;

import com.axin.picturebackend.common.DeleteRequest;
import com.axin.picturebackend.model.dto.user.*;
import com.axin.picturebackend.model.entity.User;
import com.axin.picturebackend.model.vo.LoginUserVO;
import com.axin.picturebackend.model.vo.UserVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author kdkt1
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2026-01-19 20:21:40
*/
public interface UserService extends IService<User> {

	/**
	 * 用户注册
	 * @param userRegisterRequest 用户注册请求
	 * @return 用户id
	 */
	Long userRegister(UserRegisterRequest userRegisterRequest);

	/**
	 * 获取加密密码
	 * @param password 密码
	 * @return 加密密码
	 */
	String getEncryptPassword(String password);

	/**
	 * 用户登录
	 * @param userLoginRequest 用户登录请求
	 * @return 用户信息
	 */
	LoginUserVO userLogin(UserLoginRequest userLoginRequest, HttpServletRequest request);

	/**
	 * 获取登录用户信息
	 * @param user  用户
	 * @return 用户信息
	 */
	LoginUserVO getLoginUserVO(User user);

	/**
	 * 获取当前登录用户
	 * @param request 请求
	 * @return 用户信息
	 */
	User getLoginUser(HttpServletRequest request);

	/**
	 * 退出登录
	 * @param request 请求
	 * @return 退出登录结果
	 */
	boolean userLogout(HttpServletRequest request);

	/**
	 * 获取用户信息(脱敏）
	 * @param user  用户
	 * @return 用户信息
	 */
	UserVO getUserVO(User user);

	/**
	 * 获取用户列表（脱敏
	 * @param userList 用户列表
	 * @return 用户列表
	 */
	List<UserVO> listUserVO(List<User> userList);

	/**
	 * 获取查询包装类
	 * @param userQueryRequest 查询条件
	 * @return 查询包装类
	 */
	QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);

	/**
	 * 添加用户
	 * @param userAddRequest 添加用户请求
	 * @return 添加结果
	 */
	Boolean addUser(UserAddRequest userAddRequest);

	/**
	 * 根据id获取用户 管理员
	 * @param id  id
	 * @return  用户
	 */
	User getUserById(Long id);

	/**
	 * 根据id获取用户VO
	 * @param id  id
	 * @return  用户VO
	 */
	UserVO getUserVOById(Long id);

	/**
	 * 删除用户
	 * @param deleteRequest 删除条件
	 * @return 删除结果
	 */
	Boolean deleteUser(DeleteRequest deleteRequest);

	/**
	 * 修改用户
	 * @param userUpdateRequest 修改用户请求
	 * @return 修改结果
	 */
	Boolean updateUser(UserUpdateRequest userUpdateRequest);

	/**
	 * 分页获取用户列表
	 * @param userQueryRequest 查询条件
	 * @return 用户列表
	 */
	Page<UserVO> listPageUserVO(UserQueryRequest userQueryRequest);

	/**
	 * 判断是否为管理员
	 * @param user  用户
	 * @return 是否为管理员
	 */
	boolean isAdmin(User user);
}
