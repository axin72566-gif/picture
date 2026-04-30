package com.axin.picturebackend.controller;

import com.axin.picturebackend.common.BaseResponse;
import com.axin.picturebackend.common.ResultUtils;
import com.axin.picturebackend.exception.ErrorCode;
import com.axin.picturebackend.exception.ThrowUtils;
import com.axin.picturebackend.model.dto.comment.CommentAddRequest;
import com.axin.picturebackend.model.dto.comment.CommentQueryRequest;
import com.axin.picturebackend.model.entity.User;
import com.axin.picturebackend.model.vo.CommentVO;
import com.axin.picturebackend.service.CommentService;
import com.axin.picturebackend.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 图片评论控制器
 */
@RestController
@RequestMapping("/comment")
public class CommentController {

    @Resource
    private CommentService commentService;

    @Resource
    private UserService userService;

    /**
     * 发布评论（或回复评论）
     *
     * @param commentAddRequest 发布评论请求（pictureId、parentId、content）
     * @param request           HTTP 请求
     * @return 新评论的 ID
     */
    @PostMapping("/add")
    public BaseResponse<Long> addComment(@RequestBody CommentAddRequest commentAddRequest,
                                         HttpServletRequest request) {
        ThrowUtils.throwIf(commentAddRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(commentService.addComment(commentAddRequest, loginUser));
    }

    /**
     * 删除评论（本人或管理员，级联删除子评论）
     *
     * @param commentId 评论ID
     * @param request   HTTP 请求
     * @return 是否成功
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteComment(@RequestParam Long commentId,
                                               HttpServletRequest request) {
        ThrowUtils.throwIf(commentId == null || commentId <= 0, ErrorCode.PARAMS_ERROR, "评论ID不合法");
        User loginUser = userService.getLoginUser(request);
        commentService.deleteComment(commentId, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 查询图片评论树（一级评论分页 + 子孙评论全量）
     * <p>公共图片无需登录也可查看</p>
     *
     * @param commentQueryRequest 查询请求（pictureId、current、pageSize）
     * @param request             HTTP 请求
     * @return 树形评论列表
     */
    @PostMapping("/list")
    public BaseResponse<List<CommentVO>> listCommentTree(@RequestBody CommentQueryRequest commentQueryRequest,
                                                         HttpServletRequest request) {
        ThrowUtils.throwIf(commentQueryRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = tryGetLoginUser(request);
        return ResultUtils.success(commentService.listCommentTree(commentQueryRequest, loginUser));
    }

    /**
     * 尝试获取当前登录用户，未登录时返回 null（不抛出异常）
     */
    private User tryGetLoginUser(HttpServletRequest request) {
        try {
            return userService.getLoginUser(request);
        } catch (Exception ignored) {
            return null;
        }
    }
}
