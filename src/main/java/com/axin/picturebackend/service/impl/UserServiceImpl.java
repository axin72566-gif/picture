package com.axin.picturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.axin.picturebackend.common.DeleteRequest;
import com.axin.picturebackend.exception.BusinessException;
import com.axin.picturebackend.exception.ErrorCode;
import com.axin.picturebackend.exception.ThrowUtils;
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
 * 用户 Service 实现
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {

    /** 密码加密盐值 */
    private static final String SALT = "axin";

    /** 管理员创建用户时的默认密码 */
    private static final String DEFAULT_PASSWORD = "12345678";

    // ====== 注册 / 登录 / 登出 ======

    /**
     * 用户注册
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
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "确认密码过短");
        }
        if (!password.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
        // 检查账号是否重复
        long count = this.count(new QueryWrapper<User>().eq("userAccount", userAccount));
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户已存在");
        }
        // 加密并保存
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(getEncryptPassword(password));
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
     */
    @Override
    public LoginUserVO userLogin(UserLoginRequest userLoginRequest, HttpServletRequest request) {
        if (userLoginRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "登录参数为空");
        }
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        if (userAccount == null || userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if (userPassword == null || userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        User user = this.getOne(new QueryWrapper<User>()
                .eq("userAccount", userAccount)
                .eq("userPassword", getEncryptPassword(userPassword)));
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
     * 用户登出
     */
    @Override
    public boolean userLogout(HttpServletRequest request) {
        if (request.getSession().getAttribute(USER_LOGIN_STATE) == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未登录");
        }
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return true;
    }

    // ====== 查询 / 脱敏 ======

    /**
     * 获取当前登录用户
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        return (User) request.getSession().getAttribute(USER_LOGIN_STATE);
    }

    /**
     * 获取登录用户脱敏视图
     */
    @Override
    public LoginUserVO getLoginUserVO(User user) {
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtil.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    /**
     * 获取用户脱敏视图
     */
    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在");
        }
        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO);
        return userVO;
    }

    /**
     * 批量获取用户脱敏视图
     */
    @Override
    public List<UserVO> listUserVO(List<User> userList) {
        if (userList == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户列表为空");
        }
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }

    /**
     * 根据ID获取用户实体
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
     * 根据ID获取用户脱敏视图
     */
    @Override
    public UserVO getUserVOById(Long id) {
        return getUserVO(getUserById(id));
    }

    /**
     * 构建查询条件
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
        queryWrapper.orderBy(StringUtils.isNotBlank(sortField), "ascend".equals(sortOrder), sortField);
        return queryWrapper;
    }

    /**
     * 分页查询用户脱敏视图
     */
    @Override
    public Page<UserVO> listPageUserVO(UserQueryRequest userQueryRequest) {
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR, "查询条件为空");
        long current = userQueryRequest.getCurrent();
        long pageSize = userQueryRequest.getPageSize();
        QueryWrapper<User> queryWrapper = getQueryWrapper(userQueryRequest);
        Page<User> userPage = this.page(new Page<>(current, pageSize), queryWrapper);
        List<UserVO> userVOS = this.listUserVO(userPage.getRecords());
        Page<UserVO> userVOPage = new Page<>(current, pageSize, userPage.getTotal());
        userVOPage.setRecords(userVOS);
        return userVOPage;
    }

    // ====== 管理员操作 ======

    /**
     * 管理员添加用户（默认密码：12345678）
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
        user.setUserPassword(getEncryptPassword(DEFAULT_PASSWORD));
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
     * 删除用户
     */
    @Override
    public Boolean deleteUser(DeleteRequest deleteRequest) {
        if (deleteRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long id = deleteRequest.getId();
        if (this.getById(id) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }
        return this.removeById(id);
    }

    /**
     * 修改用户
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

    // ====== 权限 / 加密 ======

    /**
     * 判断是否为管理员
     */
    @Override
    public boolean isAdmin(User user) {
        if (user == null) {
            return false;
        }
        return UserRoleEnum.ADMIN.getValue().equals(user.getUserRole());
    }

    /**
     * 修改用户信息
     * @param userEditRequest 修改用户信息请求
     * @return 修改结果
     */
    @Override
    public Boolean editUser(UserEditRequest userEditRequest) {
        if (userEditRequest == null || userEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户id不能为空");
        }
        User user = new User();
        BeanUtil.copyProperties(userEditRequest, user);
        boolean update = this.updateById(user);
        if (!update) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "修改用户信息失败");
        }
        return true;
    }

    /**
     * MD5+盐值加密
     */
    @Override
    public String getEncryptPassword(String userPassword) {
        return DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
    }
}
