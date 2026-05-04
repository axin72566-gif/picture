package com.axin.picturebackend.service.impl;

import com.axin.picturebackend.exception.BusinessException;
import com.axin.picturebackend.exception.ErrorCode;
import com.axin.picturebackend.mapper.CouponMapper;
import com.axin.picturebackend.mapper.SeckillActivityMapper;
import com.axin.picturebackend.model.Enum.CouponStatusEnum;
import com.axin.picturebackend.model.entity.Coupon;
import com.axin.picturebackend.model.entity.SeckillActivity;
import com.axin.picturebackend.model.vo.CouponVO;
import com.axin.picturebackend.service.CouponService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 优惠券 Service 实现
 */
@Slf4j
@Service
public class CouponServiceImpl extends ServiceImpl<CouponMapper, Coupon>
        implements CouponService {

    @Resource
    private SeckillActivityMapper seckillActivityMapper;

    /**
     * 查询当前用户的优惠券列表，按创建时间倒序
     */
    @Override
    public List<CouponVO> listMyCoupons(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        // 自动将已过期且状态为 UNUSED 的券标记为 EXPIRED
        this.update(
                new LambdaUpdateWrapper<Coupon>()
                        .eq(Coupon::getUserId, userId)
                        .eq(Coupon::getStatus, CouponStatusEnum.UNUSED.getValue())
                        .lt(Coupon::getExpireTime, new Date())
                        .set(Coupon::getStatus, CouponStatusEnum.EXPIRED.getValue())
        );

        List<Coupon> coupons = this.list(
                new LambdaQueryWrapper<Coupon>()
                        .eq(Coupon::getUserId, userId)
                        .orderByDesc(Coupon::getCreateTime)
        );
        if (coupons.isEmpty()) {
            return new ArrayList<>();
        }

        // 批量查活动名称
        List<Long> activityIds = coupons.stream()
                .map(Coupon::getActivityId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> activityNameMap = seckillActivityMapper.selectBatchIds(activityIds)
                .stream()
                .collect(Collectors.toMap(SeckillActivity::getId, SeckillActivity::getName));

        List<CouponVO> result = new ArrayList<>();
        for (Coupon coupon : coupons) {
            CouponVO vo = convertToVO(coupon, activityNameMap.get(coupon.getActivityId()));
            result.add(vo);
        }
        return result;
    }

    /**
     * 校验并锁定优惠券（下单时调用）
     * 使用乐观锁更新：WHERE status='UNUSED' AND id=? AND userId=?
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BigDecimal lockCoupon(Long couponId, Long userId) {
        if (couponId == null || userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        Coupon coupon = this.getById(couponId);
        if (coupon == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "优惠券不存在");
        }
        if (!coupon.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权使用该优惠券");
        }
        if (!CouponStatusEnum.UNUSED.getValue().equals(coupon.getStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "优惠券已使用或已过期");
        }
        if (coupon.getExpireTime() != null && coupon.getExpireTime().before(new Date())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "优惠券已过期");
        }

        // 乐观锁更新：status UNUSED -> USED（并发安全）
        int updated = this.baseMapper.update(null,
                new LambdaUpdateWrapper<Coupon>()
                        .eq(Coupon::getId, couponId)
                        .eq(Coupon::getUserId, userId)
                        .eq(Coupon::getStatus, CouponStatusEnum.UNUSED.getValue())
                        .set(Coupon::getStatus, CouponStatusEnum.USED.getValue())
                        .set(Coupon::getUseTime, new Date())
        );
        if (updated == 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "优惠券状态已变更，请刷新重试");
        }

        log.info("[Coupon] 优惠券锁定成功 couponId={}, userId={}, faceValue={}", couponId, userId, coupon.getFaceValue());
        return coupon.getFaceValue();
    }

    /**
     * 根据ID查询有效的（UNUSED 且未过期）优惠券
     */
    @Override
    public CouponVO getValidCoupon(Long couponId, Long userId) {
        Coupon coupon = this.getOne(
                new LambdaQueryWrapper<Coupon>()
                        .eq(Coupon::getId, couponId)
                        .eq(Coupon::getUserId, userId)
                        .eq(Coupon::getStatus, CouponStatusEnum.UNUSED.getValue())
        );
        if (coupon == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "未找到可用优惠券");
        }
        if (coupon.getExpireTime() != null && coupon.getExpireTime().before(new Date())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "优惠券已过期");
        }
        SeckillActivity activity = seckillActivityMapper.selectById(coupon.getActivityId());
        String activityName = activity != null ? activity.getName() : "";
        return convertToVO(coupon, activityName);
    }

    // ====== private 工具方法 ======

    private CouponVO convertToVO(Coupon coupon, String activityName) {
        CouponVO vo = new CouponVO();
        vo.setId(coupon.getId());
        vo.setCouponNo(coupon.getCouponNo());
        vo.setFaceValue(coupon.getFaceValue());
        vo.setStatus(coupon.getStatus());
        CouponStatusEnum statusEnum = CouponStatusEnum.getEnumByValue(coupon.getStatus());
        vo.setStatusName(statusEnum != null ? statusEnum.getText() : coupon.getStatus());
        vo.setActivityName(activityName != null ? activityName : "");
        vo.setExpireTime(coupon.getExpireTime());
        vo.setUseTime(coupon.getUseTime());
        vo.setCreateTime(coupon.getCreateTime());
        return vo;
    }
}
