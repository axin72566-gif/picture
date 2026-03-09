package com.axin.picturebackend.manager;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.axin.picturebackend.config.CosClientConfig;
import com.axin.picturebackend.exception.BusinessException;
import com.axin.picturebackend.exception.ErrorCode;
import com.axin.picturebackend.exception.ThrowUtils;
import com.axin.picturebackend.model.dto.File.UploadPictureResult;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class FileManager {

	@Resource
	private CosClientConfig cosClientConfig;

	@Resource
	private CosManager cosManager;

	/**
	 * 文件上传
	 *
	 * @param multipartFile 前端上传的文件对象
	 * @param prefix        文件路径前缀
	 * @return 上传后的文件路径
	 */
	public UploadPictureResult uploadPicture(MultipartFile multipartFile, String prefix) {
		// 校验文件
		validPicture(multipartFile);
		// 拼接 COS 文件路径 调用方指定前缀 + 日期 + UUID + 文件后缀
		String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
		String uuid = RandomUtil.randomString(16);
		String suffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
		String uploadFileName = String.format("%s_%s.%s", currentDate, uuid, suffix);
		String uploadPath = String.format("%s/%s", prefix, uploadFileName);
		// 上传文件
		File file = null;
		try {
			// 创建临时文件
			file = File.createTempFile(uploadPath, null);
			// 前端文件对象写入临时文件
			multipartFile.transferTo(file);
			// 上传图片
			PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
			ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
			// 封装返回结果
			UploadPictureResult uploadPictureResult = new UploadPictureResult();
			uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
			uploadPictureResult.setPicName(FileUtil.mainName((File) multipartFile));
			uploadPictureResult.setPicSize(multipartFile.getSize());
			uploadPictureResult.setPicWidth(imageInfo.getWidth());
			uploadPictureResult.setPicHeight(imageInfo.getHeight());
			uploadPictureResult.setPicScale(imageInfo.getWidth() / imageInfo.getHeight() * 1.0);
			uploadPictureResult.setPicFormat(imageInfo.getFormat());
			return uploadPictureResult;
		} catch (IOException e) {
			throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
		} finally {
			// 删除临时文件
			FileUtil.del(file);
		}
	}

	/**
	 * URL 文件上传
	 *
	 * @param url    文件地址
	 * @param prefix 文件路径前缀
	 * @return 上传后的文件路径
	 */
	public UploadPictureResult uploadPictureByUrl(String url, String prefix) {
		// 校验 url
		validPicture(url);
		// 拼接 COS 文件路径 调用方指定前缀 + 日期 + UUID + 文件后缀
		String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
		String uuid = RandomUtil.randomString(16);
		String suffix = FileUtil.getSuffix(FileUtil.file(url));
		String uploadFileName = String.format("%s_%s.%s", currentDate, uuid, suffix);
		String uploadPath = String.format("%s/%s", prefix, uploadFileName);
		// 上传文件
		File file = null;
		try {
			// 创建临时文件
			file = File.createTempFile(uploadPath, null);
			// 前端文件对象写入临时文件
			HttpUtil.downloadFile(url, file);
			// 上传图片
			PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
			ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
			// 封装返回结果
			UploadPictureResult uploadPictureResult = new UploadPictureResult();
			uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
			uploadPictureResult.setPicName(FileUtil.mainName(FileUtil.file(url)));
			uploadPictureResult.setPicSize(FileUtil.size(FileUtil.file(url)));
			uploadPictureResult.setPicWidth(imageInfo.getWidth());
			uploadPictureResult.setPicHeight(imageInfo.getHeight());
			uploadPictureResult.setPicScale(imageInfo.getWidth() / imageInfo.getHeight() * 1.0);
			uploadPictureResult.setPicFormat(imageInfo.getFormat());
			return uploadPictureResult;
		} catch (IOException e) {
			throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
		} finally {
			// 删除临时文件
			FileUtil.del(file);
		}
	}

	/**
	 * 校验文件 url
	 *
	 * @param fileUrl 文件地址
	 */
	private void validPicture(String fileUrl) {
		ThrowUtils.throwIf(StrUtil.isBlank(fileUrl), ErrorCode.PARAMS_ERROR, "文件地址不能为空");
		try {
			// 1. 验证 URL 格式
			new URL(fileUrl); // 验证是否是合法的 URL
		} catch (MalformedURLException e) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件地址格式不正确");
		}
		// 2. 校验 URL 协议
		ThrowUtils.throwIf(!(fileUrl.startsWith("http://") || fileUrl.startsWith("https://")),
				ErrorCode.PARAMS_ERROR, "仅支持 HTTP 或 HTTPS 协议的文件地址");
		// 3. 发送 HEAD 请求以验证文件是否存在
		try (HttpResponse response = HttpUtil.createRequest(Method.HEAD, fileUrl).execute()) {
			// 未正常返回，无需执行其他判断
			if (response.getStatus() != HttpStatus.HTTP_OK) {
				return;
			}
			// 4. 校验文件类型
			String contentType = response.header("Content-Type");
			if (StrUtil.isNotBlank(contentType)) {
				// 允许的图片类型
				final List<String> ALLOW_CONTENT_TYPES = Arrays.asList("image/jpeg", "image/jpg", "image/png", "image/webp");
				ThrowUtils.throwIf(!ALLOW_CONTENT_TYPES.contains(contentType.toLowerCase()),
						ErrorCode.PARAMS_ERROR, "文件类型错误");
			}
			// 5. 校验文件大小
			String contentLengthStr = response.header("Content-Length");
			if (StrUtil.isNotBlank(contentLengthStr)) {
				try {
					long contentLength = Long.parseLong(contentLengthStr);
					final long TWO_MB = 2 * 1024 * 1024L; // 限制文件大小为 2MB
					ThrowUtils.throwIf(contentLength > TWO_MB, ErrorCode.PARAMS_ERROR, "文件大小不能超过 2M");
				} catch (NumberFormatException e) {
					throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小格式错误");
				}
			}
		}
	}

	/**
	 * 校验文件 本地文件
	 *
	 * @param multipartFile 文件
	 */
	private void validPicture(MultipartFile multipartFile) {
		// 文件大小
		long fileSize = multipartFile.getSize();
		if (fileSize > 1024 * 1024 * 10) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小不能超过 10M");
		}
		// 文件后缀
		String fileSuffix = multipartFile.getContentType();
		// 允许的文件后缀
		List<String> fileSuffixList = Arrays.asList("jpg", "jpeg", "png");
		if (!fileSuffixList.contains(fileSuffix)) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的文件类型");
		}
	}
}

