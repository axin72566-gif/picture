package com.axin.picturebackend.manager;

import com.axin.picturebackend.config.CosClientConfig;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;

/**
 * 腾讯云 COS 对象存储管理类
 */
@Slf4j
@Component
public class CosManager {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private COSClient cosClient;

    /**
     * 上传图片对象（附带图片基本信息）
     *
     * @param key  COS 对象键
     * @param file 本地文件
     * @return 上传结果
     */
    public PutObjectResult putPictureObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);
        // 开启图片信息返回（1 = 返回原图信息）
        PicOperations picOperations = new PicOperations();
        picOperations.setIsPicInfo(1);
        putObjectRequest.setPicOperations(picOperations);
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 删除 COS 对象
     *
     * @param key COS 对象键
     */
    public void deleteObject(String key) {
        try {
            cosClient.deleteObject(cosClientConfig.getBucket(), key);
        } catch (CosServiceException e) {
            log.error("COS 服务端异常，key={}, error={}", key, e.getMessage());
        } catch (CosClientException e) {
            log.error("COS 客户端异常，key={}, error={}", key, e.getMessage());
        }
    }
}
