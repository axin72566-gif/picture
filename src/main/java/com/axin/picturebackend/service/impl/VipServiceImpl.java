package com.axin.picturebackend.service.impl;

import cn.hutool.core.date.DateUtil;
import com.axin.picturebackend.constant.UserConstant;
import com.axin.picturebackend.exception.BusinessException;
import com.axin.picturebackend.exception.ErrorCode;
import com.axin.picturebackend.mapper.UserVipMapper;
import com.axin.picturebackend.model.entity.User;
import com.axin.picturebackend.model.entity.UserVip;
import com.axin.picturebackend.model.vo.UserVipVO;
import com.axin.picturebackend.service.VipService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;

/**
 * 用户会员 Service 实现
 */
@Slf4j
@Service
public class VipServiceImpl extends ServiceImpl<UserVipMapper, UserVip>
        implements VipService {

    @Resource
    private com.axin.picturebackend.mapper.UserMapper userMapper;

    /**
     * 开通或续费VIP
     * - 若用户已有有效VIP：在现有到期时间基础上续期
     * - 若用户VIP已到期或从未开通：从当前时间起计算到期时间
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void openOrRenewVip(Long userId, int days) {
        if (userId == null || days <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数异常");
        }

        // 查询当前 user_vip 记录（包含已到期的）
        UserVip userVip = this.getOne(
                new LambdaQueryWrapper<UserVip>().eq(UserVip::getUserId, userId)
        );

        Date now = new Date();
        if (userVip == null) {
            // 首次开通
            userVip = new UserVip();
            userVip.setUserId(userId);
            userVip.setVipLevel(1);
            userVip.setExpireTime(DateUtil.offsetDay(now, days));
            this.save(userVip);
        } else {
            // 续费：若当前 VIP 未到期则基于到期时间续，否则基于当前时间续
            Date base = userVip.getExpireTime().after(now) ? userVip.getExpireTime() : now;
            userVip.setExpireTime(DateUtil.offsetDay(base, days));
            this.updateById(userVip);
        }

        // 同步更新 user.userRole = vip
        User updateUser = new User();
        updateUser.setId(userId);
        updateUser.setUserRole(UserConstant.VIP_ROLE);
        userMapper.updateById(updateUser);

        log.info("用户[{}] VIP 开通/续费成功，到期时间：{}", userId, userVip.getExpireTime());
    }

    /**
     * 查询用户VIP信息
     */
    @Override
    public UserVipVO getVipInfo(Long userId) {
        UserVipVO vo = new UserVipVO();
        UserVip userVip = this.getOne(
                new LambdaQueryWrapper<UserVip>().eq(UserVip::getUserId, userId)
        );
        if (userVip == null || !userVip.getExpireTime().after(new Date())) {
            vo.setIsVip(false);
            vo.setRemainDays(-1L);
            return vo;
        }
        long remainMs = userVip.getExpireTime().getTime() - System.currentTimeMillis();
        long remainDays = remainMs / (1000 * 60 * 60 * 24);
        vo.setIsVip(true);
        vo.setVipLevel(userVip.getVipLevel());
        vo.setExpireTime(userVip.getExpireTime());
        vo.setRemainDays(Math.max(remainDays, 0L));
        return vo;
    }

    /**
     * 判断VIP是否有效（实时查库）
     */
    @Override
    public boolean isVip(Long userId) {
        if (userId == null) {
            return false;
        }
        UserVip userVip = this.getOne(
                new LambdaQueryWrapper<UserVip>().eq(UserVip::getUserId, userId)
        );
        return userVip != null && userVip.getExpireTime().after(new Date());
    }
}
