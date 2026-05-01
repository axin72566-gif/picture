package com.axin.picturebackend.controller;

import com.axin.picturebackend.annotation.RoleCheck;
import com.axin.picturebackend.common.BaseResponse;
import com.axin.picturebackend.common.DeleteRequest;
import com.axin.picturebackend.common.ResultUtils;
import com.axin.picturebackend.constant.UserConstant;
import com.axin.picturebackend.exception.ErrorCode;
import com.axin.picturebackend.exception.ThrowUtils;
import com.axin.picturebackend.manager.auth.SaSpaceCheckPermission;
import com.axin.picturebackend.manager.auth.model.SpaceUserPermissionConstant;
import com.axin.picturebackend.model.dto.picture.*;
import com.axin.picturebackend.model.entity.Picture;
import com.axin.picturebackend.model.entity.User;
import com.axin.picturebackend.model.vo.PictureVO;
import com.axin.picturebackend.service.PictureLikeService;
import com.axin.picturebackend.service.PictureService;
import com.axin.picturebackend.service.UserService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;

/**
 * 图片控制器
 */
@RestController
@RequestMapping("/picture")
public class PictureController {

    private static final List<String> DEFAULT_TAG_LIST =
            Arrays.asList("热门", "搞笑", "生活", "高清", "艺术", "校园", "背景", "简历", "创意");

    private static final List<String> DEFAULT_CATEGORY_LIST =
            Arrays.asList("模板", "电商", "表情包", "素材", "海报");

    @Resource
    private PictureService pictureService;

    @Resource
    private UserService userService;

    @Resource
    private PictureLikeService pictureLikeService;

    // ==================== 上传 ====================

    /**
     * 上传图片（文件方式）
     */
    @PostMapping("/upload")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)
    public BaseResponse<PictureVO> uploadPicture(@ModelAttribute PictureUploadRequest pictureUploadRequest,
                                                 @RequestPart("file") MultipartFile multipartFile,
                                                 HttpServletRequest request) {
        ThrowUtils.throwIf(pictureUploadRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(pictureService.uploadPicture(pictureUploadRequest, multipartFile, loginUser));
    }

    /**
     * 上传图片（URL方式）
     */
    @PostMapping("/upload/url")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)
    public BaseResponse<PictureVO> uploadPictureByUrl(@RequestBody PictureUploadRequest pictureUploadRequest,
                                                      HttpServletRequest request) {
        ThrowUtils.throwIf(pictureUploadRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(pictureService.uploadPicture(pictureUploadRequest, pictureUploadRequest.getFileUrl(), loginUser));
    }

    /**
     * 批量抓取上传图片（管理员）
     */
    @PostMapping("/upload/batch")
    @RoleCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Integer> uploadPictureByBatch(@RequestBody PictureUploadByBatchRequest pictureUploadByBatchRequest,
                                                      HttpServletRequest request) {
        ThrowUtils.throwIf(pictureUploadByBatchRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        int uploadCount = pictureService.uploadPictureByBatch(pictureUploadByBatchRequest, loginUser);
        return ResultUtils.success(uploadCount);
    }

    // ==================== 删除 / 编辑 / 更新 ====================

    /**
     * 删除图片
     */
    @PostMapping("/delete")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_DELETE)
    public BaseResponse<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest,
                                               HttpServletRequest request) {
        ThrowUtils.throwIf(deleteRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(pictureService.deletePicture(deleteRequest, loginUser));
    }

    /**
     * 编辑图片（普通用户）
     */
    @PostMapping("/edit")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<PictureVO> editPicture(@RequestBody PictureEditRequest pictureEditRequest,
                                               HttpServletRequest request) {
        ThrowUtils.throwIf(pictureEditRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(pictureService.editPicture(pictureEditRequest, loginUser));
    }

    /**
     * 更新图片（管理员）
     */
    @PostMapping("/update")
    @RoleCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<PictureVO> updatePicture(@RequestBody PictureUpdateRequest pictureUpdateRequest,
                                                 HttpServletRequest request) {
        ThrowUtils.throwIf(pictureUpdateRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(pictureService.updatePicture(pictureUpdateRequest, loginUser));
    }

    // ==================== 查询 ====================

    /**
     * 获取图片VO（登录用户）
     */
    @GetMapping("/get/vo")
    public BaseResponse<PictureVO> getPictureVOById(@RequestParam Long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(pictureService.getPictureVOById(id, loginUser));
    }

    /**
     * 分页获取图片VO列表（登录用户）
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<PictureVO>> listPictureVO(@RequestBody PictureQueryRequest pictureQueryRequest,
                                                       HttpServletRequest request) {
        ThrowUtils.throwIf(pictureQueryRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(pictureService.listPictureVO(pictureQueryRequest, loginUser));
    }

    /**
     * 获取图片原始信息（管理员）
     */
    @GetMapping("/get")
    @RoleCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Picture> getPictureById(@RequestParam Long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(pictureService.getPictureById(id, loginUser));
    }

    /**
     * 分页获取图片列表（管理员）
     */
    @PostMapping("/list/page")
    @RoleCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Picture>> listPicture(@RequestBody PictureQueryRequest pictureQueryRequest) {
        ThrowUtils.throwIf(pictureQueryRequest == null, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(pictureService.listPicture(pictureQueryRequest));
    }

    /**
     * 获取图片标签和分类（公开）
     */
    @GetMapping("/tag_category")
    public BaseResponse<PictureTagCategory> listPictureTagCategory() {
        PictureTagCategory pictureTagCategory = new PictureTagCategory();
        pictureTagCategory.setTagList(DEFAULT_TAG_LIST);
        pictureTagCategory.setCategoryList(DEFAULT_CATEGORY_LIST);
        return ResultUtils.success(pictureTagCategory);
    }

    // ==================== 审核 ====================

    /**
     * 图片审核（管理员）
     */
    @PostMapping("/review")
    @RoleCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> reviewPicture(@RequestBody PictureReviewRequest pictureReviewRequest,
                                               HttpServletRequest request) {
        ThrowUtils.throwIf(pictureReviewRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(pictureService.doPictureReview(pictureReviewRequest, loginUser));
    }

    /**
     * 下载图片
     */
    @GetMapping("/download")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_DOWNLOAD)
    public void downloadPicture(@RequestParam Long id, HttpServletRequest request, HttpServletResponse response) {
        User loginUser = userService.getLoginUser(request);
        pictureService.downloadPicture(id, loginUser, response);
    }

    // ==================== 点赞 ====================

    /**
     * 点赞 / 取消点赞（登录用户）
     */
    @PostMapping("/like")
    public BaseResponse<Boolean> doLike(@RequestBody PictureLikeRequest pictureLikeRequest,
                                        HttpServletRequest request) {
        ThrowUtils.throwIf(pictureLikeRequest == null || pictureLikeRequest.getPictureId() == null,
                ErrorCode.PARAMS_ERROR, "参数错误");
        User loginUser = userService.getLoginUser(request);
        boolean result = pictureLikeService.doLike(pictureLikeRequest.getPictureId(), loginUser);
        return ResultUtils.success(result);
    }
}
