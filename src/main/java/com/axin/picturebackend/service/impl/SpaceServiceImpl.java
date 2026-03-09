package com.axin.picturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.axin.picturebackend.common.DeleteRequest;
import com.axin.picturebackend.exception.BusinessException;
import com.axin.picturebackend.exception.ErrorCode;
import com.axin.picturebackend.manager.auth.SpaceUserAuthManager;
//import com.axin.picturebackend.manager.sharding.DynamicShardingManager;
import com.axin.picturebackend.model.Enum.SpaceLevelEnum;
import com.axin.picturebackend.model.Enum.SpaceRoleEnum;
import com.axin.picturebackend.model.Enum.SpaceTypeEnum;
import com.axin.picturebackend.model.dto.space.SpaceAddRequest;
import com.axin.picturebackend.model.dto.space.SpaceEditRequest;
import com.axin.picturebackend.model.dto.space.SpaceQueryRequest;
import com.axin.picturebackend.model.dto.space.SpaceUpdateRequest;
import com.axin.picturebackend.model.entity.SpaceUser;
import com.axin.picturebackend.model.entity.User;
import com.axin.picturebackend.model.vo.SpaceVO;
import com.axin.picturebackend.model.vo.UserVO;
import com.axin.picturebackend.service.SpaceUserService;
import com.axin.picturebackend.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.axin.picturebackend.model.entity.Space;
import com.axin.picturebackend.service.SpaceService;
import com.axin.picturebackend.mapper.SpaceMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
* @author kdkt1
* @description 针对表【space(空间)】的数据库操作Service实现
* @createDate 2026-01-25 10:59:10
*/
@Service
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space>
    implements SpaceService {

	@Resource
	private TransactionTemplate transactionTemplate;
	@Resource
	private UserService userService;
	@Resource
	@Lazy
	private SpaceUserService spaceUserService;
	@Resource
	@Lazy
	private SpaceUserAuthManager spaceUserAuthManager;
//	@Resource
//	private DynamicShardingManager dynamicShardingManager;

	/**
	 * 校验空间名称和级别
	 *
	 * @param space 空间
	 * @param add   是否为创建
	 */
	@Override
	public void validSpace(Space space, boolean add) {
		if (space == null) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间为空");
		}
		// 取值
		String spaceName = space.getSpaceName();
		Integer spaceLevel = space.getSpaceLevel();
		// 如果是创建，空间名称和空间级别不能为空
		if (add) {
			if (spaceName == null || spaceLevel == null) {
				throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称和空间级别不能为空");
			}
			if (spaceName.length() > 20) {
				throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称过长");
			}
			if (space.getSpaceType() != 0 && space.getSpaceType() != 1) {
				throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间类型错误");
			}
		} else {
			// 修改空间
			if (spaceName == null || spaceName.length() > 20) {
				throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称过长");
			}
			//空间级别不存在
			if (spaceLevel == null || SpaceLevelEnum.getEnumByValue(spaceLevel) == null) {
				throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间级别不存在");
			}
			if (space.getSpaceType() != 0 && space.getSpaceType() != 1) {
				throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间类型错误");
			}
		}
	}

	/**
	 * 填充空间额度
	 *
	 * @param space 空间
	 */
	@Override
	public void fillSpaceParam(Space space) {
		if (space == null) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间为空");
		}
		Integer spaceLevel = space.getSpaceLevel();
		// 获取枚举
		SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(spaceLevel);
		if (spaceLevelEnum == null) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间级别不存在");
		}
		// 填充参数
		space.setMaxCount(spaceLevelEnum.getMaxCount());
		space.setMaxSize(spaceLevelEnum.getMaxSize());
	}

	/**
	 * 修改空间
	 *
	 * @param spaceUpdateRequest 修改空间请求
	 * @return 修改空间结果
	 */
	@Override
	public Boolean updateSpace(SpaceUpdateRequest spaceUpdateRequest) {
		// 校验参数
		if (spaceUpdateRequest == null) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR, "修改空间为空");
		}
		Long id = spaceUpdateRequest.getId();
		if (id == null) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间id错误");
		}
		// 判断老空间是否存在
		Space oldSpace = this.getById(id);
		if (oldSpace == null) {
			throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "空间不存在");
		}
		Space newSpace = new Space();
		BeanUtil.copyProperties(spaceUpdateRequest, newSpace);
		// 校验名称和级别
		validSpace(newSpace, false);
		// 填充空间额度
		fillSpaceParam(newSpace);
		boolean update = this.updateById(newSpace);
		if (!update) {
			throw new BusinessException(ErrorCode.OPERATION_ERROR, "修改空间失败");
		}
		return true;
	}

	/**
	 * 添加空间
	 *
	 * @param spaceAddRequest 添加空间请求
	 * @param loginUser       登录用户
	 * @return 添加空间结果
	 */
	@Override
	public Long addSpace(SpaceAddRequest spaceAddRequest, User loginUser) {
		if (spaceAddRequest == null) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间创建参数为空");
		}
		Space newSpace = new Space();
		// 名称 级别 类型
		BeanUtil.copyProperties(spaceAddRequest, newSpace);
		if (newSpace.getSpaceName() == null) {
			newSpace.setSpaceName("默认空间");
		}
		if (newSpace.getSpaceLevel() == null) {
			newSpace.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
		}
		if (newSpace.getSpaceType() == null) {
			// 默认私有
			newSpace.setSpaceType(SpaceTypeEnum.PRIVATE.getValue());
		}
		validSpace(newSpace, true);
		// 填充空间额度
		fillSpaceParam(newSpace);
		newSpace.setUserId(loginUser.getId());
		// 用户只能创建普通版，旗舰版需要管理员权限
		if (newSpace.getSpaceLevel() != SpaceLevelEnum.COMMON.getValue() && !userService.isAdmin(loginUser)) {
			throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "用户无权限创建旗舰版空间");
		}
		// 对用户id加锁
		String lock = String.valueOf(loginUser.getId()).intern();
		synchronized (lock) {
			// 判断用户是否有空间
			return transactionTemplate.execute(transactionStatus -> {
				// 判断用户是否有空间
				Long userId = loginUser.getId();
				Long spaceType = Long.valueOf(newSpace.getSpaceType());
				QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
				queryWrapper.eq("userId", userId);
				queryWrapper.eq("spaceType", spaceType);
				if (this.count(queryWrapper) > 0) {
					throw new BusinessException(ErrorCode.OPERATION_ERROR, "用户已存在此类型空间");
				}
				boolean save = this.save(newSpace);
				if (!save) {
					throw new BusinessException(ErrorCode.OPERATION_ERROR, "创建空间失败");
				}
				// 如果是团队空间，自己就是空间成员且是管理者
				if (newSpace.getSpaceType() == SpaceTypeEnum.TEAM.getValue()) {
					SpaceUser spaceUser = new SpaceUser();
					spaceUser.setSpaceId(newSpace.getId());
					spaceUser.setUserId(loginUser.getId());
					spaceUser.setSpaceRole(SpaceRoleEnum.ADMIN.getValue());
					boolean saveSpaceUserCreator = spaceUserService.save(spaceUser);
					if (!saveSpaceUserCreator) {
						throw new BusinessException(ErrorCode.OPERATION_ERROR, "创建空间失败");
					}
//					// 创建分表
//					dynamicShardingManager.createSpacePictureTable(newSpace);
				}
				return newSpace.getId();
			});
		}
	}

	/**
	 * 删除空间
	 *
	 * @param deleteRequest 删除条件
	 * @param loginUser     登录用户
	 * @return 删除结果
	 */
	@Override
	public boolean deleteSpace(DeleteRequest deleteRequest, User loginUser) {
		if (deleteRequest == null) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR, "删除空间为空");
		}
		Long id = deleteRequest.getId();
		Space space = this.getById(id);
		if (space == null) {
			throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "空间不存在");
		}
		boolean delete = this.removeById(id);
		if (!delete) {
			throw new BusinessException(ErrorCode.OPERATION_ERROR, "删除空间失败");
		}
		return true;
	}

	/**
	 * 获取查询包装类
	 *
	 * @param spaceQueryRequest 查询条件
	 * @return 查询包装类
	 */
	@Override
	public QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest) {
		if (spaceQueryRequest == null) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR, "查询条件为空");
		}
		Long id = spaceQueryRequest.getId();
		Long userId = spaceQueryRequest.getUserId();
		String spaceName = spaceQueryRequest.getSpaceName();
		Integer spaceLevel = spaceQueryRequest.getSpaceLevel();
		Integer spaceType = spaceQueryRequest.getSpaceType();
		String sortField = spaceQueryRequest.getSortField();
		String sortOrder = spaceQueryRequest.getSortOrder();
		// 封装条件
		QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
		queryWrapper.eq(id != null && id > 0, "id", id);
		queryWrapper.eq(userId != null && userId > 0, "userId", userId);
		queryWrapper.like(StringUtils.isNotBlank(spaceName), "spaceName", spaceName);
		queryWrapper.eq(spaceLevel != null, "spaceLevel", spaceLevel);
		queryWrapper.eq(spaceType != null, "spaceType", spaceType);
		queryWrapper.orderBy(StringUtils.isNotBlank(sortField), sortOrder.equals("ascend"), sortField);
		return queryWrapper;
	}

	/**
	 * 获取空间
	 *
	 * @param id id
	 * @return 空间
	 */
	@Override
	public Space getSpaceById(Long id, User loginUser) {
		if (id == null) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间id错误");
		}
		Space space = this.getById(id);
		if (space == null) {
			throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "空间不存在");
		}
		return space;
	}

	/**
	 * 获取空间视图
	 *
	 * @param id        id
	 * @param loginUser 登录用户
	 * @return 空间视图
	 */
	@Override
	public SpaceVO getSpaceVOById(Long id, User loginUser) {
		Space space = this.getSpaceById(id, loginUser);
		SpaceVO spaceVO = SpaceVO.objToVo(space);
		Long userId = space.getUserId();
		if (userId == null) {
			throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户id不存在");
		}
		User user = userService.getById(userId);
		if (user == null) {
			throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
		}
		UserVO userVO = new UserVO();
		BeanUtils.copyProperties(user, userVO);
		spaceVO.setUser(userVO);
		// 获取登录用户对空间的权限
		List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);
		spaceVO.setPermissionList(permissionList);
		return spaceVO;
	}

	/**
	 * 获取空间列表
	 * @param spaceQueryRequest 查询条件
	 * @param loginUser 登录用户
	 * @return 空间列表
	 */
	@Override
	public Page<Space> listPageSpace(SpaceQueryRequest spaceQueryRequest, User loginUser) {
		if (spaceQueryRequest == null) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR, "查询条件为空");
		}
		int current = spaceQueryRequest.getCurrent();
		int pageSize = spaceQueryRequest.getPageSize();
		QueryWrapper<Space> queryWrapper = getQueryWrapper(spaceQueryRequest);
		Page<Space> spacePage = this.page(new Page<>(current, pageSize), queryWrapper);
		if (spacePage == null || spacePage.getTotal() <= 0) {
			// 返回空
			return new Page<>();
		}
		return spacePage;
	}

	/**
	 * 获取空间列表视图
	 * @param spaceQueryRequest 查询条件
	 * @param loginUser 登录用户
	 * @return 空间列表视图
	 */
	@Override
	public Page<SpaceVO> listPageSpaceVO(SpaceQueryRequest spaceQueryRequest, User loginUser) {
		Page<Space> spacePage = listPageSpace(spaceQueryRequest, loginUser);
		if (spacePage == null || spacePage.getTotal() <= 0) {
			// 返回空
			return new Page<>();
		}
		List<Space> spaceList = spacePage.getRecords();
		// 获取所有用户id
		List<Long> userIdList = spaceList.stream().map(Space::getUserId).collect(Collectors.toList());
		Map<Long, User> userMap = userService.listByIds(userIdList).stream().collect(Collectors.toMap(User::getId, user -> user));
		List<SpaceVO> spaceVOList = spaceList.stream().map(space -> {
			SpaceVO spaceVO = SpaceVO.objToVo(space);
			Long userId = space.getUserId();
			User user = userMap.get(userId);
			UserVO userVO = new UserVO();
			BeanUtils.copyProperties(user, userVO);
			spaceVO.setUser(userVO);
			return spaceVO;
		}).collect(Collectors.toList());
		Page<SpaceVO> spaceVOPage = new Page<>(spacePage.getCurrent(), spacePage.getSize(), spacePage.getTotal());
		spaceVOPage.setRecords(spaceVOList);
		return spaceVOPage;
	}

	/**
	 * 编辑空间
	 * @param spaceEditRequest 编辑空间请求
	 * @param loginUser 登录用户
	 * @return 编辑空间结果
	 */
	@Override
	public Boolean editSpace(SpaceEditRequest spaceEditRequest, User loginUser) {
		if (spaceEditRequest == null) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR, "编辑空间为空");
		}
		Long id = spaceEditRequest.getId();
		Space space = this.getById(id);
		if (space == null) {
			throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "空间不存在");
		}
		Long userId = loginUser.getId();
		if (!userId.equals(space.getUserId())) {
			throw new BusinessException(ErrorCode.OPERATION_ERROR, "无权限编辑空间");
		}
		Space updateSpace = new Space();
		updateSpace.setId(id);
		updateSpace.setSpaceName(spaceEditRequest.getSpaceName());
		boolean update = this.updateById(updateSpace);
		if (!update) {
			throw new BusinessException(ErrorCode.OPERATION_ERROR, "编辑空间失败");
		}
		return true;
	}
}














