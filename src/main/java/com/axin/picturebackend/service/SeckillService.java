package com.axin.picturebackend.service;

import com.axin.picturebackend.model.entity.SeckillActivity;
import com.axin.picturebackend.model.vo.SeckillActivityVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 秒杀 Service 接口
 */
public interface SeckillService extends IService<SeckillActivity> {

    /**
     * 用户秒杀购买优惠券
     *
     * @param userId     当前登录用户ID
     * @param activityId 秒杀活动ID
     * @return 抢购结果描述
     */
    String buyCoupon(Long userId, Long activityId);

    /**
     * 查询当前有效的秒杀活动列表
     *
     * @return 活动列表
     */
    List<SeckillActivityVO> listActiveActivities();

    /**
     * 管理员初始化活动库存到 Redis（新增活动或服务重启后调用）
     *
     * @param activityId 活动ID
     */
    void initStockToRedis(Long activityId);
}
