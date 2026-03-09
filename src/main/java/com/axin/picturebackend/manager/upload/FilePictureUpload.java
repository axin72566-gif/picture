package com.axin.picturebackend.manager.upload;

import cn.hutool.core.io.FileUtil;
import com.axin.picturebackend.exception.ErrorCode;
import com.axin.picturebackend.exception.ThrowUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Service
public class FilePictureUpload extends PictureUploadTemplate {

	/**
	 * 校验文件
	 *
	 * @param inputSource 文件源
	 */
	@Override
	public void validInputSource(Object inputSource) {
		MultipartFile multipartFile = (MultipartFile) inputSource;
		ThrowUtils.throwIf(multipartFile == null, ErrorCode.PARAMS_ERROR, "文件不能为空");
		// 1. 校验文件大小
		long fileSize = multipartFile.getSize();
		final long ONE_M = 1024 * 1024L;
		ThrowUtils.throwIf(fileSize > 2 * ONE_M, ErrorCode.PARAMS_ERROR, "文件大小不能超过 2M");
		// 2. 校验文件后缀
		String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
		// 允许上传的文件后缀
		final List<String> ALLOW_FORMAT_LIST = Arrays.asList("jpeg", "jpg", "png", "webp");
		ThrowUtils.throwIf(!ALLOW_FORMAT_LIST.contains(fileSuffix), ErrorCode.PARAMS_ERROR, "文件类型错误");
	}

	/**
	 * 获取文件名 加后缀
	 *
	 * @param inputSource 文件源
	 * @return 文件名
	 */
	@Override
	public String getOriginFileName(Object inputSource) {
		MultipartFile multipartFile = (MultipartFile) inputSource;
		return multipartFile.getOriginalFilename();
	}

	/**
	 * 写临时文件
	 *
	 * @param inputSource 文件源
	 * @param file        文件
	 */
	@Override
	public void dealFile(Object inputSource, File file) {
		MultipartFile multipartFile = (MultipartFile) inputSource;
		try {
			multipartFile.transferTo(file);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}

