package com.axin.picturebackend.service;

import com.axin.picturebackend.common.DeleteRequest;
import com.axin.picturebackend.model.dto.picture.*;
import com.axin.picturebackend.model.entity.Picture;
import com.axin.picturebackend.model.entity.User;
import com.axin.picturebackend.model.vo.PictureVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
* @author kdkt1
* @description 针对表【picture(图片)】的数据库操作Service
* @createDate 2026-01-20 21:05:55
*/
public interface PictureService extends IService<Picture> {

	/**
	 * 上传图片
	 * @param pictureUploadRequest 图片上传请求
	 * @param inputSource 输入源
	 * @param loginUser 登录用户
	 * @return 图片信息
	 */
	PictureVO uploadPicture(PictureUploadRequest pictureUploadRequest, Object inputSource, User loginUser);

	/**
	 * 获取查询条件
	 * @param pictureQueryRequest 查询条件
	 * @return 查询条件
	 */
	QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);

	/**
	 * 获取图片信息
	 * @param picture  图片
	 * @return 图片信息
	 */
	PictureVO getPictureVO(Picture picture);

	/**
	 * 获取图片列表
	 * @param picturePage 图片分页
	 * @return 图片列表
	 */
	Page<PictureVO> getPagePictureVO(Page<Picture> picturePage);

	/**
	 * 删除图片
	 * @param deleteRequest 删除条件
	 * @param loginUser 登录用户
	 * @return 删除结果
	 */
	Boolean deletePicture(DeleteRequest deleteRequest, User loginUser);

	/**
	 * 修改图片
	 *
	 * @param pictureUpdateRequest 修改图片请求
	 * @param loginUser 登录用户
	 * @return 修改结果
	 */
	PictureVO updatePicture(PictureUpdateRequest pictureUpdateRequest, User loginUser);

	/**
	 * 获取图片信息
	 * @param id 图片id
	 * @return 图片信息
	 */
	Picture getPictureById(Long id, User loginUser);

	/**
	 * 获取图片信息
	 *
	 * @param id        图片id
	 * @param loginUser 登录用户
	 * @return 图片信息
	 */
	PictureVO getPictureVOById(Long id, User loginUser);

	/**
	 * 获取图片
	 * @param pictureQueryRequest 查询条件
	 * @return  图片
	 */
	Page<Picture> listPicture(PictureQueryRequest pictureQueryRequest);

	/**
	 * 获取图片列表
	 * @param pictureQueryRequest 查询条件
	 * @return 图片列表
	 */
	Page<PictureVO> listPictureVO(PictureQueryRequest pictureQueryRequest, User loginUser);

	/**
	 * 修改图片
	 * @param pictureEditRequest 修改图片请求
	 * @param loginUser 登录用户
	 * @return 修改结果
	 */
	PictureVO editPicture(PictureEditRequest pictureEditRequest, User loginUser);

	/**
	 * 校验图片
	 * @param picture  图片
	 */
	void validPicture(Picture picture);

	/**
	 * 图片审核
	 * @param pictureReviewRequest 图片审核请求
	 * @param loginUser 登录用户
	 */
	boolean doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser);

	/**
	 * 批量抓取和创建图片
	 *
	 * @param pictureUploadByBatchRequest 图片批量上传请求
	 * @param loginUser 登录用户
	 * @return 成功创建的图片数
	 */
	Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser);

	/**
	 * 下载图片
	 *
	 * @param pictureId 图片 id
	 * @param loginUser 登录用户
	 * @param response  响应对象
	 */
	void downloadPicture(Long pictureId, User loginUser, HttpServletResponse response);

	/**
	 * 获取每日推荐图片列表
	 *
	 * @param loginUser 登录用户（用于填充点赞状态）
	 * @return 推荐图片VO列表（最多30条）
	 */
	List<PictureVO> getDailyRecommendation(User loginUser);
}
