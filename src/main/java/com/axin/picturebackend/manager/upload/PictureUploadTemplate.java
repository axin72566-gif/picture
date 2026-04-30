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

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 图片上传模板类（模板方法模式）
 * <p>
 * 定义图片上传的标准流程：校验 → 生成路径 → 写临时文件 → 上传 COS → 封装结果 → 清理临时文件
 * 子类只需实现 {@link #validInputSource}、{@link #getOriginFileName}、{@link #dealFile} 三个抽象方法。
 * </p>
 */
public abstract class PictureUploadTemplate {

    @Resource
    private CosManager cosManager;

    @Resource
    private CosClientConfig cosClientConfig;

    /**
     * 上传图片（模板方法）
     *
     * @param inputSource 输入源（文件或 URL）
     * @param prefix      COS 路径前缀
     * @return 上传结果
     */
    public UploadPictureResult uploadPicture(Object inputSource, String prefix) {
        validInputSource(inputSource);
        // 生成唯一文件路径
        String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String uuid = RandomUtil.randomString(16);
        String suffix = FileUtil.getSuffix(getOriginFileName(inputSource));
        String uploadFileName = String.format("%s_%s.%s", currentDate, uuid, suffix);
        String uploadPath = String.format("%s/%s", prefix, uploadFileName);
        File tempFile = null;
        try {
            tempFile = File.createTempFile(uploadPath, null);
            dealFile(inputSource, tempFile);
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, tempFile);
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            return buildResult(imageInfo, uploadPath, tempFile);
        } catch (IOException e) {
            throw new RuntimeException("文件上传失败", e);
        } finally {
            FileUtil.del(tempFile);
        }
    }

    /**
     * 校验输入源合法性
     */
    public abstract void validInputSource(Object inputSource);

    /**
     * 获取原始文件名（含后缀）
     */
    public abstract String getOriginFileName(Object inputSource);

    /**
     * 将输入源内容写入临时文件
     */
    public abstract void dealFile(Object inputSource, File file);

    /**
     * 构建上传结果（原图信息）
     *
     * @param imageInfo  COS 返回的原图信息
     * @param uploadPath COS 对象路径
     * @param file       临时文件
     * @return 上传结果
     */
    public UploadPictureResult buildResult(ImageInfo imageInfo, String uploadPath, File file) {
        UploadPictureResult result = new UploadPictureResult();
        result.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
        result.setPicName(FileUtil.getName(file));
        result.setPicSize(FileUtil.size(file));
        result.setPicWidth(imageInfo.getWidth());
        result.setPicHeight(imageInfo.getHeight());
        result.setPicScale(imageInfo.getWidth() / (imageInfo.getHeight() * 1.0));
        result.setPicFormat(imageInfo.getFormat());
        return result;
    }

    /**
     * 构建上传结果（压缩图 + 缩略图）
     *
     * @param originFilename     原始文件名
     * @param compressedCiObject 压缩图对象
     * @param thumbnailCiObject  缩略图对象
     * @return 上传结果
     */
    public UploadPictureResult buildResult(String originFilename, CIObject compressedCiObject, CIObject thumbnailCiObject) {
        UploadPictureResult result = new UploadPictureResult();
        result.setPicName(FileUtil.mainName(originFilename));
        result.setPicWidth(compressedCiObject.getWidth());
        result.setPicHeight(compressedCiObject.getHeight());
        result.setPicScale(NumberUtil.round(compressedCiObject.getWidth() * 1.0 / compressedCiObject.getHeight(), 2).doubleValue());
        result.setPicFormat(compressedCiObject.getFormat());
        result.setPicSize(compressedCiObject.getSize().longValue());
        result.setUrl(cosClientConfig.getHost() + "/" + compressedCiObject.getKey());
        result.setThumbnailUrl(cosClientConfig.getHost() + "/" + thumbnailCiObject.getKey());
        return result;
    }
}
