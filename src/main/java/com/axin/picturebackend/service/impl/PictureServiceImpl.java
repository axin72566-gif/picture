package com.axin.picturebackend.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.http.HttpUtil;
import com.axin.picturebackend.config.CosClientConfig;
import com.axin.picturebackend.manager.auth.model.SpaceUserPermissionConstant;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.axin.picturebackend.common.DeleteRequest;
import com.axin.picturebackend.constant.PictureConstant;
import com.axin.picturebackend.constant.RedisConstant;
import com.axin.picturebackend.exception.BusinessException;
import com.axin.picturebackend.exception.ErrorCode;
import com.axin.picturebackend.exception.ThrowUtils;
import com.axin.picturebackend.manager.CosManager;
import com.axin.picturebackend.manager.auth.SpaceUserAuthManager;
import com.axin.picturebackend.manager.upload.FilePictureUpload;
import com.axin.picturebackend.manager.upload.UrlPictureUpload;
import com.axin.picturebackend.model.Enum.PictureReviewStatusEnum;
import com.axin.picturebackend.model.Enum.SpaceTypeEnum;
import com.axin.picturebackend.model.Enum.UserRoleEnum;
import com.axin.picturebackend.model.dto.File.UploadPictureResult;
import com.axin.picturebackend.model.dto.picture.*;
import com.axin.picturebackend.model.entity.Picture;
import com.axin.picturebackend.model.entity.Space;
import com.axin.picturebackend.model.entity.SpaceUser;
import com.axin.picturebackend.model.entity.User;
import com.axin.picturebackend.model.vo.PictureVO;
import com.axin.picturebackend.service.PictureLikeService;
import com.axin.picturebackend.service.PictureService;
import com.axin.picturebackend.service.SpaceService;
import com.axin.picturebackend.service.SpaceUserService;
import com.axin.picturebackend.service.UserService;
import com.axin.picturebackend.mapper.PictureMapper;
import com.axin.picturebackend.utils.GsonUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 图片 Service 实现
 */
@Service
@Slf4j
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
        implements PictureService {

    @Resource
    private FilePictureUpload filePictureUpload;
    @Resource
    private UrlPictureUpload urlPictureUpload;
    @Resource
    private UserService userService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private SpaceService spaceService;
    @Resource
    private TransactionTemplate transactionTemplate;
    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;
    @Resource
    private SpaceUserService spaceUserService;
    @Resource
    private CosManager cosManager;
    @Resource
    private CosClientConfig cosClientConfig;
    @Lazy
    @Resource
    private PictureLikeService pictureLikeService;

    /** 本地 Caffeine 缓存（最多 10000 条，5 分钟过期） */
    private final Cache<String, String> LOCAL_PICTURE_CACHE =
            Caffeine.newBuilder()
                    .initialCapacity(1024)
                    .maximumSize(10000L)
                    .expireAfterWrite(5L, TimeUnit.MINUTES)
                    .build();

    /** Redis 缓存基础过期时间：1小时 */
    private static final int BASE_EXPIRE_SECONDS = 3600;
    /** Redis 缓存随机偏移：0~600 秒，防缓存雪崩 */
    private static final int RANDOM_EXPIRE_RANGE = 600;
    private static final Random RANDOM = new Random();

    // ====== 上传 ======

    /**
     * 上传图片（文件/URL 两种输入源），支持新增和更新
     */
    @Override
    public PictureVO uploadPicture(PictureUploadRequest pictureUploadRequest, Object inputSource, User loginUser) {
        ThrowUtils.throwIf(pictureUploadRequest == null, ErrorCode.PARAMS_ERROR, "上传图片参数错误");
        ThrowUtils.throwIf(inputSource == null, ErrorCode.PARAMS_ERROR, "上传图片文件为空");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR, "未登录");
        Long spaceId = pictureUploadRequest.getSpaceId();
        if (spaceId != null) {
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            ThrowUtils.throwIf(space.getTotalCount() >= space.getMaxCount(), ErrorCode.OPERATION_ERROR, "空间数量已满");
            ThrowUtils.throwIf(space.getTotalSize() >= space.getMaxSize(), ErrorCode.OPERATION_ERROR, "空间大小已满");
        }
        // 判断是新增还是更新
        Long pictureId = pictureUploadRequest.getId();
        if (pictureId != null) {
            Picture oldPicture = this.getById(pictureId);
            ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        }
        // 确定存储前缀
        String prefix = spaceId != null
                ? String.format(PictureConstant.SPACE_PICTURE, spaceId)
                : String.format(PictureConstant.PUBLIC_PICTURE, loginUser.getId());
        // 选择上传策略
        UploadPictureResult uploadPictureResult = null;
        if (inputSource instanceof MultipartFile) {
            uploadPictureResult = filePictureUpload.uploadPicture(inputSource, prefix);
        } else if (inputSource instanceof String) {
            uploadPictureResult = urlPictureUpload.uploadPicture(inputSource, prefix);
        }
        if (uploadPictureResult == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传图片失败");
        }
        // 封装图片实体
        Picture picture = new Picture();
        picture.setUrl(uploadPictureResult.getUrl());
        String picName = StrUtil.isNotBlank(pictureUploadRequest.getPicName())
                ? pictureUploadRequest.getPicName()
                : uploadPictureResult.getPicName();
        picture.setName(picName);
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setUserId(loginUser.getId());
        picture.setUpdateTime(new Date());
        picture.setThumbnailUrl(uploadPictureResult.getThumbnailUrl());
        picture.setSpaceId(spaceId);
        UploadPictureResult finalResult = uploadPictureResult;
        if (pictureId != null) {
            // 更新
            picture.setId(pictureId);
            picture.setEditTime(new Date());
            fillPictureReview(picture, loginUser);
            transactionTemplate.execute(status -> {
                ThrowUtils.throwIf(!this.updateById(picture), ErrorCode.SYSTEM_ERROR, "更新图片失败");
                updateSpaceQuota(spaceId, finalResult.getPicSize());
                return null;
            });
        } else {
            // 新增
            picture.setCreateTime(new Date());
            picture.setEditTime(new Date());
            fillPictureReview(picture, loginUser);
            transactionTemplate.execute(status -> {
                ThrowUtils.throwIf(!this.save(picture), ErrorCode.SYSTEM_ERROR, "保存图片失败");
                updateSpaceQuota(spaceId, finalResult.getPicSize());
                return null;
            });
        }
        PictureVO pictureVO = PictureVO.objToVo(picture);
        pictureVO.setUser(userService.getUserVO(loginUser));
        return pictureVO;
    }

    /**
     * 更新空间的图片数量和总大小
     */
    private void updateSpaceQuota(Long spaceId, Long picSize) {
        if (spaceId == null) {
            return;
        }
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        space.setTotalCount(space.getTotalCount() + 1);
        space.setTotalSize(space.getTotalSize() + picSize);
        ThrowUtils.throwIf(!spaceService.updateById(space), ErrorCode.SYSTEM_ERROR, "更新空间额度失败");
    }

    /**
     * 批量爬取图片（调 Bing 图片搜索，最多 30 条）
     */
    @Override
    public Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser) {
        String searchText = pictureUploadByBatchRequest.getSearchText();
        Integer count = pictureUploadByBatchRequest.getCount();
        ThrowUtils.throwIf(count > 30, ErrorCode.PARAMS_ERROR, "最多 30 条");
        String fetchUrl = String.format("https://cn.bing.com/images/async?q=%s&mmasync=1", searchText);
        Document document;
        try {
            document = Jsoup.connect(fetchUrl).get();
        } catch (IOException e) {
            log.error("获取 Bing 页面失败, searchText={}", searchText, e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取页面失败");
        }
        Element div = document.getElementsByClass("dgControl").first();
        if (ObjUtil.isNull(div)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取元素失败");
        }
        Elements imgElementList = div.select("img.mimg");
        int uploadCount = 0;
        for (Element imgElement : imgElementList) {
            String fileUrl = imgElement.attr("src");
            if (StrUtil.isBlank(fileUrl)) {
                continue;
            }
            // 去掉 URL 中的查询参数，防止转义问题
            int questionMarkIndex = fileUrl.indexOf("?");
            if (questionMarkIndex > -1) {
                fileUrl = fileUrl.substring(0, questionMarkIndex);
            }
            PictureUploadRequest pictureUploadRequest = new PictureUploadRequest();
            String name = StrUtil.isNotBlank(pictureUploadByBatchRequest.getNamePrefix())
                    ? pictureUploadByBatchRequest.getNamePrefix()
                    : searchText;
            pictureUploadRequest.setPicName(name + (uploadCount + 1));
            try {
                PictureVO pictureVO = this.uploadPicture(pictureUploadRequest, fileUrl, loginUser);
                log.info("批量抓取图片成功, id={}", pictureVO.getId());
                uploadCount++;
            } catch (Exception e) {
                log.error("批量抓取图片失败, url={}", fileUrl, e);
            }
            if (uploadCount >= count) {
                break;
            }
        }
        return uploadCount;
    }

    // ====== 填充审核 ======

    /**
     * 填充图片审核字段：管理员直接通过，普通用户设为待审核
     */
    private void fillPictureReview(Picture picture, User loginUser) {
        if (userService.isAdmin(loginUser)) {
            picture.setReviewStatus((long) PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewMessage("管理员通过");
            picture.setReviewTime(new Date());
            picture.setReviewerId(loginUser.getId());
        } else {
            picture.setReviewStatus((long) PictureReviewStatusEnum.REVIEWING.getValue());
            picture.setReviewMessage("待审核");
        }
    }

    // ====== 删除 / 更新 / 编辑 ======

    /**
     * 删除图片（同步清理 COS 文件、空间额度、缓存）
     */
    @Override
    public Boolean deletePicture(DeleteRequest deleteRequest, User loginUser) {
        ThrowUtils.throwIf(deleteRequest == null, ErrorCode.PARAMS_ERROR, "删除参数不能为空");
        Long id = deleteRequest.getId();
        transactionTemplate.execute(transactionStatus -> {
            Picture picture = this.getById(id);
            ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
            ThrowUtils.throwIf(!this.removeById(id), ErrorCode.SYSTEM_ERROR, "删除图片失败");
            // 清理 COS 文件
            String url = picture.getUrl();
            if (StringUtils.isNotBlank(url)) {
                String key = url.replace(cosClientConfig.getHost() + "/", "");
                cosManager.deleteObject(key);
            }
            String thumbnailUrl = picture.getThumbnailUrl();
            if (StringUtils.isNotBlank(thumbnailUrl)) {
                String thumbKey = thumbnailUrl.replace(cosClientConfig.getHost() + "/", "");
                cosManager.deleteObject(thumbKey);
            }
            // 更新空间额度（仅空间图片）
            Long spaceId = picture.getSpaceId();
            if (spaceId != null) {
                Space space = spaceService.getById(spaceId);
                ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
                space.setTotalCount(space.getTotalCount() - 1);
                space.setTotalSize(space.getTotalSize() - picture.getPicSize());
                ThrowUtils.throwIf(!spaceService.updateById(space), ErrorCode.SYSTEM_ERROR, "更新空间额度失败");
            }
            // 清理缓存
            String cacheKey = RedisConstant.PICTURE + id;
            LOCAL_PICTURE_CACHE.invalidate(cacheKey);
            // 批量删除多个key
            stringRedisTemplate.delete(Arrays.asList(
                    cacheKey,
                    RedisConstant.PICTURE_LIKE_COUNT + id
            ));

            return true;
        });
        return true;
    }

    /**
     * 管理员更新图片（含审核填充）
     */
    @Override
    public PictureVO updatePicture(PictureUpdateRequest pictureUpdateRequest, User loginUser) {
        ThrowUtils.throwIf(pictureUpdateRequest == null
                           || pictureUpdateRequest.getId() == null
                           || pictureUpdateRequest.getId() <= 0,
                           ErrorCode.PARAMS_ERROR, "更新参数不能为空, id不能为空, 且必须大于0");
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureUpdateRequest, picture);
        picture.setTags(JSONUtil.toJsonStr(pictureUpdateRequest.getTags()));
        picture.setEditTime(new Date());
        picture.setUpdateTime(new Date());
        validPicture(picture);
        fillPictureReview(picture, loginUser);
        ThrowUtils.throwIf(this.getById(pictureUpdateRequest.getId()) == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        ThrowUtils.throwIf(!this.updateById(picture), ErrorCode.SYSTEM_ERROR, "修改图片失败");
        evictPictureCache(pictureUpdateRequest.getId());
        return getPictureVO(picture);
    }

    /**
     * 用户编辑图片（含审核填充）
     */
    @Override
    public PictureVO editPicture(PictureEditRequest pictureEditRequest, User loginUser) {
        ThrowUtils.throwIf(pictureEditRequest == null
                           || pictureEditRequest.getId() == null
                           || pictureEditRequest.getId() <= 0,
                           ErrorCode.PARAMS_ERROR, "编辑参数不能为空, id不能为空, 且必须大于0");
        Long id = pictureEditRequest.getId();
        ThrowUtils.throwIf(this.getById(id) == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureEditRequest, picture);
        picture.setTags(JSONUtil.toJsonStr(pictureEditRequest.getTags()));
        picture.setEditTime(new Date());
        validPicture(picture);
        fillPictureReview(picture, loginUser);
        ThrowUtils.throwIf(!this.updateById(picture), ErrorCode.SYSTEM_ERROR, "修改图片失败");
        evictPictureCache(id);
        return getPictureVO(this.getById(id));
    }

    /**
     * 图片审核（状态只能从待审核流转到通过/拒绝）
     */
    @Override
    public boolean doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser) {
        ThrowUtils.throwIf(pictureReviewRequest == null
                           || pictureReviewRequest.getId() == null
                           || pictureReviewRequest.getId() <= 0,
                           ErrorCode.PARAMS_ERROR, "审核参数不能为空, id不能为空, 且必须大于0");
        Long id = pictureReviewRequest.getId();
        Integer reviewStatus = pictureReviewRequest.getReviewStatus();
        String reviewMessage = pictureReviewRequest.getReviewMessage();
        Picture picture = this.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        // 状态只能是待审核
        PictureReviewStatusEnum oldStatus = PictureReviewStatusEnum.getEnumByValue(Math.toIntExact(picture.getReviewStatus()));
        ThrowUtils.throwIf(oldStatus == null || !oldStatus.equals(PictureReviewStatusEnum.REVIEWING),
                            ErrorCode.OPERATION_ERROR, "图片已审核");
        // 提交状态只能是通过或拒绝
        PictureReviewStatusEnum newStatus = PictureReviewStatusEnum.getEnumByValue(Math.toIntExact(reviewStatus));
        ThrowUtils.throwIf(newStatus != PictureReviewStatusEnum.PASS && newStatus != PictureReviewStatusEnum.REJECT, ErrorCode.OPERATION_ERROR, "状态错误");
        Picture updatePicture = new Picture();
        updatePicture.setId(id);
        updatePicture.setReviewStatus(Long.valueOf(reviewStatus));
        updatePicture.setReviewMessage(reviewMessage);
        updatePicture.setReviewerId(loginUser.getId());
        updatePicture.setReviewTime(new Date());
        ThrowUtils.throwIf(!this.updateById(updatePicture), ErrorCode.SYSTEM_ERROR, "图片审核失败");
        return true;
    }

    /**
     * 校验图片字段合法性
     */
    @Override
    public void validPicture(Picture picture) {
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(ObjUtil.isNull(picture.getId()), ErrorCode.PARAMS_ERROR, "id 不能为空");
        String url = picture.getUrl();
        if (StrUtil.isNotBlank(url)) {
            ThrowUtils.throwIf(url.length() > 1024, ErrorCode.PARAMS_ERROR, "url 过长");
        }
        String introduction = picture.getIntroduction();
        if (StrUtil.isNotBlank(introduction)) {
            ThrowUtils.throwIf(introduction.length() > 800, ErrorCode.PARAMS_ERROR, "简介过长");
        }
    }

    // ====== 查询 ======

    /**
     * 根据ID获取图片实体（含空间权限校验）
     */
    @Override
    public Picture getPictureById(Long id, User loginUser) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR, "id不能为空, 且必须大于0");
        Picture picture = this.getById(id);
        if (picture == null) {
            return null;
        }
        Long spaceId = picture.getSpaceId();
        if (spaceId != null) {
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            SpaceTypeEnum spaceTypeEnum = SpaceTypeEnum.getEnumByValue(space.getSpaceType());
            ThrowUtils.throwIf(spaceTypeEnum == null, ErrorCode.SYSTEM_ERROR, "空间类型不存在");
            if (spaceTypeEnum == SpaceTypeEnum.PRIVATE) {
                // 私有空间：仅创建者可访问
                ThrowUtils.throwIf(!loginUser.getId().equals(space.getUserId()), ErrorCode.NO_AUTH_ERROR, "无访问权限");
            } else {
                // 团队空间：仅成员可访问
                long count = spaceUserService.count(new QueryWrapper<SpaceUser>()
                        .eq("spaceId", spaceId)
                        .eq("userId", loginUser.getId()));
                ThrowUtils.throwIf(count <= 0, ErrorCode.NO_AUTH_ERROR, "无访问权限");
            }
        }
        return picture;
    }

    /**
     * 获取图片 VO（含用户信息和点赞数，不包含 isLiked）
     */
    @Override
    public PictureVO getPictureVO(Picture picture) {
        PictureVO pictureVO = PictureVO.objToVo(picture);
        Long userId = picture.getUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户id不存在");
        }
        User user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }
        pictureVO.setUser(userService.getUserVO(user));
        pictureVO.setLikeCount(pictureLikeService.getLikeCount(picture.getId(), picture.getLikeCount()));
        // 填充空间类型
        Long spaceId = picture.getSpaceId();
        if (spaceId != null) {
            Space space = spaceService.getById(spaceId);
            if (space != null) {
                pictureVO.setSpaceType(space.getSpaceType());
            }
        }
        return pictureVO;
    }

    /**
     * 图片列表转 VO 分页（批量关联用户信息和点赞数）
     */
    @Override
    public Page<PictureVO> getPagePictureVO(Page<Picture> picturePage) {
        if (picturePage == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分页参数错误");
        }
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        if (CollectionUtil.isEmpty(picturePage.getRecords())) {
            return pictureVOPage;
        }
        List<Picture> pictures = picturePage.getRecords();
        List<Long> userIdList = pictures.stream().map(Picture::getUserId).collect(Collectors.toList());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdList)
                .stream().collect(Collectors.groupingBy(User::getId));
        // 批量关联空间信息
        Set<Long> spaceIdSet = pictures.stream().map(Picture::getSpaceId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Space> spaceMap = new HashMap<>();
        if (!spaceIdSet.isEmpty()) {
            spaceMap = spaceService.listByIds(spaceIdSet).stream().collect(Collectors.toMap(Space::getId, s -> s));
        }
        final Map<Long, Space> finalSpaceMap = spaceMap;
        List<PictureVO> pictureVOS = pictures.stream().map(picture -> {
            PictureVO pictureVO = PictureVO.objToVo(picture);
            List<User> userList = userIdUserListMap.get(picture.getUserId());
            if (userList != null && !userList.isEmpty()) {
                pictureVO.setUser(userService.getUserVO(userList.get(0)));
            }
            pictureVO.setLikeCount(pictureLikeService.getLikeCount(picture.getId(), picture.getLikeCount()));
            // 填充空间类型
            Long spaceId = picture.getSpaceId();
            if (spaceId != null && finalSpaceMap.containsKey(spaceId)) {
                pictureVO.setSpaceType(finalSpaceMap.get(spaceId).getSpaceType());
            }
            return pictureVO;
        }).collect(Collectors.toList());
        pictureVOPage.setRecords(pictureVOS);
        return pictureVOPage;
    }

    /**
     * 根据ID获取图片 VO（二级缓存：Caffeine + Redis，含权限和点赞状态）
     */
    @Override
    public PictureVO getPictureVOById(Long id, User loginUser) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String cacheKey = RedisConstant.PICTURE + id;
        // 1. 查本地缓存
        String localCacheStr = LOCAL_PICTURE_CACHE.getIfPresent(cacheKey);
        if (localCacheStr != null) {
            if (localCacheStr.isEmpty()) {
                return null;
            }
            stringRedisTemplate.opsForValue().increment(RedisConstant.PICTURE_VIEW_COUNT + id);
            return buildPictureVOWithExtra(GsonUtils.fromJson(localCacheStr, PictureVO.class), id, loginUser);
        }
        // 2. 查 Redis 缓存
        String redisCacheStr = stringRedisTemplate.opsForValue().get(cacheKey);
        if (redisCacheStr != null) {
            if (redisCacheStr.isEmpty()) {
                return null;
            }
            stringRedisTemplate.opsForValue().increment(RedisConstant.PICTURE_VIEW_COUNT + id);
            LOCAL_PICTURE_CACHE.put(cacheKey, redisCacheStr);
            return buildPictureVOWithExtra(GsonUtils.fromJson(redisCacheStr, PictureVO.class), id, loginUser);
        }
        // 3. 查数据库
        Picture picture = getPictureById(id, loginUser);
        if (picture == null) {
            LOCAL_PICTURE_CACHE.put(cacheKey, "");
            stringRedisTemplate.opsForValue().set(cacheKey, "", BASE_EXPIRE_SECONDS, TimeUnit.SECONDS);
            return null;
        }
        stringRedisTemplate.opsForValue().increment(RedisConstant.PICTURE_VIEW_COUNT + id);
        PictureVO pictureVO = getPictureVO(picture);
        int expireSeconds = BASE_EXPIRE_SECONDS + RANDOM.nextInt(RANDOM_EXPIRE_RANGE);
        String pictureVOJson = GsonUtils.toJson(pictureVO);
        LOCAL_PICTURE_CACHE.put(cacheKey, pictureVOJson);
        stringRedisTemplate.opsForValue().set(cacheKey, pictureVOJson, expireSeconds, TimeUnit.SECONDS);
        // 权限列表不缓存（实时计算）
        Long spaceId = picture.getSpaceId();
        Space space = spaceId != null ? spaceService.getById(spaceId) : null;
        pictureVO.setPermissionList(spaceUserAuthManager.getPermissionList(space, loginUser));
        fillLikeStatus(pictureVO, id, loginUser);
        return pictureVO;
    }

    /**
     * 从缓存反序列化后补充权限和点赞状态
     */
    private PictureVO buildPictureVOWithExtra(PictureVO pictureVO, Long id, User loginUser) {
        Long spaceId = pictureVO.getSpaceId();
        Space space = spaceId != null ? spaceService.getById(spaceId) : null;
        if (space != null) {
            pictureVO.setSpaceType(space.getSpaceType());
        }
        pictureVO.setPermissionList(spaceUserAuthManager.getPermissionList(space, loginUser));
        pictureVO.setLikeCount(pictureLikeService.getLikeCount(id, pictureVO.getLikeCount()));
        fillLikeStatus(pictureVO, id, loginUser);
        return pictureVO;
    }

    /**
     * 填充当前用户的点赞状态
     */
    private void fillLikeStatus(PictureVO pictureVO, Long pictureId, User loginUser) {
        if (loginUser != null) {
            pictureVO.setIsLiked(pictureLikeService.isLiked(pictureId, loginUser.getId()));
        } else {
            pictureVO.setIsLiked(false);
        }
    }

    /**
     * 淘汰图片缓存（本地 + Redis）
     */
    private void evictPictureCache(Long pictureId) {
        String cacheKey = RedisConstant.PICTURE + pictureId;
        LOCAL_PICTURE_CACHE.invalidate(cacheKey);
        stringRedisTemplate.delete(cacheKey);
    }

    /**
     * 构建图片查询条件
     */
    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        if (pictureQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "查询条件错误");
        }
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(pictureQueryRequest.getId() != null, "id", pictureQueryRequest.getId());
        queryWrapper.like(pictureQueryRequest.getName() != null, "name", pictureQueryRequest.getName());
        queryWrapper.like(pictureQueryRequest.getIntroduction() != null, "introduction", pictureQueryRequest.getIntroduction());
        queryWrapper.like(pictureQueryRequest.getCategory() != null, "category", pictureQueryRequest.getCategory());
        List<String> tags = pictureQueryRequest.getTags();
        if (tags != null && !tags.isEmpty()) {
            queryWrapper.and(wrapper -> {
                for (String tag : tags) {
                    wrapper.or().apply("JSON_CONTAINS(tags, {0})", "\"" + tag + "\"");
                }
            });
        }
        queryWrapper.eq(pictureQueryRequest.getPicSize() != null, "picSize", pictureQueryRequest.getPicSize());
        queryWrapper.eq(pictureQueryRequest.getPicWidth() != null, "picWidth", pictureQueryRequest.getPicWidth());
        queryWrapper.eq(pictureQueryRequest.getPicHeight() != null, "picHeight", pictureQueryRequest.getPicHeight());
        queryWrapper.eq(pictureQueryRequest.getPicScale() != null, "picScale", pictureQueryRequest.getPicScale());
        queryWrapper.like(pictureQueryRequest.getPicFormat() != null, "picFormat", pictureQueryRequest.getPicFormat());
        String searchText = pictureQueryRequest.getSearchText();
        if (searchText != null) {
            queryWrapper.and(wrapper -> wrapper.like("name", searchText).or().like("introduction", searchText));
        }
        queryWrapper.eq(pictureQueryRequest.getUserId() != null, "userId", pictureQueryRequest.getUserId());
        queryWrapper.eq(pictureQueryRequest.getReviewStatus() != null, "reviewStatus", pictureQueryRequest.getReviewStatus());
        queryWrapper.eq(pictureQueryRequest.getReviewMessage() != null, "reviewMessage", pictureQueryRequest.getReviewMessage());
        queryWrapper.eq(pictureQueryRequest.getReviewerId() != null, "reviewerId", pictureQueryRequest.getReviewerId());
        queryWrapper.eq(pictureQueryRequest.getSpaceId() != null, "spaceId", pictureQueryRequest.getSpaceId());
        if (pictureQueryRequest.isNullSpaceId()) {
            queryWrapper.isNull("spaceId");
        }
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();
        if (StringUtils.isNotBlank(sortField)) {
            queryWrapper.orderBy(true, "ascend".equals(sortOrder), sortField);
        }
        return queryWrapper;
    }

    /**
     * 管理员分页查询图片（不含权限隔离）
     */
    @Override
    public Page<Picture> listPicture(PictureQueryRequest pictureQueryRequest) {
        if (pictureQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (pictureQueryRequest.getPageSize() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "每页最多 20 条");
        }
        return this.page(
                new Page<>(pictureQueryRequest.getCurrent(), pictureQueryRequest.getPageSize()),
                getQueryWrapper(pictureQueryRequest));
    }

    /**
     * 用户分页查询图片 VO（含空间权限隔离和批量点赞状态）
     */
    @Override
    public Page<PictureVO> listPictureVO(PictureQueryRequest pictureQueryRequest, User loginUser) {
        ThrowUtils.throwIf(pictureQueryRequest == null, ErrorCode.PARAMS_ERROR, "查询条件不能为空");
        ThrowUtils.throwIf(pictureQueryRequest.getPageSize() > 20, ErrorCode.PARAMS_ERROR, "参数错误，不能超过20条");
        Long userId = loginUser.getId();
        Long spaceId = pictureQueryRequest.getSpaceId();
        // 是否是管理员查看审核平台
        boolean isReview = pictureQueryRequest.isReview();
        if (isReview) {
            // 审核平台：用户必须是管理员
            ThrowUtils.throwIf(!loginUser.getUserRole().equals(UserRoleEnum.ADMIN.getValue()), ErrorCode.NO_AUTH_ERROR, "无访问权限");
            // 查询符合状态的图片
            Page<Picture> picturePage = listPicture(pictureQueryRequest);
            Page<PictureVO> pictureVOPage = getPagePictureVO(picturePage);
            // 批量填充当前用户的点赞状态
            List<PictureVO> records = pictureVOPage.getRecords();
            if (!CollectionUtils.isEmpty(records)) {
                List<Long> pictureIds = records.stream().map(PictureVO::getId).collect(Collectors.toList());
                Set<Long> likedIds = pictureLikeService.batchIsLiked(pictureIds, userId);
                records.forEach(vo -> vo.setIsLiked(likedIds.contains(vo.getId())));
            }
            return pictureVOPage;
        }
        if (spaceId == null) {
            // 公共图库：只查已过审且无 spaceId 的图片
            pictureQueryRequest.setReviewStatus((long) PictureReviewStatusEnum.PASS.getValue());
            pictureQueryRequest.setNullSpaceId(true);
        } else {
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            SpaceTypeEnum spaceTypeEnum = SpaceTypeEnum.getEnumByValue(space.getSpaceType());
            // 私有空间：用户必须是空间创建者或已加入空间
            if (spaceTypeEnum == SpaceTypeEnum.PRIVATE) {
                ThrowUtils.throwIf(!userId.equals(space.getUserId()), ErrorCode.NO_AUTH_ERROR, "无访问权限");
            } else {
                // 团队空间：用户必须是空间成员或已加入空间
                SpaceUser spaceUser = spaceUserService.getOne(new QueryWrapper<SpaceUser>()
                        .eq("userId", userId)
                        .eq("spaceId", spaceId));
                ThrowUtils.throwIf(spaceUser == null, ErrorCode.NO_AUTH_ERROR, "无访问权限");
            }
        }
        Page<Picture> picturePage = listPicture(pictureQueryRequest);
        Page<PictureVO> pictureVOPage = getPagePictureVO(picturePage);
        // 批量填充当前用户的点赞状态
        List<PictureVO> records = pictureVOPage.getRecords();
        if (!CollectionUtils.isEmpty(records)) {
            List<Long> pictureIds = records.stream().map(PictureVO::getId).collect(Collectors.toList());
            Set<Long> likedIds = pictureLikeService.batchIsLiked(pictureIds, userId);
            records.forEach(vo -> vo.setIsLiked(likedIds.contains(vo.getId())));
        }
        return pictureVOPage;
    }

    // ====== 定时任务 ======

    /**
     * 每天凌晨4点将 Redis 中的浏览量批量写入 MySQL
     */
    @Scheduled(cron = "0 0 4 * * ?")
    public void writeViewCount() {
        Set<String> keys = stringRedisTemplate.keys(RedisConstant.PICTURE_VIEW_COUNT + "*");
        if (CollectionUtils.isEmpty(keys)) {
            log.info("暂无图片浏览量需要同步");
            return;
        }
        List<Long> pictureIds = keys.stream()
                .map(this::parsePictureIdFromKey)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        int successCount = 0;
        for (Long pictureId : pictureIds) {
            Picture picture = getPictureById(pictureId, null);
            if (picture == null) {
                continue;
            }
            String viewCountStr = stringRedisTemplate.opsForValue().get(RedisConstant.PICTURE_VIEW_COUNT + pictureId);
            if (viewCountStr == null) {
                continue;
            }
            try {
                picture.setViewCount(Long.parseLong(viewCountStr));
                if (updateById(picture)) {
                    successCount++;
                } else {
                    log.warn("更新图片浏览量失败, pictureId={}", pictureId);
                }
            } catch (NumberFormatException e) {
                log.error("图片浏览量格式错误, pictureId={}, value={}", pictureId, viewCountStr);
            }
        }
        Long deletedCount = stringRedisTemplate.delete(keys);
        log.info("浏览量写库完成，共同步 {} 条，删除 Redis key {} 个", successCount, deletedCount);
    }

    /**
     * 从 Redis Key 中解析图片ID，格式：picture:view:{pictureId}
     */
    private Long parsePictureIdFromKey(String redisKey) {
        try {
            String[] parts = redisKey.split(":");
            if (parts.length < 3) {
                return null;
            }
            return Long.parseLong(parts[2]);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 下载图片
     */
    @Override
    public void downloadPicture(Long pictureId, User loginUser, HttpServletResponse response) {
        ThrowUtils.throwIf(pictureId == null || pictureId <= 0, ErrorCode.PARAMS_ERROR);
        Picture picture = this.getById(pictureId);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");

        // 执行下载
        String pictureUrl = picture.getUrl();
        String pictureName = picture.getName();
        String format = picture.getPicFormat();
        if (StrUtil.isBlank(format)) {
            format = "png";
        }
        String fileName = pictureName + "." + format;

        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType("application/octet-stream");
            String encodedFileName = java.net.URLEncoder.encode(fileName, StandardCharsets.UTF_8.name()).replace("+", "%20");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + encodedFileName + "\"; filename*=utf-8''" + encodedFileName);

            URL url = new URL(pictureUrl);
            URLConnection connection = url.openConnection();
            inputStream = connection.getInputStream();
            outputStream = response.getOutputStream();

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
        } catch (Exception e) {
            log.error("下载图片失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "下载图片失败");
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    log.error("关闭输入流失败", e);
                }
            }
        }
    }
}
