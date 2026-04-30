package com.axin.picturebackend.service.impl;

import com.axin.picturebackend.constant.RedisConstant;
import com.axin.picturebackend.exception.BusinessException;
import com.axin.picturebackend.exception.ErrorCode;
import com.axin.picturebackend.mapper.PictureLikeMapper;
import com.axin.picturebackend.model.entity.Picture;
import com.axin.picturebackend.model.entity.PictureLike;
import com.axin.picturebackend.model.entity.User;
import com.axin.picturebackend.service.PictureLikeService;
import com.axin.picturebackend.service.PictureService;
import com.axin.picturebackend.service.SysNoticeService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 图片点赞 Service 实现
 */
@Service
@Slf4j
public class PictureLikeServiceImpl extends ServiceImpl<PictureLikeMapper, PictureLike>
        implements PictureLikeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Lazy
    @Resource
    private PictureService pictureService;

    @Lazy
    @Resource
    private SysNoticeService sysNoticeService;

    // ==================== 点赞 / 取消点赞 ====================

    @Override
    public boolean doLike(Long pictureId, User loginUser) {
        if (pictureId == null || pictureId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片ID不合法");
        }
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "未登录");
        }
        Long userId = loginUser.getId();

        // 查询点赞关系是否存在
        QueryWrapper<PictureLike> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        queryWrapper.eq("pictureId", pictureId);
        PictureLike existLike = this.getOne(queryWrapper);

        if (existLike != null) {
            // 已点赞 → 取消点赞
            boolean removed = this.remove(queryWrapper);
            if (!removed) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "取消点赞失败");
            }
            // Redis DECR（不低于0）
            String redisKey = RedisConstant.PICTURE_LIKE_COUNT + pictureId;
            Long currentCount = stringRedisTemplate.opsForValue().decrement(redisKey);
            if (currentCount != null && currentCount < 0) {
                stringRedisTemplate.opsForValue().set(redisKey, "0");
            }
            return false;
        } else {
            // 未点赞 → 点赞
            PictureLike pictureLike = new PictureLike();
            pictureLike.setUserId(userId);
            pictureLike.setPictureId(pictureId);
            pictureLike.setCreateTime(new Date());
            boolean saved = this.save(pictureLike);
            if (!saved) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "点赞失败");
            }
            // Redis INCR
            stringRedisTemplate.opsForValue().increment(RedisConstant.PICTURE_LIKE_COUNT + pictureId);
            // 发送点赞通知给图片作者（自己给自己点赞不通知）
            try {
                Picture picture = pictureService.getById(pictureId);
                if (picture != null && !picture.getUserId().equals(userId)) {
                    String noticeContent = String.format("用户「%s」点赞了你的图片「%s」",
                            loginUser.getUserName() == null ? loginUser.getUserAccount() : loginUser.getUserName(),
                            picture.getName() == null ? "" : picture.getName());
                    sysNoticeService.sendNotice(picture.getUserId(), "收到新点赞", noticeContent, pictureId);
                }
            } catch (Exception e) {
                log.warn("发送点赞通知失败, pictureId={}, error={}", pictureId, e.getMessage());
            }
            return true;
        }
    }

    // ==================== 查询点赞状态 ====================

    @Override
    public boolean isLiked(Long pictureId, Long userId) {
        if (pictureId == null || userId == null) {
            return false;
        }
        QueryWrapper<PictureLike> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        queryWrapper.eq("pictureId", pictureId);
        return this.count(queryWrapper) > 0;
    }

    @Override
    public Set<Long> batchIsLiked(List<Long> pictureIds, Long userId) {
        if (CollectionUtils.isEmpty(pictureIds) || userId == null) {
            return Collections.emptySet();
        }
        List<Long> likedIds = baseMapper.selectLikedPictureIds(userId, pictureIds);
        return new HashSet<>(likedIds);
    }

    // ==================== 获取点赞数 ====================

    @Override
    public long getLikeCount(Long pictureId, Long dbCount) {
        if (pictureId == null) {
            return dbCount == null ? 0 : dbCount;
        }
        String countStr = stringRedisTemplate.opsForValue().get(RedisConstant.PICTURE_LIKE_COUNT + pictureId);
        if (countStr != null) {
            try {
                return Long.parseLong(countStr);
            } catch (NumberFormatException e) {
                log.warn("图片点赞数格式错误, pictureId={}, value={}", pictureId, countStr);
            }
        }
        return dbCount == null ? 0 : dbCount;
    }

    // ==================== 定时任务：写库 ====================

    /**
     * 每天凌晨4点将 Redis 中的点赞数写入 MySQL
     */
    @Scheduled(cron = "0 0 4 * * ?")
    public void writeLikeCount() {
        Set<String> likeCountKeys = stringRedisTemplate.keys(RedisConstant.PICTURE_LIKE_COUNT + "*");
        if (CollectionUtils.isEmpty(likeCountKeys)) {
            log.info("暂无图片点赞计数需要同步");
            return;
        }

        List<Long> pictureIds = likeCountKeys.stream()
                .map(this::parsePictureIdFromKey)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        for (Long pictureId : pictureIds) {
            String countStr = stringRedisTemplate.opsForValue().get(RedisConstant.PICTURE_LIKE_COUNT + pictureId);
            if (countStr == null) {
                continue;
            }
            try {
                long likeCount = Long.parseLong(countStr);
                Picture picture = new Picture();
                picture.setId(pictureId);
                picture.setLikeCount(likeCount);
                boolean updateSuccess = pictureService.updateById(picture);
                if (!updateSuccess) {
                    log.warn("更新图片点赞数失败, pictureId={}", pictureId);
                }
            } catch (NumberFormatException e) {
                log.error("图片点赞数格式错误, pictureId={}, value={}", pictureId, countStr);
            }
        }

        // 写库完成后删除 Redis key
        Long deletedCount = stringRedisTemplate.delete(likeCountKeys);
        log.info("点赞数写库完成，共同步 {} 条，删除 Redis key {} 个", pictureIds.size(), deletedCount);
    }

    /**
     * 从 Redis Key 中解析图片ID，格式：picture:like:{pictureId}
     */
    private Long parsePictureIdFromKey(String redisKey) {
        try {
            String[] parts = redisKey.split(":");
            if (parts.length < 3) {
                return null;
            }
            return Long.parseLong(parts[2]);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
