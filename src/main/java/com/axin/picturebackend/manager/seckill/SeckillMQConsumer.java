package com.axin.picturebackend.manager.seckill;

import com.axin.picturebackend.config.SeckillRocketMQConfig;
import com.axin.picturebackend.mapper.CouponMapper;
import com.axin.picturebackend.mapper.SeckillActivityMapper;
import com.axin.picturebackend.model.Enum.CouponStatusEnum;
import com.axin.picturebackend.model.Enum.NoticeTypeEnum;
import com.axin.picturebackend.model.entity.Coupon;
import com.axin.picturebackend.model.entity.SeckillActivity;
import com.axin.picturebackend.service.SysNoticeService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Calendar;
import java.util.Date;

/**
 * 秒杀 RocketMQ 消息消费者
 *
 * <p>消费秒杀成功消息，完成以下操作：
 * <ol>
 *   <li>幂等校验（couponNo 已存在则跳过）</li>
 *   <li>写入 coupon 表（status=UNUSED，过期时间=发放后30天）</li>
 *   <li>扣减 seckill_activity.remainStock（WHERE remainStock > 0 保证不超卖）</li>
 *   <li>通过 SysNoticeService 发送系统通知告知用户</li>
 * </ol>
 */
@Slf4j
@Component
@RocketMQMessageListener(
        topic = SeckillRocketMQConfig.SECKILL_COUPON_TOPIC,
        selectorExpression = SeckillRocketMQConfig.SECKILL_COUPON_TAG,
        consumerGroup = SeckillRocketMQConfig.SECKILL_CONSUMER_GROUP,
        consumeThreadNumber = 4
)
public class SeckillMQConsumer implements RocketMQListener<SeckillMQMessage> {

    @Resource
    private CouponMapper couponMapper;

    @Resource
    private SeckillActivityMapper seckillActivityMapper;

    @Lazy
    @Resource
    private SysNoticeService sysNoticeService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onMessage(SeckillMQMessage message) {
        Long userId = message.getUserId();
        Long activityId = message.getActivityId();
        String couponNo = message.getCouponNo();

        log.info("[SeckillMQ] 消费消息 userId={}, activityId={}, couponNo={}", userId, activityId, couponNo);

        // 1. 幂等校验：若该 couponNo 已存在，跳过处理（消息重投场景）
        Long existCount = couponMapper.selectCount(
                new LambdaQueryWrapper<Coupon>().eq(Coupon::getCouponNo, couponNo)
        );
        if (existCount > 0) {
            log.warn("[SeckillMQ] 幂等拦截，couponNo={} 已存在，跳过", couponNo);
            return;
        }

        // 2. 查询活动信息
        SeckillActivity activity = seckillActivityMapper.selectById(activityId);
        if (activity == null) {
            log.error("[SeckillMQ] 活动不存在 activityId={}", activityId);
            return;
        }

        // 3. 写入 coupon 表（过期时间默认30天后）
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, 30);
        Date expireTime = calendar.getTime();

        Coupon coupon = new Coupon();
        coupon.setUserId(userId);
        coupon.setActivityId(activityId);
        coupon.setCouponNo(couponNo);
        coupon.setFaceValue(message.getFaceValue());
        coupon.setStatus(CouponStatusEnum.UNUSED.getValue());
        coupon.setExpireTime(expireTime);
        couponMapper.insert(coupon);

        // 4. 扣减数据库库存（WHERE remainStock > 0 防超卖兜底）
        int updated = seckillActivityMapper.update(null,
                new LambdaUpdateWrapper<SeckillActivity>()
                        .eq(SeckillActivity::getId, activityId)
                        .gt(SeckillActivity::getRemainStock, 0)
                        .setSql("remainStock = remainStock - 1")
        );
        if (updated == 0) {
            log.warn("[SeckillMQ] 数据库库存已耗尽，activityId={}", activityId);
        }

        // 5. 发送系统通知（最佳努力，失败不影响主流程）
        try {
            sysNoticeService.sendNotice(
                    userId,
                    "优惠券已到账",
                    String.format("您成功抢购的「%s」优惠券（面值 %.2f 元）已发放到账，请尽快使用！",
                            activity.getName(), message.getFaceValue()),
                    coupon.getId(),
                    NoticeTypeEnum.SYSTEM.getValue()
            );
        } catch (Exception e) {
            log.error("[SeckillMQ] 发送系统通知失败 userId={}, couponId={}", userId, coupon.getId(), e);
        }

        log.info("[SeckillMQ] 消费完成 userId={}, couponId={}, couponNo={}", userId, coupon.getId(), couponNo);
    }
}
