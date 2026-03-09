package com.axin.picturebackend.service;

import com.axin.picturebackend.common.DeleteRequest;
import com.axin.picturebackend.model.dto.space.SpaceAddRequest;
import com.axin.picturebackend.model.dto.space.SpaceEditRequest;
import com.axin.picturebackend.model.dto.space.SpaceQueryRequest;
import com.axin.picturebackend.model.dto.space.SpaceUpdateRequest;
import com.axin.picturebackend.model.entity.Space;
import com.axin.picturebackend.model.entity.User;
import com.axin.picturebackend.model.vo.SpaceVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author kdkt1
* @description 针对表【space(空间)】的数据库操作Service
* @createDate 2026-01-25 10:59:10
*/
public interface SpaceService extends IService<Space> {

	/**
	 * 校验
	 * @param space  空间
	 * @param add  是否为创建
	 */
	void validSpace(Space space, boolean add);

	/**
	 * 填充空间参数
	 * @param space  空间
	 */
	void fillSpaceParam(Space space);

	/**
	 * 修改空间
	 * @param spaceUpdateRequest 修改空间请求
	 * @return 修改空间结果
	 */
	Boolean updateSpace(SpaceUpdateRequest spaceUpdateRequest);

	/**
	 * 添加空间
	 * @param spaceAddRequest 添加空间请求
	 * @param loginUser 登录用户
	 * @return 添加空间结果
	 */
	Long addSpace(SpaceAddRequest spaceAddRequest, User loginUser);

	/**
	 * 删除空间
	 * @param deleteRequest 删除条件
	 * @param loginUser 登录用户
	 * @return 删除结果
	 */
	boolean deleteSpace(DeleteRequest deleteRequest, User loginUser);

	/**
	 * 获取查询条件
	 * @param spaceQueryRequest 查询条件
	 * @return 查询条件
	 */
	QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);

	/**
	 *  根据id获取空间
	 * @param id  id
	 * @return 空间
	 */
	Space getSpaceById(Long id, User loginUser);

	/**
	 * 获取空间
	 * @param id  id
	 * @param loginUser 登录用户
	 * @return 空间
	 */
	SpaceVO getSpaceVOById(Long id, User loginUser);

	/**
	 * 获取空间列表
	 * @param spaceQueryRequest 查询条件
	 * @param loginUser 登录用户
	 * @return 空间列表
	 */
	Page<Space> listPageSpace(SpaceQueryRequest spaceQueryRequest, User loginUser);

	/**
	 * 获取空间列表
	 * @param spaceQueryRequest 查询条件
	 * @param loginUser 登录用户
	 * @return 空间列表
	 */
	Page<SpaceVO> listPageSpaceVO(SpaceQueryRequest spaceQueryRequest, User loginUser);

	/**
	 * 编辑空间
	 * @param spaceEditRequest 编辑空间请求
	 * @param loginUser 登录用户
	 * @return 编辑空间结果
	 */
	Boolean editSpace(SpaceEditRequest spaceEditRequest, User loginUser);
}
