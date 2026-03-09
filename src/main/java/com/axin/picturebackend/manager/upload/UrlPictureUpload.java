package com.axin.picturebackend.manager.upload;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.axin.picturebackend.exception.BusinessException;
import com.axin.picturebackend.exception.ErrorCode;
import com.axin.picturebackend.exception.ThrowUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

@Service
public class UrlPictureUpload extends PictureUploadTemplate {

	/**
	 * 校验文件
	 *
	 * @param inputSource 文件
	 */
	@Override
	public void validInputSource(Object inputSource) {
		String fileUrl = (String) inputSource;
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
	 * 获取文件名
	 *
	 * @param inputSource 文件源
	 * @return 文件名
	 */
	@Override
	public String getOriginFileName(Object inputSource) {
		String url = (String) inputSource;
		return FileUtil.getName(url);
	}

	/**
	 * 处理文件
	 *
	 * @param inputSource 文件源
	 * @param file        文件
	 */
	@Override
	public void dealFile(Object inputSource, File file) {
		String url = (String) inputSource;
		try {
			FileUtil.writeFromStream(new URL(url).openStream(), file);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
