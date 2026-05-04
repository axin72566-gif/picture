package com.axin.picturebackend.service.impl;

import com.axin.picturebackend.constant.FeedConstant;
import com.axin.picturebackend.constant.RedisConstant;
import com.axin.picturebackend.model.dto.feed.FeedQueryRequest;
import com.axin.picturebackend.model.entity.Picture;
import com.axin.picturebackend.model.entity.User;
import com.axin.picturebackend.model.entity.UserFollow;
import com.axin.picturebackend.model.vo.FeedPageResult;
import com.axin.picturebackend.model.vo.FeedVO;
import com.axin.picturebackend.service.FeedService;
import com.axin.picturebackend.service.PictureService;
import com.axin.picturebackend.service.UserFollowService;
import com.axin.picturebackend.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Feed 流服务实现
 *
 * <p><b>合并策略（推拉结合）：</b>
 * <ol>
 *   <li>从用户收件箱 ZSet（feed:inbox:{userId}）读取普通关注者推来的动态</li>
 *   <li>从该用户关注的所有大V发件箱 ZSet（feed:outbox:{bigVId}）读取大V动态</li>
 *   <li>内存合并 → 按 score（时间戳）倒序 → 游标截断 → 去重 → 取 size 条</li>
 *   <li>批量查询 Picture 详情，批量查询 User 信息，组装 FeedVO</li>
 * </ol>
 *
 * <p><b>游标分页：</b>使用时间戳作为游标，避免翻页数据错位。
 * 首次请求 cursor=null（从最新开始），后续传上次返回的 nextCursor。
 */
@Slf4j
@Service
public class FeedServiceImpl implements FeedService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private UserFollowService userFollowService;

    @Resource
    private PictureService pictureService;

    @Resource
    private UserService userService;

    @Override
    public FeedPageResult getFeedList(FeedQueryRequest request, User loginUser) {
        Long userId = loginUser.getId();

        // 参数校验与默认值
        int size = (request.getSize() == null || request.getSize() <= 0)
                ? FeedConstant.FEED_DEFAULT_PAGE_SIZE
                : Math.min(request.getSize(), FeedConstant.FEED_MAX_PAGE_SIZE);

        // 游标：null 表示首次请求（从最新开始），传值则表示从该时间戳往前翻页
        double maxScore = request.getCursor() == null
                ? Double.MAX_VALUE
                : (double) request.getCursor() - 1;  // -1 确保游标点不重复

        // ====== 1. 读取收件箱（普通用户推送过来的） ======
        Set<ZSetOperations.TypedTuple<String>> inboxSet = getZSetRange(
                RedisConstant.FEED_INBOX + userId, maxScore);

        // ====== 2. 读取关注大V的发件箱（拉模式合并） ======
        List<Long> bigVIds = getBigVFollowingIds(userId);
        Map<Long, Set<ZSetOperations.TypedTuple<String>>> bigVOutboxMap = new HashMap<>();
        for (Long bigVId : bigVIds) {
            Set<ZSetOperations.TypedTuple<String>> outboxSet = getZSetRange(
                    RedisConstant.FEED_OUTBOX + bigVId, maxScore);
            if (!CollectionUtils.isEmpty(outboxSet)) {
                bigVOutboxMap.put(bigVId, outboxSet);
            }
        }

        // ====== 3. 内存合并所有 TypedTuple，按 score 倒序 ======
        // 使用 Map<pictureId, score> 去重（同一 pictureId 保留最新 score）
        Map<Long, Double> mergedMap = new LinkedHashMap<>();
        collectTuples(mergedMap, inboxSet);
        for (Set<ZSetOperations.TypedTuple<String>> outbox : bigVOutboxMap.values()) {
            collectTuples(mergedMap, outbox);
        }

        // 按 score 倒序排列
        List<Map.Entry<Long, Double>> sortedEntries = mergedMap.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .collect(Collectors.toList());

        // 截取当前页数据
        List<Map.Entry<Long, Double>> pageEntries = sortedEntries.stream()
                .limit(size + 1L)   // 多取1条判断 hasMore
                .collect(Collectors.toList());

        boolean hasMore = pageEntries.size() > size;
        if (hasMore) {
            pageEntries = pageEntries.subList(0, size);
        }

        if (pageEntries.isEmpty()) {
            return FeedPageResult.builder()
                    .records(Collections.emptyList())
                    .nextCursor(null)
                    .hasMore(false)
                    .build();
        }

        // ====== 4. 批量查图片（防 N+1） ======
        List<Long> pictureIds = pageEntries.stream()
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        List<Picture> pictures = pictureService.listByIds(pictureIds);

        // 过滤已删除或审核未通过的图片，建立 id -> Picture 映射
        Map<Long, Picture> pictureMap = pictures.stream()
                .filter(p -> p != null && p.getIsDelete() == 0)
                .collect(Collectors.toMap(Picture::getId, Function.identity()));

        // ====== 5. 批量查作者信息（防 N+1） ======
        Set<Long> uploaderIds = pictureMap.values().stream()
                .map(Picture::getUserId)
                .collect(Collectors.toSet());
        Map<Long, User> userMap = Collections.emptyMap();
        if (!uploaderIds.isEmpty()) {
            List<User> users = userService.listByIds(uploaderIds);
            userMap = users.stream()
                    .collect(Collectors.toMap(User::getId, Function.identity()));
        }

        // ====== 6. 组装 FeedVO ======
        List<FeedVO> records = new ArrayList<>();
        for (Map.Entry<Long, Double> entry : pageEntries) {
            Long picId = entry.getKey();
            Picture picture = pictureMap.get(picId);
            if (picture == null) {
                // 图片已删除，跳过（不占用分页位置会导致条数少，可接受）
                continue;
            }
            FeedVO vo = new FeedVO();
            vo.setPictureId(picture.getId());
            vo.setThumbnailUrl(picture.getThumbnailUrl());
            vo.setUrl(picture.getUrl());
            vo.setPicName(picture.getName());
            vo.setCreateTime(entry.getValue().longValue());

            User author = userMap.get(picture.getUserId());
            if (author != null) {
                vo.setAuthorId(author.getId());
                vo.setAuthorName(author.getUserName() != null ? author.getUserName() : author.getUserAccount());
                vo.setAuthorAvatar(author.getUserAvatar());
            }
            records.add(vo);
        }

        // ====== 7. 计算 nextCursor（最后一条的时间戳） ======
        Long nextCursor = null;
        if (hasMore && !pageEntries.isEmpty()) {
            nextCursor = pageEntries.get(pageEntries.size() - 1).getValue().longValue();
        }

        log.debug("[Feed] userId={} 查询完成, 返回{}条, hasMore={}, nextCursor={}",
                userId, records.size(), hasMore, nextCursor);

        return FeedPageResult.builder()
                .records(records)
                .nextCursor(nextCursor)
                .hasMore(hasMore)
                .build();
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 从 Redis ZSet 中按 score 倒序读取 [0, maxScore] 范围的数据
     * 最多读取 FEED_MAX_PAGE_SIZE + 1 条，避免一次性读取过多
     */
    private Set<ZSetOperations.TypedTuple<String>> getZSetRange(String key, double maxScore) {
        try {
            return stringRedisTemplate.opsForZSet()
                    .reverseRangeByScoreWithScores(key, 0, maxScore,
                            0, FeedConstant.FEED_MAX_PAGE_SIZE + 1L);
        } catch (Exception e) {
            log.warn("[Feed] 读取 Redis ZSet 失败 key={}, error={}", key, e.getMessage());
            return Collections.emptySet();
        }
    }

    /**
     * 将 TypedTuple Set 中的数据合并到 mergedMap（key=pictureId, value=score）
     * 同一 pictureId 保留较大的 score（更新的时间）
     */
    private void collectTuples(Map<Long, Double> mergedMap,
                                Set<ZSetOperations.TypedTuple<String>> tupleSet) {
        if (CollectionUtils.isEmpty(tupleSet)) {
            return;
        }
        for (ZSetOperations.TypedTuple<String> tuple : tupleSet) {
            if (tuple.getValue() == null || tuple.getScore() == null) {
                continue;
            }
            try {
                Long picId = Long.parseLong(tuple.getValue());
                mergedMap.merge(picId, tuple.getScore(), Math::max);
            } catch (NumberFormatException e) {
                log.warn("[Feed] 解析 pictureId 失败: {}", tuple.getValue());
            }
        }
    }

    /**
     * 获取当前用户关注的大V用户ID列表
     * 大V定义：粉丝数 >= {@link FeedConstant#BIG_V_THRESHOLD}
     *
     * <p>粉丝数优先从 Redis 缓存读取（user:fans:count:{userId}），缓存未命中时查库
     */
    private List<Long> getBigVFollowingIds(Long userId) {
        // 获取该用户所有关注对象
        LambdaQueryWrapper<UserFollow> wrapper = new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getUserId, userId)
                .select(UserFollow::getFollowUserId);
        List<UserFollow> followList = userFollowService.list(wrapper);
        if (CollectionUtils.isEmpty(followList)) {
            return Collections.emptyList();
        }

        List<Long> bigVIds = new ArrayList<>();
        for (UserFollow follow : followList) {
            Long followUserId = follow.getFollowUserId();
            long fansCount = getFansCountFromCache(followUserId);
            if (fansCount >= FeedConstant.BIG_V_THRESHOLD) {
                bigVIds.add(followUserId);
            }
        }
        return bigVIds;
    }

    /**
     * 从 Redis 缓存获取粉丝数，缓存未命中时查库并回写缓存
     */
    private long getFansCountFromCache(Long targetUserId) {
        String cacheKey = RedisConstant.USER_FANS_COUNT + targetUserId;
        String cachedVal = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cachedVal != null) {
            try {
                return Long.parseLong(cachedVal);
            } catch (NumberFormatException ignored) {
            }
        }
        // 缓存未命中，查库
        long count = userFollowService.countFollowers(targetUserId);
        // 回写缓存，TTL 10分钟（粉丝数允许轻微不一致）
        stringRedisTemplate.opsForValue().set(cacheKey, String.valueOf(count),
                10, java.util.concurrent.TimeUnit.MINUTES);
        return count;
    }
}
