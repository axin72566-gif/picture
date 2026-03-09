package com.axin.picturebackend.manager.upload;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.axin.picturebackend.config.CosClientConfig;
import com.axin.picturebackend.manager.CosManager;
import com.axin.picturebackend.model.dto.File.UploadPictureResult;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.CIObject;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.qcloud.cos.model.ciModel.persistence.ProcessResults;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public abstract class PictureUploadTemplate {

	@Resource
	private CosManager cosManager;

	@Resource
	private CosClientConfig cosClientConfig;

	/**
	 * 文件上传
	 *
	 * @param inputSource 文件源
	 * @param prefix      文件路径前缀
	 * @return 上传后的文件路径
	 */
	public UploadPictureResult uploadPicture(Object inputSource, String prefix) {
		// 校验输入源
		validInputSource(inputSource);
		// 获取文件上传路径
		String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
		String uuid = RandomUtil.randomString(16);
		String suffix = FileUtil.getSuffix(getOriginFileName(inputSource));
		String uploadFileName = String.format("%s_%s.%s", currentDate, uuid, suffix);
		String uploadPath = String.format("%s/%s", prefix, uploadFileName);
		File file = null;
		try {
			// 创建临时文件
			file = File.createTempFile(uploadPath, null);
			// 输入源写到临时文件
			dealFile(inputSource, file);
			// 上传文件
			PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
			ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
//			ProcessResults processResults = putObjectResult.getCiUploadResult().getProcessResults();
//			List<CIObject> objectList = processResults.getObjectList();
//			if (!objectList.isEmpty()) {
//				CIObject compressCiObject = objectList.get(0);
//				// 默认压缩图
//				CIObject thumbnailCiObject = compressCiObject;
//			    // 有缩略图才获取缩略图结果
//				if (objectList.size() > 1) {
//					thumbnailCiObject = objectList.get(1);
//				}
//				return buildResult(FileUtil.getName(file), compressCiObject, thumbnailCiObject);
//			}
			// 结果封装
			return buildResult(imageInfo, uploadPath, file);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			// 删除临时文件
			FileUtil.del(file);
		}
	}

	// 校验输入源
	public abstract void validInputSource(Object inputSource);

	// 获取原始文件名
	public abstract String getOriginFileName(Object inputSource);

	// 输入源处理成文件
	public abstract void dealFile(Object inputSource, File file);

	/**
	 * 构建结果 原图
	 *
	 * @param imageInfo  图片信息
	 * @param uploadPath 文件上传路径
	 * @param file       文件
	 * @return 上传结果
	 */
	public UploadPictureResult buildResult(ImageInfo imageInfo, String uploadPath, File file) {
		UploadPictureResult uploadPictureResult = new UploadPictureResult();
		uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
		uploadPictureResult.setPicName(FileUtil.getName(file));
		uploadPictureResult.setPicSize(FileUtil.size(file));
		uploadPictureResult.setPicWidth(imageInfo.getWidth());
		uploadPictureResult.setPicHeight(imageInfo.getHeight());
		uploadPictureResult.setPicScale(imageInfo.getWidth() / imageInfo.getHeight() * 1.0);
		uploadPictureResult.setPicFormat(imageInfo.getFormat());
		return uploadPictureResult;
	}

	/**
	 * 构建结果 压缩图 缩略图
	 *
	 * @param originFilename     原始文件名
	 * @param compressedCiObject 压缩后的图片对象
	 * @return 上传结果
	 */
	public UploadPictureResult buildResult(String originFilename, CIObject compressedCiObject, CIObject thumbnailCiObject) {
		UploadPictureResult uploadPictureResult = new UploadPictureResult();
		uploadPictureResult.setPicName(FileUtil.mainName(originFilename));
		uploadPictureResult.setPicWidth(compressedCiObject.getWidth());
		uploadPictureResult.setPicHeight(compressedCiObject.getHeight());
		uploadPictureResult.setPicScale(NumberUtil.round(compressedCiObject.getWidth() * 1.0 / compressedCiObject.getHeight(), 2).doubleValue());
		uploadPictureResult.setPicFormat(compressedCiObject.getFormat());
		uploadPictureResult.setPicSize(compressedCiObject.getSize().longValue());
		uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + compressedCiObject.getKey());
		uploadPictureResult.setThumbnailUrl(cosClientConfig.getHost() + "/" + thumbnailCiObject.getKey());
		return uploadPictureResult;
	}
}
