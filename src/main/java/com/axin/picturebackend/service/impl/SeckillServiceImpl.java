package com.axin.picturebackend.service.impl;

import cn.hutool.core.util.IdUtil;
import com.axin.picturebackend.constant.RedisConstant;
import com.axin.picturebackend.exception.BusinessException;
import com.axin.picturebackend.exception.ErrorCode;
import com.axin.picturebackend.manager.seckill.SeckillMQMessage;
import com.axin.picturebackend.manager.seckill.SeckillMQProducer;
import com.axin.picturebackend.mapper.SeckillActivityMapper;
import com.axin.picturebackend.model.entity.SeckillActivity;
import com.axin.picturebackend.model.vo.SeckillActivityVO;
import com.axin.picturebackend.service.SeckillService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 秒杀 Service 实现
 *
 * <p>核心流程：
 * <ol>
 *   <li>Redis SETNX 用户限购校验（同一活动限购1张）</li>
 *   <li>Redis DECR 原子预扣库存</li>
 *   <li>扣减失败时回滚 Redis（INCR + DEL 限购key）</li>
 *   <li>预扣成功后异步投递 MQ，由 Consumer 完成落库</li>
 * </ol>
 */
@Slf4j
@Service
public class SeckillServiceImpl extends ServiceImpl<SeckillActivityMapper, SeckillActivity>
        implements SeckillService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private SeckillMQProducer seckillMQProducer;

    /**
     * 用户秒杀购买优惠券
     */
    @Override
    public String buyCoupon(Long userId, Long activityId) {
        if (userId == null || activityId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 1. 查询活动基本信息校验
        SeckillActivity activity = this.getById(activityId);
        if (activity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "活动不存在");
        }
        Date now = new Date();
        if (now.before(activity.getStartTime())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "活动尚未开始");
        }
        if (now.after(activity.getEndTime())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "活动已结束");
        }

        // 2. 用户限购校验：SETNX seckill:user:{actId}:{userId}
        String userKey = RedisConstant.SECKILL_USER + activityId + ":" + userId;
        // 有效期设置为活动结束时间之后的冗余时长（7天，防止活动刚结束时判断错误）
        long ttlSeconds = (activity.getEndTime().getTime() - System.currentTimeMillis()) / 1000 + 7 * 24 * 3600;
        Boolean isNew = stringRedisTemplate.opsForValue()
                .setIfAbsent(userKey, "1", Math.max(ttlSeconds, 60), TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(isNew)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "每人限购一张，您已参与过本次活动");
        }

        // 3. Redis 原子预扣库存
        String stockKey = RedisConstant.SECKILL_STOCK + activityId;
        Long remain = stringRedisTemplate.opsForValue().decrement(stockKey);

        if (remain == null || remain < 0) {
            // 库存不足：回滚 Redis 操作
            if (remain != null) {
                stringRedisTemplate.opsForValue().increment(stockKey);
            }
            stringRedisTemplate.delete(userKey);
            log.info("[Seckill] 库存不足 userId={}, activityId={}", userId, activityId);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "手慢了，优惠券已抢完！");
        }

        // 4. 预生成优惠券编号，发送 MQ 消息异步落库
        String couponNo = "CPN" + System.currentTimeMillis() + IdUtil.randomUUID().substring(0, 6).toUpperCase();
        SeckillMQMessage mqMessage = SeckillMQMessage.builder()
                .userId(userId)
                .activityId(activityId)
                .couponNo(couponNo)
                .faceValue(activity.getFaceValue())
                .timestamp(System.currentTimeMillis())
                .build();
        seckillMQProducer.sendSeckillMessage(mqMessage);

        log.info("[Seckill] 抢购成功 userId={}, activityId={}, couponNo={}, remainStock={}",
                userId, activityId, couponNo, remain);
        return "抢购成功！优惠券发放中，请稍候查看我的优惠券";
    }

    /**
     * 查询当前有效的秒杀活动列表
     */
    @Override
    public List<SeckillActivityVO> listActiveActivities() {
        Date now = new Date();
        List<SeckillActivity> activities = this.list(
                new LambdaQueryWrapper<SeckillActivity>()
                        .ge(SeckillActivity::getEndTime, now)
                        .orderByAsc(SeckillActivity::getStartTime)
        );
        List<SeckillActivityVO> result = new ArrayList<>();
        for (SeckillActivity activity : activities) {
            result.add(convertToVO(activity));
        }
        return result;
    }

    /**
     * 管理员初始化活动库存到 Redis
     * 服务重启或新增活动后调用此接口将库存同步到 Redis
     */
    @Override
    public void initStockToRedis(Long activityId) {
        SeckillActivity activity = this.getById(activityId);
        if (activity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "活动不存在");
        }
        String stockKey = RedisConstant.SECKILL_STOCK + activityId;
        // 计算 key TTL（活动结束后自动过期，冗余1天）
        long ttlSeconds = (activity.getEndTime().getTime() - System.currentTimeMillis()) / 1000 + 86400;
        stringRedisTemplate.opsForValue().set(
                stockKey,
                String.valueOf(activity.getRemainStock()),
                Math.max(ttlSeconds, 60),
                TimeUnit.SECONDS
        );
        log.info("[Seckill] 库存初始化到Redis activityId={}, stock={}", activityId, activity.getRemainStock());
    }

    // ====== private 工具方法 ======

    private SeckillActivityVO convertToVO(SeckillActivity activity) {
        SeckillActivityVO vo = new SeckillActivityVO();
        vo.setId(activity.getId());
        vo.setName(activity.getName());
        vo.setTotalStock(activity.getTotalStock());
        vo.setRemainStock(activity.getRemainStock());
        vo.setFaceValue(activity.getFaceValue());
        vo.setSalePrice(activity.getSalePrice());
        vo.setStartTime(activity.getStartTime());
        vo.setEndTime(activity.getEndTime());

        Date now = new Date();
        if (now.before(activity.getStartTime())) {
            vo.setStatusName("未开始");
        } else if (now.after(activity.getEndTime())) {
            vo.setStatusName("已结束");
        } else if (activity.getRemainStock() <= 0) {
            vo.setStatusName("已售罄");
        } else {
            vo.setStatusName("进行中");
        }
        return vo;
    }
}
