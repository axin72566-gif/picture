package com.axin.picturebackend.service;

import com.axin.picturebackend.common.DeleteRequest;
import com.axin.picturebackend.model.dto.spaceuser.SpaceUserAddRequest;
import com.axin.picturebackend.model.dto.spaceuser.SpaceUserEditRequest;
import com.axin.picturebackend.model.dto.spaceuser.SpaceUserQueryRequest;
import com.axin.picturebackend.model.entity.SpaceUser;
import com.axin.picturebackend.model.entity.User;
import com.axin.picturebackend.model.vo.SpaceUserVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 空间用户关联 Service 接口
 */
public interface SpaceUserService extends IService<SpaceUser> {

    /**
     * 添加空间用户
     *
     * @param spaceUserAddRequest 添加请求
     * @param loginUser           登录用户
     * @return 新增记录 ID
     */
    Long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest, User loginUser);

    /**
     * 校验空间用户（空间/用户存在性、角色合法性）
     *
     * @param spaceUser 空间用户
     */
    void validSpaceUser(SpaceUser spaceUser);

    /**
     * 获取空间用户视图（含空间信息和用户信息）
     *
     * @param spaceUser 空间用户
     * @param loginUser 登录用户
     * @return 视图
     */
    SpaceUserVO getSpaceUserVO(SpaceUser spaceUser, User loginUser);

    /**
     * 批量获取空间用户视图
     *
     * @param spaceUserList 列表
     * @param loginUser     登录用户
     * @return 视图列表
     */
    List<SpaceUserVO> listSpaceUserVO(List<SpaceUser> spaceUserList, User loginUser);

    /**
     * 构建查询条件
     *
     * @param spaceUserQueryRequest 查询请求
     * @return 查询包装类
     */
    QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest);

    /**
     * 删除空间成员
     *
     * @param deleteRequest 删除请求
     * @param loginUser     登录用户
     * @return 是否成功
     */
    Boolean deleteSpaceUser(DeleteRequest deleteRequest, User loginUser);

    /**
     * 获取空间成员记录
     *
     * @param spaceUserQueryRequest 查询请求
     * @param loginUser             登录用户
     * @return 空间成员
     */
    SpaceUser getSpaceUser(SpaceUserQueryRequest spaceUserQueryRequest, User loginUser);

    /**
     * 编辑空间成员角色
     *
     * @param spaceUserEditRequest 编辑请求
     * @param loginUser            登录用户
     * @return 是否成功
     */
    Boolean editSpaceUser(SpaceUserEditRequest spaceUserEditRequest, User loginUser);

    /**
     * 获取登录用户参与的所有团队空间
     *
     * @param loginUser 登录用户
     * @return 空间用户视图列表
     */
    List<SpaceUserVO> listMySpaces(User loginUser);

    /**
     * 分页查询空间成员
     *
     * @param spaceUserQueryRequest 查询请求
     * @param loginUser             登录用户
     * @return 分页结果
     */
    Page<SpaceUserVO> listSpaceUser(SpaceUserQueryRequest spaceUserQueryRequest, User loginUser);
}
