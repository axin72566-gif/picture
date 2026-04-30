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

/**
 * URL 上传实现（远程图片 URL 方式）
 */
@Service
public class UrlPictureUpload extends PictureUploadTemplate {

    private static final long MAX_FILE_SIZE = 2 * 1024 * 1024L;
    private static final List<String> ALLOW_CONTENT_TYPES = Arrays.asList("image/jpeg", "image/jpg", "image/png", "image/webp");

    /**
     * 校验 URL 合法性：格式 → 协议 → HEAD 请求验证类型和大小
     */
    @Override
    public void validInputSource(Object inputSource) {
        String fileUrl = (String) inputSource;
        ThrowUtils.throwIf(StrUtil.isBlank(fileUrl), ErrorCode.PARAMS_ERROR, "文件地址不能为空");
        // 校验 URL 格式
        try {
            new URL(fileUrl);
        } catch (MalformedURLException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件地址格式不正确");
        }
        // 校验协议
        ThrowUtils.throwIf(
                !(fileUrl.startsWith("http://") || fileUrl.startsWith("https://")),
                ErrorCode.PARAMS_ERROR, "仅支持 HTTP 或 HTTPS 协议的文件地址"
        );
        // HEAD 请求验证文件类型和大小
        try (HttpResponse response = HttpUtil.createRequest(Method.HEAD, fileUrl).execute()) {
            if (response.getStatus() != HttpStatus.HTTP_OK) {
                return;
            }
            String contentType = response.header("Content-Type");
            if (StrUtil.isNotBlank(contentType)) {
                ThrowUtils.throwIf(
                        !ALLOW_CONTENT_TYPES.contains(contentType.toLowerCase()),
                        ErrorCode.PARAMS_ERROR, "文件类型错误"
                );
            }
            String contentLengthStr = response.header("Content-Length");
            if (StrUtil.isNotBlank(contentLengthStr)) {
                try {
                    ThrowUtils.throwIf(
                            Long.parseLong(contentLengthStr) > MAX_FILE_SIZE,
                            ErrorCode.PARAMS_ERROR, "文件大小不能超过 2M"
                    );
                } catch (NumberFormatException e) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小格式错误");
                }
            }
        }
    }

    @Override
    public String getOriginFileName(Object inputSource) {
        return FileUtil.getName((String) inputSource);
    }

    @Override
    public void dealFile(Object inputSource, File file) {
        try {
            FileUtil.writeFromStream(new URL((String) inputSource).openStream(), file);
        } catch (IOException e) {
            throw new RuntimeException("URL 文件写入失败", e);
        }
    }
}
