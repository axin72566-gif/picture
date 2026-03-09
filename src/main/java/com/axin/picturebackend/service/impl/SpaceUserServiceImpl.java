package com.axin.picturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.axin.picturebackend.common.DeleteRequest;
import com.axin.picturebackend.exception.BusinessException;
import com.axin.picturebackend.exception.ErrorCode;
import com.axin.picturebackend.model.Enum.SpaceRoleEnum;
import com.axin.picturebackend.model.dto.spaceuser.SpaceUserAddRequest;
import com.axin.picturebackend.model.dto.spaceuser.SpaceUserEditRequest;
import com.axin.picturebackend.model.dto.spaceuser.SpaceUserQueryRequest;
import com.axin.picturebackend.model.entity.Space;
import com.axin.picturebackend.model.entity.User;
import com.axin.picturebackend.model.vo.SpaceUserVO;
import com.axin.picturebackend.model.vo.SpaceVO;
import com.axin.picturebackend.model.vo.UserVO;
import com.axin.picturebackend.service.SpaceService;
import com.axin.picturebackend.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.axin.picturebackend.model.entity.SpaceUser;
import com.axin.picturebackend.service.SpaceUserService;
import com.axin.picturebackend.mapper.SpaceUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
* @author kdkt1
* @description 针对表【space_user(空间用户关联)】的数据库操作Service实现
* @createDate 2026-02-12 23:04:10
*/
@Service
public class SpaceUserServiceImpl extends ServiceImpl<SpaceUserMapper, SpaceUser>
    implements SpaceUserService{

	@Resource
	private SpaceService spaceService;
	@Resource
	private UserService userService;
	@Resource
	private TransactionTemplate transactionTemplate;

	/**
	 * 添加空间用户
	 * @param spaceUserAddRequest 添加空间用户请求
	 * @param loginUser           登录用户
	 * @return 添加的空间用户 ID
	 */
	@Override
	public Long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest, User loginUser) {
		// 参数校验
		if (spaceUserAddRequest == null) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
		}
		SpaceUser spaceUser = new SpaceUser();
		BeanUtil.copyProperties(spaceUserAddRequest, spaceUser);
		// 校验
		validSpaceUser(spaceUser);
		// 插入数据
		boolean result = this.save(spaceUser);
		if (!result) {
			throw new BusinessException(ErrorCode.OPERATION_ERROR, "添加失败");
		}
		return spaceUser.getId();
	}

	/**
	 * 校验空间用户
	 * @param spaceUser 空间用户
	 */
	@Override
	public void validSpaceUser(SpaceUser spaceUser) {
		if (spaceUser == null) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
		}
		// 校验空间 ID
		Long spaceId = spaceUser.getSpaceId();
		Space space = spaceService.getById(spaceId);
		if (space == null) {
			throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "空间不存在");
		}
		// 校验用户 ID
		Long userId = spaceUser.getUserId();
		User user = userService.getById(userId);
		if (user == null) {
			throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
		}
		// 校验空间角色
		String spaceRole = spaceUser.getSpaceRole();
		List<String> spaceUserRoles = SpaceRoleEnum.getAllValues();
		if (!spaceUserRoles.contains(spaceRole)) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间角色错误");
		}
	}

	/**
	 * 获取空间用户
	 * @param spaceUser 空间用户
	 * @param loginUser 登录用户
	 * @return 空间用户
	 */
	@Override
	public SpaceUserVO getSpaceUserVO(SpaceUser spaceUser, User loginUser) {
		if (spaceUser == null) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
		}
		SpaceUserVO spaceUserVO = SpaceUserVO.objToVo(spaceUser);
		Long spaceId = spaceUser.getSpaceId();
		Long userId = spaceUser.getUserId();
		SpaceVO spaceVO = spaceService.getSpaceVOById(spaceId, loginUser);
		spaceUserVO.setSpace(spaceVO);
		UserVO userVO = userService.getUserVO(userService.getById(userId));
		spaceUserVO.setUser(userVO);
		return spaceUserVO;
	}

	/**
	 * 获取空间用户列表
	 *
	 * @param spaceUserList 空间用户列表
	 * @return 查询结果
	 */
	@Override
	public List<SpaceUserVO> listSpaceUserVO(List<SpaceUser> spaceUserList, User loginUser) {
		if (spaceUserList == null) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
		}
		return spaceUserList.stream().map(spaceUser -> {
			SpaceUserVO spaceUserVO = SpaceUserVO.objToVo(spaceUser);
			Long spaceId = spaceUser.getSpaceId();
			Long userId = spaceUser.getUserId();
			SpaceVO spaceVO = spaceService.getSpaceVOById(spaceId, loginUser);
			spaceUserVO.setSpace(spaceVO);
			UserVO userVO = userService.getUserVO(userService.getById(userId));
			spaceUserVO.setUser(userVO);
			return spaceUserVO;
		}).collect(Collectors.toList());
	}

	/**
	 * 获取查询条件
	 * @param spaceUserQueryRequest 查询条件
	 * @return 查询条件
	 */
	@Override
	public QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest) {
		if (spaceUserQueryRequest == null) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
		}
		Long id = spaceUserQueryRequest.getId();
		Long spaceId = spaceUserQueryRequest.getSpaceId();
		Long userId = spaceUserQueryRequest.getUserId();
		String spaceRole = spaceUserQueryRequest.getSpaceRole();
		String sortField = spaceUserQueryRequest.getSortField();
		String sortOrder = spaceUserQueryRequest.getSortOrder();
		QueryWrapper<SpaceUser> queryWrapper = new QueryWrapper<>();
		queryWrapper.eq(id != null, "id", id);
		queryWrapper.eq(spaceId != null, "spaceId", spaceId);
		queryWrapper.eq(userId != null, "userId", userId);
		queryWrapper.eq(spaceRole != null, "spaceRole", spaceRole);
		queryWrapper.orderBy(StringUtils.isNotBlank(sortField), sortOrder.equals("ascend"), sortField);
		return queryWrapper;
	}

	/**
	 * 删除空间成员
	 * @param deleteRequest 删除条件
	 * @param loginUser 登录用户
	 * @return 删除结果
	 */
	@Override
	public Boolean deleteSpace(DeleteRequest deleteRequest, User loginUser) {
		if (deleteRequest == null) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR, "删除空间为空");
		}
		Long id = deleteRequest.getId();
		SpaceUser oldSpaceUser = this.getById(id);
		if (oldSpaceUser == null) {
			throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "空间不存在");
		}
		boolean delete = this.removeById(id);
		if (!delete) {
			throw new BusinessException(ErrorCode.OPERATION_ERROR, "删除失败");
		}
		return true;
	}

	/**
	 * 获取空间成员
	 * @param spaceUserQueryRequest 查询条件
	 * @param loginUser 登录用户
	 * @return 空间成员
	 */
	@Override
	public SpaceUser getSpaceUser(SpaceUserQueryRequest spaceUserQueryRequest, User loginUser) {
		if (spaceUserQueryRequest == null) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
		}
		QueryWrapper<SpaceUser> queryWrapper = getQueryWrapper(spaceUserQueryRequest);
		SpaceUser one = this.getOne(queryWrapper);
		if (one == null) {
			throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "空间成员不存在");
		}
		return one;
	}

	/**
	 * 编辑空间成员
	 * @param spaceUserEditRequest 编辑空间成员请求
	 * @param loginUser 登录用户
	 * @return 编辑结果
	 */
	@Override
	public Boolean editSpaceUser(SpaceUserEditRequest spaceUserEditRequest, User loginUser) {
		if (spaceUserEditRequest == null) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
		}
		Long id = spaceUserEditRequest.getId();
		SpaceUser oldSpaceUser = this.getById(id);
		if (oldSpaceUser == null) {
			throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "空间成员不存在");
		}
		SpaceUser updateSpaceUser = new SpaceUser();
		BeanUtil.copyProperties(spaceUserEditRequest, updateSpaceUser);
		boolean update = this.updateById(updateSpaceUser);
		if (!update) {
			throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新失败");
		}
		return true;
	}

	/**
	 * 获取用户空间
	 * @param loginUser 登录用户
	 * @return 用户空间
	 */
	@Override
	public List<SpaceUserVO> listMySpaces(User loginUser) {
		Long userId = loginUser.getId();
		List<SpaceUser> spaceUserList = this.list(new QueryWrapper<SpaceUser>().eq("userId", userId));
		if (spaceUserList.isEmpty()) {
			throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户团队空间不存在");
		}
		return listSpaceUserVO(spaceUserList, loginUser);
	}

	/**
	 * 获取空间成员列表
	 * @param spaceUserQueryRequest 查询条件
	 * @param loginUser 登录用户
	 * @return 获取空间成员列表
	 */
	@Override
	public Page<SpaceUserVO> listSpaceUser(SpaceUserQueryRequest spaceUserQueryRequest, User loginUser) {
		if (spaceUserQueryRequest == null) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
		}
		QueryWrapper<SpaceUser> queryWrapper = getQueryWrapper(spaceUserQueryRequest);
		Page<SpaceUser> spaceUserPage = this.page(new Page<>(spaceUserQueryRequest.getCurrent(), spaceUserQueryRequest.getPageSize()), queryWrapper);
		List<SpaceUser> spaceUserList = spaceUserPage.getRecords();
		List<SpaceUserVO> spaceUserVOS = listSpaceUserVO(spaceUserList, loginUser);
		Page<SpaceUserVO> spaceUserVOPage = new Page<>(spaceUserQueryRequest.getCurrent(), spaceUserQueryRequest.getPageSize(), spaceUserPage.getTotal());
		spaceUserVOPage.setRecords(spaceUserVOS);
		return spaceUserVOPage;
	}
}




