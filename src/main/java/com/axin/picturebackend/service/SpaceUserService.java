package com.axin.picturebackend.service;

import com.axin.picturebackend.common.DeleteRequest;
import com.axin.picturebackend.model.dto.spaceuser.SpaceUserAddRequest;
import com.axin.picturebackend.model.dto.spaceuser.SpaceUserEditRequest;
import com.axin.picturebackend.model.dto.spaceuser.SpaceUserQueryRequest;
import com.axin.picturebackend.model.entity.SpaceUser;
import com.axin.picturebackend.model.entity.User;
import com.axin.picturebackend.model.vo.SpaceUserVO;
import com.axin.picturebackend.model.vo.SpaceVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author kdkt1
* @description 针对表【space_user(空间用户关联)】的数据库操作Service
* @createDate 2026-02-12 23:04:10
*/
public interface SpaceUserService extends IService<SpaceUser> {

	/**
	 * 添加空间用户
	 *
	 * @param spaceUserAddRequest 添加空间用户请求
	 * @param loginUser           登录用户
	 * @return 添加空间用户结果
	 */
	Long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest, User loginUser);

	/**
	 * 校验
	 * @param spaceUser 空间用户
	 */
	void validSpaceUser(SpaceUser spaceUser);

	/**
	 * 空间用户返回视图
	 *
	 * @param spaceUser 空间用户
	 * @return 封装视图
	 */
	SpaceUserVO getSpaceUserVO(SpaceUser spaceUser, User loginUser);

	/**
	 * 获取空间用户列表
	 * @param spaceUserList 列表
	 * @return 返回查询结果
	 */
	List<SpaceUserVO> listSpaceUserVO(List<SpaceUser> spaceUserList, User loginUser);

	/**
	 * 获取查询条件
	 * @param spaceUserQueryRequest 查询条件
	 * @return 查询条件
	 */
	QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest);

	/**
	 * 删除空间成员
	 * @param deleteRequest 删除条件
	 * @param loginUser 登录用户
	 * @return 删除结果
	 */
	Boolean deleteSpace(DeleteRequest deleteRequest, User loginUser);

	/**
	 * 获取空间成员
	 * @param spaceUserQueryRequest 查询条件
	 * @param loginUser 登录用户
	 * @return 查询结果
	 */
	SpaceUser getSpaceUser(SpaceUserQueryRequest spaceUserQueryRequest, User loginUser);

	/**
	 * 编辑空间成员
	 * @param spaceUserEditRequest 编辑空间成员请求
	 * @param loginUser 登录用户
	 * @return 编辑结果
	 */
	Boolean editSpaceUser(SpaceUserEditRequest spaceUserEditRequest, User loginUser);

	/**
	 * 获取登录用户空间列表
	 * @param loginUser 登录用户
	 * @return 登录用户空间列表
	 */
	List<SpaceUserVO> listMySpaces(User loginUser);

	/**
	 * 获取空间成员列表
	 * @param spaceUserQueryRequest 查询条件
	 * @param loginUser 登录用户
	 * @return 获取空间成员列表
	 */
	Page<SpaceUserVO> listSpaceUser(SpaceUserQueryRequest spaceUserQueryRequest, User loginUser);
}
