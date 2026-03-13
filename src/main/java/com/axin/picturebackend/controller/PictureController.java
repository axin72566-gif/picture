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
import com.axin.picturebackend.service.PictureService;
import com.axin.picturebackend.service.UserService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/picture")
public class PictureController {

	@Resource
	private PictureService pictureService;
	@Resource
	private UserService userService;

	/**
	 * 上传图片 文件
	 *
	 * @param pictureUploadRequest 图片上传请求
	 * @param multipartFile        文件
	 * @param request              请求
	 * @return 图片信息
	 */
	@PostMapping("/upload")
	@SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)
	public BaseResponse<PictureVO> uploadPicture(@ModelAttribute PictureUploadRequest pictureUploadRequest,
	                                             @RequestPart("file") MultipartFile multipartFile, HttpServletRequest request) {
		return ResultUtils.success(pictureService.uploadPicture(pictureUploadRequest, multipartFile, userService.getLoginUser(request)));
	}

	/**
	 * 上传图片 链接
	 *
	 * @param pictureUploadRequest 图片上传请求
	 * @param request              请求
	 * @return 图片信息
	 */
	@PostMapping("/upload/url")
	@SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)
	public BaseResponse<PictureVO> uploadPictureByUrl(@RequestBody PictureUploadRequest pictureUploadRequest, HttpServletRequest request) {
		return ResultUtils.success(pictureService.uploadPicture(pictureUploadRequest, pictureUploadRequest.getFileUrl(), userService.getLoginUser(request)));
	}

	/**
	 * 删除图片
	 *
	 * @param deleteRequest 删除条件
	 * @param request       请求
	 * @return 删除结果
	 */
	@PostMapping("/delete")
	@SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_DELETE)
	public BaseResponse<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
		return ResultUtils.success(pictureService.deletePicture(deleteRequest, userService.getLoginUser(request)));
	}

	/**
	 * 更新图片 管理员
	 *
	 * @param pictureUpdateRequest 修改图片请求
	 * @return 修改结果
	 */
	@PostMapping("/update")
	@RoleCheck(mustRole = UserConstant.ADMIN_ROLE)
	public BaseResponse<PictureVO> updatePicture(@RequestBody PictureUpdateRequest pictureUpdateRequest, HttpServletRequest request) {
		return ResultUtils.success(pictureService.updatePicture(pictureUpdateRequest, userService.getLoginUser(request)));
	}

	/**
	 * 编辑图片
	 *
	 * @param pictureEditRequest 编辑图片请求
	 * @return 编辑图片结果
	 */
	@PostMapping("/edit")
	@SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
	public BaseResponse<PictureVO> editPicture(@RequestBody PictureEditRequest pictureEditRequest, HttpServletRequest request) {
		return ResultUtils.success(pictureService.editPicture(pictureEditRequest, userService.getLoginUser(request)));
	}

	/**
	 * 获取图片信息
	 *
	 * @param id 图片id
	 * @return 图片信息
	 */
	@GetMapping("/get/vo")
	public BaseResponse<PictureVO> getPictureVOById(@RequestParam Long id, HttpServletRequest request) {
		return ResultUtils.success(pictureService.getPictureVOById(id, userService.getLoginUser(request)));
	}

	/**
	 * 获取图片列表
	 *
	 * @param pictureQueryRequest 获取图片列表请求
	 * @return 图片列表
	 */
	@PostMapping("/list/page/vo")
	public BaseResponse<Page<PictureVO>> listPictureVO(@RequestBody PictureQueryRequest pictureQueryRequest, HttpServletRequest request) {
		return ResultUtils.success(pictureService.listPictureVO(pictureQueryRequest, userService.getLoginUser(request)));
	}

	/**
	 * 获取图片 管理员
	 *
	 * @param id 图片id
	 * @return 图片信息
	 */
	@GetMapping("/get")
	@RoleCheck(mustRole = UserConstant.ADMIN_ROLE)
	public BaseResponse<Picture> getPictureById(@RequestParam Long id, HttpServletRequest request) {
		return ResultUtils.success(pictureService.getPictureById(id, userService.getLoginUser(request)));
	}

	/**
	 * 获取图片列表 管理员
	 *
	 * @param pictureQueryRequest 查询条件
	 * @return 图片列表
	 */
	@PostMapping("/list/page")
	@RoleCheck(mustRole = UserConstant.ADMIN_ROLE)
	public BaseResponse<Page<Picture>> listPicture(@RequestBody PictureQueryRequest pictureQueryRequest) {
		return ResultUtils.success(pictureService.listPicture(pictureQueryRequest));
	}

	/**
	 * 图片审核 管理员
	 *
	 * @param pictureReviewRequest 图片审核请求
	 * @param request              请求
	 * @return 审核结果
	 */
	@PostMapping("/review")
	@RoleCheck(mustRole = UserConstant.ADMIN_ROLE)
	public BaseResponse<Boolean> reviewPicture(@RequestBody PictureReviewRequest pictureReviewRequest, HttpServletRequest request) {
		return ResultUtils.success(pictureService.doPictureReview(pictureReviewRequest, userService.getLoginUser(request)));
	}

	/**
	 * 批量上传图片 管理员
	 *
	 * @param pictureUploadByBatchRequest 图片上传批量请求
	 * @param request                     请求
	 * @return 图片数量
	 */
	@PostMapping("/upload/batch")
	@RoleCheck(mustRole = UserConstant.ADMIN_ROLE)
	public BaseResponse<Integer> uploadPictureByBatch(@RequestBody PictureUploadByBatchRequest pictureUploadByBatchRequest, HttpServletRequest request) {
		ThrowUtils.throwIf(pictureUploadByBatchRequest == null, ErrorCode.PARAMS_ERROR);
		User loginUser = userService.getLoginUser(request);
		int uploadCount = pictureService.uploadPictureByBatch(pictureUploadByBatchRequest, loginUser);
		return ResultUtils.success(uploadCount);
	}

	/**
	 * 获取图片标签和分类
	 *
	 * @return 图片标签和分类
	 */
	@GetMapping("/tag_category")
	public BaseResponse<PictureTagCategory> listPictureTagCategory() {
		PictureTagCategory pictureTagCategory = new PictureTagCategory();
		List<String> tagList = Arrays.asList("热门", "搞笑", "生活", "高清", "艺术", "校园", "背景", "简历", "创意");
		List<String> categoryList = Arrays.asList("模板", "电商", "表情包", "素材", "海报");
		pictureTagCategory.setTagList(tagList);
		pictureTagCategory.setCategoryList(categoryList);
		return ResultUtils.success(pictureTagCategory);
	}

}
