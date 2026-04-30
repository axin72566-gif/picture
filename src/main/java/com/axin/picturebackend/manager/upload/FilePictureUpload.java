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

/**
 * 文件上传实现（MultipartFile 方式）
 */
@Service
public class FilePictureUpload extends PictureUploadTemplate {

    private static final long MAX_FILE_SIZE = 2 * 1024 * 1024L;
    private static final List<String> ALLOW_FORMAT_LIST = Arrays.asList("jpeg", "jpg", "png", "webp");

    /**
     * 校验上传文件：大小不超过 2MB，格式限 jpeg/jpg/png/webp
     */
    @Override
    public void validInputSource(Object inputSource) {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        ThrowUtils.throwIf(multipartFile == null, ErrorCode.PARAMS_ERROR, "文件不能为空");
        ThrowUtils.throwIf(multipartFile.getSize() > MAX_FILE_SIZE, ErrorCode.PARAMS_ERROR, "文件大小不能超过 2M");
        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        ThrowUtils.throwIf(!ALLOW_FORMAT_LIST.contains(fileSuffix), ErrorCode.PARAMS_ERROR, "文件类型错误");
    }

    @Override
    public String getOriginFileName(Object inputSource) {
        return ((MultipartFile) inputSource).getOriginalFilename();
    }

    @Override
    public void dealFile(Object inputSource, File file) {
        try {
            ((MultipartFile) inputSource).transferTo(file);
        } catch (IOException e) {
            throw new RuntimeException("文件写入失败", e);
        }
    }
}
