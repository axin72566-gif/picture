package com.axin.picturebackend.service.impl;

import com.axin.picturebackend.constant.RedisConstant;
import com.axin.picturebackend.constant.UserConstant;
import com.axin.picturebackend.exception.BusinessException;
import com.axin.picturebackend.exception.ErrorCode;
import com.axin.picturebackend.exception.ThrowUtils;
import com.axin.picturebackend.mapper.CommentMapper;
import com.axin.picturebackend.model.dto.comment.CommentAddRequest;
import com.axin.picturebackend.model.dto.comment.CommentQueryRequest;
import com.axin.picturebackend.model.Enum.NoticeTypeEnum;
import com.axin.picturebackend.model.entity.Comment;
import com.axin.picturebackend.model.entity.Picture;
import com.axin.picturebackend.model.entity.SpaceUser;
import com.axin.picturebackend.model.entity.User;
import com.axin.picturebackend.model.vo.CommentVO;
import com.axin.picturebackend.model.vo.UserVO;
import com.axin.picturebackend.service.CommentService;
import com.axin.picturebackend.service.PictureService;
import com.axin.picturebackend.service.SpaceUserService;
import com.axin.picturebackend.service.SysNoticeService;
import com.axin.picturebackend.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 图片评论 Service 实现
 */
@Service
@Slf4j
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment>
        implements CommentService {

    @Lazy
    @Resource
    private PictureService pictureService;

    @Resource
    private SpaceUserService spaceUserService;

    @Resource
    private UserService userService;

    @Lazy
    @Resource
    private SysNoticeService sysNoticeService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    // ==================== 发布评论 ====================

    @Override
    public Long addComment(CommentAddRequest commentAddRequest, User loginUser) {
        // 1. 参数校验
        ThrowUtils.throwIf(commentAddRequest == null, ErrorCode.PARAMS_ERROR);
        Long pictureId = commentAddRequest.getPictureId();
        String content = commentAddRequest.getContent();
        ThrowUtils.throwIf(pictureId == null || pictureId <= 0, ErrorCode.PARAMS_ERROR, "图片ID不合法");
        ThrowUtils.throwIf(StringUtils.isBlank(content), ErrorCode.PARAMS_ERROR, "评论内容不能为空");
        ThrowUtils.throwIf(content.length() > 1000, ErrorCode.PARAMS_ERROR, "评论内容不能超过1000字");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);

        // 2. 查询图片，校验权限
        Picture picture = pictureService.getById(pictureId);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        checkCommentPermission(picture, loginUser);

        // 3. 父评论校验
        Long parentId = commentAddRequest.getParentId();
        if (parentId != null && parentId > 0) {
            Comment parentComment = this.getById(parentId);
            ThrowUtils.throwIf(parentComment == null, ErrorCode.NOT_FOUND_ERROR, "父评论不存在");
            ThrowUtils.throwIf(!parentComment.getPictureId().equals(pictureId),
                    ErrorCode.PARAMS_ERROR, "父评论不属于该图片");
        } else {
            parentId = 0L;
        }

        // 4. 保存评论
        Comment comment = new Comment();
        comment.setPictureId(pictureId);
        comment.setUserId(loginUser.getId());
        comment.setParentId(parentId);
        comment.setContent(content);
        boolean saved = this.save(comment);
        ThrowUtils.throwIf(!saved, ErrorCode.OPERATION_ERROR, "发布评论失败");

        // 今日评论计数 +1
        stringRedisTemplate.opsForValue().increment(RedisConstant.PICTURE_COMMENT_COUNT + pictureId, 1);

        // 5. 发送评论通知给图片作者（自己评论自己不通知）
        try {
            if (!picture.getUserId().equals(loginUser.getId())) {
                String commenterName = loginUser.getUserName() == null ? loginUser.getUserAccount() : loginUser.getUserName();
                String pictureName = picture.getName() == null ? "" : picture.getName();
                String noticeContent = String.format("用户「%s」评论了你的图片「%s」：%s",
                        commenterName, pictureName,
                        content.length() > 50 ? content.substring(0, 50) + "..." : content);
                sysNoticeService.sendNotice(picture.getUserId(), "收到新评论", noticeContent, pictureId,
                        NoticeTypeEnum.COMMENT.getValue());
            }
        } catch (Exception e) {
            log.warn("发送评论通知失败, pictureId={}, error={}", pictureId, e.getMessage());
        }

        return comment.getId();
    }

    // ==================== 删除评论 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long commentId, User loginUser) {
        ThrowUtils.throwIf(commentId == null || commentId <= 0, ErrorCode.PARAMS_ERROR, "评论ID不合法");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);

        // 查询评论
        Comment comment = this.getById(commentId);
        ThrowUtils.throwIf(comment == null, ErrorCode.NOT_FOUND_ERROR, "评论不存在");

        // 仅本人或管理员可删除
        boolean isAdmin = UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole());
        boolean isOwner = loginUser.getId().equals(comment.getUserId());
        ThrowUtils.throwIf(!isAdmin && !isOwner, ErrorCode.NO_AUTH_ERROR, "无权限删除该评论");

        // 级联逻辑删除：收集该评论及所有子孙评论 ID
        List<Long> toDeleteIds = collectDescendantIds(commentId);
        toDeleteIds.add(commentId);

        // 批量逻辑删除
        LambdaUpdateWrapper<Comment> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.in(Comment::getId, toDeleteIds)
                .set(Comment::getIsDelete, 1);
        this.update(updateWrapper);

        // 今日评论计数减少（含子孙评论）
        long deletedCount = toDeleteIds.size() + 1;
        stringRedisTemplate.opsForValue().decrement(
                RedisConstant.PICTURE_COMMENT_COUNT + comment.getPictureId(), deletedCount);
    }

    /**
     * 递归收集所有子孙评论 ID
     */
    private List<Long> collectDescendantIds(Long parentId) {
        List<Long> result = new ArrayList<>();
        LambdaQueryWrapper<Comment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Comment::getParentId, parentId);
        List<Comment> children = this.list(queryWrapper);
        for (Comment child : children) {
            result.add(child.getId());
            result.addAll(collectDescendantIds(child.getId()));
        }
        return result;
    }

    // ==================== 查询评论树 ====================

    @Override
    public List<CommentVO> listCommentTree(CommentQueryRequest commentQueryRequest, User loginUser) {
        ThrowUtils.throwIf(commentQueryRequest == null, ErrorCode.PARAMS_ERROR);
        Long pictureId = commentQueryRequest.getPictureId();
        ThrowUtils.throwIf(pictureId == null || pictureId <= 0, ErrorCode.PARAMS_ERROR, "图片ID不合法");

        // 查询图片，校验查看权限（空间图片需登录且是成员）
        Picture picture = pictureService.getById(pictureId);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        if (picture.getSpaceId() != null) {
            // 空间图片：需要登录
            ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
            checkCommentPermission(picture, loginUser);
        }

        // 一次性查出该图片所有评论（含所有层级）
        LambdaQueryWrapper<Comment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Comment::getPictureId, pictureId)
                .orderByAsc(Comment::getCreateTime);
        List<Comment> allComments = this.list(queryWrapper);

        if (CollectionUtils.isEmpty(allComments)) {
            return Collections.emptyList();
        }

        // 批量查询用户信息
        Set<Long> userIds = allComments.stream()
                .map(Comment::getUserId)
                .collect(Collectors.toSet());
        List<User> users = userService.listByIds(userIds);
        Map<Long, UserVO> userVoMap = users.stream()
                .collect(Collectors.toMap(User::getId, userService::getUserVO));

        // 转换为 VO
        Map<Long, CommentVO> voMap = new LinkedHashMap<>();
        for (Comment comment : allComments) {
            CommentVO vo = CommentVO.objToVo(comment);
            vo.setUser(userVoMap.get(comment.getUserId()));
            voMap.put(comment.getId(), vo);
        }

        // 内存组装树形结构
        List<CommentVO> roots = new ArrayList<>();
        for (CommentVO vo : voMap.values()) {
            Long pId = vo.getParentId();
            if (pId == null || pId == 0L) {
                roots.add(vo);
            } else {
                CommentVO parent = voMap.get(pId);
                if (parent != null) {
                    parent.getChildren().add(vo);
                } else {
                    // 父评论已被删除，作为一级评论处理
                    roots.add(vo);
                }
            }
        }

        // 分页：对一级评论分页
        int current = commentQueryRequest.getCurrent();
        int pageSize = commentQueryRequest.getPageSize();
        int fromIndex = (current - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, roots.size());
        if (fromIndex >= roots.size()) {
            return Collections.emptyList();
        }
        return roots.subList(fromIndex, toIndex);
    }

    // ==================== 权限校验 ====================

    /**
     * 校验用户是否有权限操作该图片的评论
     * - 公开图片（spaceId=null）：登录即可
     * - 空间图片：必须是空间成员
     */
    private void checkCommentPermission(Picture picture, User loginUser) {
        Long spaceId = picture.getSpaceId();
        if (spaceId == null) {
            // 公开图片，登录即可
            return;
        }
        // 空间图片，校验是否为空间成员
        LambdaQueryWrapper<SpaceUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SpaceUser::getSpaceId, spaceId)
                .eq(SpaceUser::getUserId, loginUser.getId());
        long count = spaceUserService.count(queryWrapper);
        if (count == 0) {
            // 管理员也可访问
            if (!UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "您不是该空间成员，无权评论");
            }
        }
    }

    // ==================== 评论计数 ====================

    @Override
    public long getCommentCount(Long pictureId) {
        String key = RedisConstant.PICTURE_COMMENT_COUNT + pictureId;
        String countStr = stringRedisTemplate.opsForValue().get(key);
        if (countStr != null) {
            try {
                return Long.parseLong(countStr);
            } catch (NumberFormatException e) {
                log.warn("评论计数格式错误, pictureId={}, value={}", pictureId, countStr);
            }
        }
        return this.count(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getPictureId, pictureId));
    }
}
