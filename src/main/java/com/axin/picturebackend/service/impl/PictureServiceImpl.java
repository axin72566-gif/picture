package com.axin.picturebackend.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.axin.picturebackend.common.DeleteRequest;
import com.axin.picturebackend.constant.PictureConstant;
import com.axin.picturebackend.constant.RedisConstant;
import com.axin.picturebackend.exception.BusinessException;
import com.axin.picturebackend.exception.ErrorCode;
import com.axin.picturebackend.exception.ThrowUtils;
import com.axin.picturebackend.manager.auth.SpaceUserAuthManager;
import com.axin.picturebackend.manager.upload.FilePictureUpload;
import com.axin.picturebackend.manager.upload.UrlPictureUpload;
import com.axin.picturebackend.model.Enum.PictureReviewStatusEnum;
import com.axin.picturebackend.model.Enum.SpaceLevelEnum;
import com.axin.picturebackend.model.Enum.SpaceTypeEnum;
import com.axin.picturebackend.model.dto.File.UploadPictureResult;
import com.axin.picturebackend.model.dto.picture.*;
import com.axin.picturebackend.model.entity.Space;
import com.axin.picturebackend.model.entity.SpaceUser;
import com.axin.picturebackend.model.entity.User;
import com.axin.picturebackend.model.vo.PictureVO;
import com.axin.picturebackend.service.SpaceService;
import com.axin.picturebackend.service.SpaceUserService;
import com.axin.picturebackend.service.UserService;
import com.axin.picturebackend.utils.GsonUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.axin.picturebackend.model.entity.Picture;
import com.axin.picturebackend.service.PictureService;
import com.axin.picturebackend.mapper.PictureMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
* @author kdkt1
* @description 针对表【picture(图片)】的数据库操作Service实现
* @createDate 2026-01-20 21:05:55
*/
@Service
@Slf4j
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
    implements PictureService{

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
	// 构建本地缓存
	private final Cache<String, String> LOCAL_PICTURE_CACHE =
			Caffeine.newBuilder().initialCapacity(1024)
					.maximumSize(10000L)
					// 缓存 5 分钟移除
					.expireAfterWrite(5L, TimeUnit.MINUTES)
					.build();
	// 基础过期时间：1小时（3600秒）
	private static final int BASE_EXPIRE_SECONDS = 3600;
	// 随机过期偏移：0~600秒（10分钟），防缓存雪崩
	private static final int RANDOM_EXPIRE_RANGE = 600;
	private static final Random RANDOM = new Random();

	/**
	 * 上传图片
	 * @param pictureUploadRequest 图片上传请求
	 * @param inputSource 输入源
	 * @param loginUser 登录用户
	 * @return 图片信息
	 */
	@Override
	public PictureVO uploadPicture(PictureUploadRequest pictureUploadRequest, Object inputSource, User loginUser) {
		// 参数校验
		if (pictureUploadRequest == null) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR, "上传图片参数错误");
		}
		if (inputSource == null) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR, "上传图片文件为空");
		}
		if (loginUser == null) {
			throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "未登录");
		}
		Long spaceId = pictureUploadRequest.getSpaceId();
		if (spaceId != null) {
			Space space = spaceService.getById(spaceId);
			if (space == null) {
				throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "空间不存在");
			}
			// 额度判断
			if (space.getTotalCount() >= space.getMaxCount()) {
				throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间数量已满");
			}
			if (space.getTotalSize() >= space.getMaxSize()) {
				throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间大小已满");
			}
		}
		// 判断是新增还是更新
		Long pictureId = pictureUploadRequest.getId();
		if (pictureId != null) {
			// 更新
			Picture oldPicture = this.getById(pictureId);
			if (oldPicture == null) {
				throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图片不存在");
			}
//			// 仅本人或者管理员可更新
//			if (!loginUser.getId().equals(oldPicture.getUserId()) && !userService.isAdmin(loginUser)) {
//				throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限更新该图片");
//			}
//			// 请求中没有空间id，说明是公共图库，复用原来的
//			if (spaceId ==  null) {
//				spaceId = oldPicture.getSpaceId();
//			} else {
//				// 传了空间id，说明是私有图库，需要判断空间权限
//				if (!spaceId.equals(oldPicture.getSpaceId())) {
//					throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限更新该图片");
//				}
//			}
		}
		String prefix;
		if (spaceId != null) {
			// 上传到空间
			prefix = String.format(PictureConstant.SPACE_PICTURE, spaceId);
		} else {
			// 公共图库
			prefix = String.format(PictureConstant.PUBLIC_PICTURE, loginUser.getId());
		}
		// 判断输入源 选择上传方式 上传文件
		UploadPictureResult uploadPictureResult = null;
		if (inputSource instanceof MultipartFile) {
			uploadPictureResult = filePictureUpload.uploadPicture(inputSource, prefix);
		}
		if (inputSource instanceof String) {
			uploadPictureResult = urlPictureUpload.uploadPicture(inputSource, prefix);
		}
		if (uploadPictureResult == null) {
			throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传图片失败");
		}
		// 封装图片结果
		Picture picture = new Picture();
		picture.setUrl(uploadPictureResult.getUrl());
		String picName = uploadPictureResult.getPicName();
		if (StrUtil.isNotBlank(pictureUploadRequest.getPicName())) {
			picName = pictureUploadRequest.getPicName();
		}
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
		// 判断是更新还是新增 插入数据库
		if (pictureId != null) {
			// 更新
			picture.setId(pictureId);
			picture.setEditTime(new Date());
			// 补充审核字段
			fillPictureReview(picture, loginUser);
			UploadPictureResult finalUploadPictureResult1 = uploadPictureResult;
			transactionTemplate.execute(transactionStatus -> {
				boolean update = this.updateById(picture);
				if (!update) {
					throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新图片失败");
				}
				// 更新空间额度
				if (spaceId != null) {
					Space space = spaceService.getById(spaceId);
					if (space != null) {
						space.setTotalCount(space.getTotalCount() + 1);
						space.setTotalSize(space.getTotalSize() + finalUploadPictureResult1.getPicSize());
						boolean updateSpace = spaceService.updateById(space);
						if (!updateSpace) {
							throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新空间失败");
						}
					}
				}
				return null;
			});
		} else {
			// 新增
			picture.setCreateTime(new Date());
			picture.setEditTime(new Date());
			fillPictureReview(picture, loginUser);
			UploadPictureResult finalUploadPictureResult = uploadPictureResult;
			transactionTemplate.execute(transactionStatus -> {
				boolean save = this.save(picture);
				if (!save) {
					throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存图片失败");
				}
				// 更新空间额度
				Long picSize = finalUploadPictureResult.getPicSize();
				if (spaceId != null) {
					Space space = spaceService.getById(spaceId);
					if (space != null) {
						space.setTotalCount(space.getTotalCount() + 1);
						space.setTotalSize(space.getTotalSize() + picSize);
						boolean update = spaceService.updateById(space);
						if (!update) {
							throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新空间失败");
						}
					}
				}
				return null;
			});
		}
		// 返回图片信息
		PictureVO pictureVO = PictureVO.objToVo(picture);
		pictureVO.setUser(userService.getUserVO(loginUser));
		return pictureVO;
	}

	/**
	 * 填充图片审核信息
	 * @param picture  图片
	 * @param loginUser 登录用户
	 */
	private void fillPictureReview(Picture picture, User loginUser) {
		// 如果是管理员，则直接通过
		if (userService.isAdmin(loginUser)) {
			picture.setReviewStatus((long) PictureReviewStatusEnum.PASS.getValue());
			picture.setReviewMessage("管理员通过");
			picture.setReviewTime(new Date());
			picture.setReviewerId(loginUser.getId());
		} else {
			// 待审核
			picture.setReviewStatus((long) PictureReviewStatusEnum.REVIEWING.getValue());
			picture.setReviewMessage("待审核");
		}
	}

	/**
	 * 获取查询条件
	 * @param pictureQueryRequest 查询条件
	 * @return 查询条件
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
			// 方式1: 使用 OR 条件匹配任意一个标签
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
			queryWrapper.and(wrapper -> wrapper.like("name", searchText)
					.or()
					.like("introduction", searchText));
		}
		queryWrapper.eq(pictureQueryRequest.getUserId() != null, "userId", pictureQueryRequest.getUserId());
		queryWrapper.eq(pictureQueryRequest.getReviewStatus() != null, "reviewStatus", pictureQueryRequest.getReviewStatus());
		queryWrapper.eq(pictureQueryRequest.getReviewMessage() != null, "reviewMessage", pictureQueryRequest.getReviewMessage());
		queryWrapper.eq(pictureQueryRequest.getReviewerId() != null, "reviewerId", pictureQueryRequest.getReviewerId());
		queryWrapper.eq(pictureQueryRequest.getSpaceId() != null, "spaceId", pictureQueryRequest.getSpaceId());
		if (pictureQueryRequest.isNullSpaceId()) {
			queryWrapper.isNull("spaceId");
		}
		// 指定排序字段
		String sortField = pictureQueryRequest.getSortField();
		String sortOrder = pictureQueryRequest.getSortOrder();
		if (StringUtils.isNotBlank(sortField)) {
			queryWrapper.orderBy(true, "ascend".equals(sortOrder), sortField);
		}
		return queryWrapper;
	}

	/**
	 * 获取图片信息
	 * @param picture  图片
	 * @return 图片信息
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
		return pictureVO;
	}

	/**
	 * 获取图片分页信息
	 * @param picturePage 图片分页
	 * @return 图片分页信息
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
		// 用户id列表
		List<Long> userIdList = pictures.stream().map(Picture::getUserId).collect(Collectors.toList());
		Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdList).stream().collect(Collectors.groupingBy(User::getId));
		List<PictureVO> pictureVOS = pictures.stream().map(picture -> {
			PictureVO pictureVO = PictureVO.objToVo(picture);
			Long userId = picture.getUserId();
			User user = userIdUserListMap.get(userId).get(0);
			pictureVO.setUser(userService.getUserVO(user));
			return pictureVO;
		}).collect(Collectors.toList());
		pictureVOPage.setRecords(pictureVOS);
		return pictureVOPage;
	}

	/**
	 * 删除图片
	 * @param deleteRequest 删除条件
	 * @param loginUser 登录用户
	 * @return 删除结果
	 */
	@Override
	public Boolean deletePicture(DeleteRequest deleteRequest, User loginUser) {
		if (deleteRequest == null) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR);
		}
		Long id = deleteRequest.getId();
		transactionTemplate.execute(transactionStatus -> {
			// 判断是否存在
			Picture picture = this.getById(id);
			if (picture == null) {
				throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
			}
			// 数据库删除
			boolean b = this.removeById(id);
			if (!b) {
				throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除图片失败");
			}
			// 更新空间额度
			Long spaceId = picture.getSpaceId();
			Space space = spaceService.getById(spaceId);
			if (space == null) {
				throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "空间不存在");
			}
			space.setTotalCount(space.getTotalCount() - 1);
			space.setTotalSize(space.getTotalSize() - picture.getPicSize());
			boolean update = spaceService.updateById(space);
			if (!update) {
				throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新空间额度失败");
			}
			// 删除本地缓存
			LOCAL_PICTURE_CACHE.invalidate(RedisConstant.PICTURE + id);
			// 删除redis
			stringRedisTemplate.delete(RedisConstant.PICTURE + id);
			return true;
		});
		return true;
	}

	/**
	 * 更新图片
	 *
	 * @param pictureUpdateRequest 修改图片请求
	 * @param loginUser 登录用户
	 * @return 修改图片结果
	 */
	@Override
	public PictureVO updatePicture(PictureUpdateRequest pictureUpdateRequest, User loginUser) {
		if (pictureUpdateRequest == null || pictureUpdateRequest.getId() == null || pictureUpdateRequest.getId() <= 0) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR);
		}
		Picture picture = new Picture();
		BeanUtils.copyProperties(pictureUpdateRequest, picture);
		picture.setTags(JSONUtil.toJsonStr(pictureUpdateRequest.getTags()));
		picture.setEditTime(new Date());
		picture.setUpdateTime(new Date());
		validPicture(picture);
		fillPictureReview(picture, loginUser);
		// 判断是否存在
		Picture oldPicture = this.getById(pictureUpdateRequest.getId());
		if (oldPicture == null) {
			throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
		}
		boolean b = this.updateById(picture);
		if (!b) {
			throw new BusinessException(ErrorCode.SYSTEM_ERROR, "修改图片失败");
		}
		// 删除本地缓存
		LOCAL_PICTURE_CACHE.invalidate(RedisConstant.PICTURE + pictureUpdateRequest.getId());
		// 删除redis
		stringRedisTemplate.delete(RedisConstant.PICTURE + pictureUpdateRequest.getId());
		return getPictureVO(picture);
	}

	/**
	 * 获取图片信息
	 * @param id 图片id
	 * @return 图片信息
	 */
	@Override
	public Picture getPictureById(Long id, User loginUser) {
		// 参数校验
		if (id == null) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR);
		}
		// 判断图片是否存在
		Picture picture = this.getById(id);
		if (picture == null) {
			return null;
		}
		// 判断图片是否属于某一个空间
		Long spaceId = picture.getSpaceId();
		if (spaceId != null) {
			Space space = spaceService.getById(spaceId);
			if (space == null) {
				throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "空间不存在");
			}
			// 获取空间类型
			SpaceTypeEnum spaceTypeEnum = SpaceTypeEnum.getEnumByValue(space.getSpaceType());
			if (spaceTypeEnum == null) {
				throw new BusinessException(ErrorCode.SYSTEM_ERROR, "空间级别不存在");
			}
			// 私有
			if (spaceTypeEnum.getValue() == SpaceTypeEnum.PRIVATE.getValue()) {
				if (!loginUser.getId().equals(space.getUserId())) {
					throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
				}
			} else {
				// 团队
				QueryWrapper<SpaceUser> queryWrapper = new QueryWrapper<>();
				queryWrapper.eq("spaceId", spaceId);
				queryWrapper.eq("userId", loginUser.getId());
				if (spaceUserService.count(queryWrapper) <= 0) {
					throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
				}
			}
		}
		return picture;
	}

	/**
	 * 获取图片信息
	 *
	 * @param id        图片id
	 * @param loginUser 登录用户
	 * @return 图片信息
	 */
	@Override
	public PictureVO getPictureVOById(Long id, User loginUser) {
		if (id == null || id <= 0) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR);
		}
		// 查本地缓存
		String localCacheKey = RedisConstant.PICTURE + id;
		String localPictureCacheStr = LOCAL_PICTURE_CACHE.getIfPresent(localCacheKey);
		if (localPictureCacheStr != null) {
			if (localPictureCacheStr.isEmpty()) {
				return null;
			}
			PictureVO pictureVO = GsonUtils.fromJson(localPictureCacheStr, PictureVO.class);
			Long spaceId = pictureVO.getSpaceId();
			Space space = spaceService.getById(spaceId);
			pictureVO.setPermissionList(spaceUserAuthManager.getPermissionList(space, loginUser));
			return pictureVO;
		}
		// 查redis缓存
		String cacheKey = RedisConstant.PICTURE + id;
		String pictureVOStr = stringRedisTemplate.opsForValue().get(cacheKey);
		// 命中缓存
		if (pictureVOStr != null) {
			// 如果是空字符串，则返回null
			if (pictureVOStr.isEmpty()) {
				return null;
			}
			// 写到本地缓存
			LOCAL_PICTURE_CACHE.put(localCacheKey, pictureVOStr);
			// 转换为对象
			PictureVO pictureVO = GsonUtils.fromJson(pictureVOStr, PictureVO.class);
			// 获取权限列表
			Long spaceId = pictureVO.getSpaceId();
			Space space = spaceService.getById(spaceId);
			pictureVO.setPermissionList(spaceUserAuthManager.getPermissionList(space, loginUser));
			return pictureVO;
		}
		// 缓存为空，从数据库查询
		Picture picture = getPictureById(id, loginUser);
		// 数据库也没有，返回null，缓存空字符串
		if (picture == null) {
			LOCAL_PICTURE_CACHE.put(localCacheKey, "");
			stringRedisTemplate.opsForValue().set(cacheKey, "", BASE_EXPIRE_SECONDS, TimeUnit.SECONDS);
			return null;
		}
		PictureVO pictureVODB = getPictureVO(picture);
		// 写缓存
		int expireSeconds = BASE_EXPIRE_SECONDS + RANDOM.nextInt(RANDOM_EXPIRE_RANGE);
		LOCAL_PICTURE_CACHE.put(localCacheKey, GsonUtils.toJson(pictureVODB));
		stringRedisTemplate.opsForValue().set(cacheKey, GsonUtils.toJson(pictureVODB), expireSeconds, TimeUnit.SECONDS);
		// 获取权限列表
		Long spaceId = picture.getSpaceId();
		Space space = spaceService.getById(spaceId);
		pictureVODB.setPermissionList(spaceUserAuthManager.getPermissionList(space, loginUser));
		return pictureVODB;
	}

	/**
	 * 获取图片列表
	 * @param pictureQueryRequest 查询条件
	 * @return 图片列表
	 */
	@Override
	public Page<Picture> listPicture(PictureQueryRequest pictureQueryRequest) {
		if (pictureQueryRequest == null) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR);
		}
		QueryWrapper<Picture> queryWrapper = getQueryWrapper(pictureQueryRequest);
		// 限制数量
		if (pictureQueryRequest.getPageSize() > 20) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR);
		}
		return this.page(new Page<>(pictureQueryRequest.getCurrent(), pictureQueryRequest.getPageSize()), queryWrapper);
	}

	/**
	 * 获取图片列表
	 * @param pictureQueryRequest 查询条件
	 * @return 图片列表
	 */
	@Override
	public Page<PictureVO> listPictureVO(PictureQueryRequest pictureQueryRequest, User loginUser) {
		// 参数校验
		if (pictureQueryRequest == null) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR);
		}
		Long userId = loginUser.getId();
		int pageSize = pictureQueryRequest.getPageSize();
		if (pageSize > 20) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数错误，不能超过20条");
		}
		// 是不是从空间中查图片
		Long spaceId = pictureQueryRequest.getSpaceId();
		if (spaceId == null) {
			// 公共图库 过审
			pictureQueryRequest.setReviewStatus((long) PictureReviewStatusEnum.PASS.getValue());
			pictureQueryRequest.setNullSpaceId(true);
		} else {
			Space space = spaceService.getById(spaceId);
			if (space == null) {
				throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "空间不存在");
			}
			// 获取空间类型
			Integer spaceType = space.getSpaceType();
			SpaceTypeEnum spaceTypeEnum = SpaceTypeEnum.getEnumByValue(spaceType);
			if (spaceTypeEnum == SpaceTypeEnum.PRIVATE) {
				// 私有空间 只有创建者才能访问
				if (!userId.equals(space.getUserId())) {
					throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无访问权限");
				}
			} else {
				// 团队空间
				QueryWrapper<SpaceUser> queryWrapper = new QueryWrapper<>();
				queryWrapper.eq("userId", userId);
				queryWrapper.eq("spaceId", spaceId);
				SpaceUser spaceUser = spaceUserService.getOne(queryWrapper);
				if (spaceUser == null) {
					throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无访问权限");
				}
			}
		}
		Page<Picture> picturePage = listPicture(pictureQueryRequest);
		return getPagePictureVO(picturePage);
	}

	/**
	 * 修改图片
	 * @param pictureEditRequest 修改图片请求
	 * @param loginUser 登录用户
	 * @return 修改图片结果
	 */
	@Override
	public PictureVO editPicture(PictureEditRequest pictureEditRequest, User loginUser) {
		if (pictureEditRequest == null || pictureEditRequest.getId() == null || pictureEditRequest.getId() <= 0) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR);
		}
		Long id = pictureEditRequest.getId();
		Picture oldPicture = this.getById(id);
		if (oldPicture == null) {
			throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
		}
		Picture picture = new Picture();
		BeanUtils.copyProperties(pictureEditRequest, picture);
		picture.setTags(JSONUtil.toJsonStr(pictureEditRequest.getTags()));
		picture.setEditTime(new Date());
		validPicture(picture);
		fillPictureReview(picture, loginUser);
		boolean b = this.updateById(picture);
		if (!b) {
			throw new BusinessException(ErrorCode.SYSTEM_ERROR, "修改图片失败");
		}
		// 删除本地缓存
		LOCAL_PICTURE_CACHE.invalidate(RedisConstant.PICTURE + id);
		// 删除redis
		stringRedisTemplate.delete(RedisConstant.PICTURE + id);
		return getPictureVO(this.getById(id));
	}

	/**
	 * 验证图片
	 * @param picture  图片
	 */
	@Override
	public void validPicture(Picture picture) {
		ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR);
		// 从对象中取值
		Long id = picture.getId();
		String url = picture.getUrl();
		String introduction = picture.getIntroduction();
		// 修改数据时，id 不能为空，有参数则校验
		ThrowUtils.throwIf(ObjUtil.isNull(id), ErrorCode.PARAMS_ERROR, "id 不能为空");
		if (StrUtil.isNotBlank(url)) {
			ThrowUtils.throwIf(url.length() > 1024, ErrorCode.PARAMS_ERROR, "url 过长");
		}
		if (StrUtil.isNotBlank(introduction)) {
			ThrowUtils.throwIf(introduction.length() > 800, ErrorCode.PARAMS_ERROR, "简介过长");
		}
	}

	/**
	 * 图片审核
	 * @param pictureReviewRequest 图片审核请求
	 * @param loginUser 登录用户
	 */
	@Override
	public boolean doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser) {
		if (pictureReviewRequest == null || pictureReviewRequest.getId() == null || pictureReviewRequest.getId() <= 0) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR);
		}
		Long id = pictureReviewRequest.getId();
		Integer reviewStatus = pictureReviewRequest.getReviewStatus();
		String reviewMessage = pictureReviewRequest.getReviewMessage();
		// 判断图片是否存在
		Picture picture = this.getById(id);
		if (picture == null) {
			throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
		}
		// 状态只能是待审核
		Long oldReviewStatus = picture.getReviewStatus();
		PictureReviewStatusEnum pictureReviewStatusEnum = PictureReviewStatusEnum.getEnumByValue(Math.toIntExact(oldReviewStatus));
		if (pictureReviewStatusEnum == null || !pictureReviewStatusEnum.equals(PictureReviewStatusEnum.REVIEWING)) {
			throw new BusinessException(ErrorCode.OPERATION_ERROR, "图片已审核");
		}
		// 提交的状态只能是通过或者拒绝
		PictureReviewStatusEnum reviewStatusEnum = PictureReviewStatusEnum.getEnumByValue(Math.toIntExact(reviewStatus));
		if ((reviewStatusEnum != PictureReviewStatusEnum.PASS && reviewStatusEnum != PictureReviewStatusEnum.REJECT)) {
			throw new BusinessException(ErrorCode.OPERATION_ERROR, "状态错误");
		}
		Picture updatePicture = new Picture();
		updatePicture.setReviewStatus(Long.valueOf(reviewStatus));
		updatePicture.setReviewMessage(reviewMessage);
		updatePicture.setReviewerId(loginUser.getId());
		updatePicture.setReviewTime(new Date());
		boolean b = this.updateById(updatePicture);
		if (!b) {
			throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片审核失败");
		}
		return true;
	}

	/**
	 * 图片批量上传 抓取
	 *
	 * @param pictureUploadByBatchRequest 图片批量上传请求
	 * @param loginUser                   登录用户
	 * @return 图片批量上传结果
	 */
	@Override
	public Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser) {
		String searchText = pictureUploadByBatchRequest.getSearchText();
		// 格式化数量
		Integer count = pictureUploadByBatchRequest.getCount();
		ThrowUtils.throwIf(count > 30, ErrorCode.PARAMS_ERROR, "最多 30 条");
		// 要抓取的地址
		String fetchUrl = String.format("https://cn.bing.com/images/async?q=%s&mmasync=1", searchText);
		Document document;
		try {
			document = Jsoup.connect(fetchUrl).get();
		} catch (IOException e) {
			log.error("获取页面失败", e);
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
				log.info("当前链接为空，已跳过: {}", fileUrl);
				continue;
			}
			// 处理图片上传地址，防止出现转义问题
			int questionMarkIndex = fileUrl.indexOf("?");
			if (questionMarkIndex > -1) {
				fileUrl = fileUrl.substring(0, questionMarkIndex);
			}
			// 上传图片
			PictureUploadRequest pictureUploadRequest = new PictureUploadRequest();
			String name = pictureUploadByBatchRequest.getNamePrefix();
			if (StrUtil.isBlank(name)) {
				name = searchText;
			}
			pictureUploadRequest.setPicName(name + (uploadCount + 1));
			try {
				PictureVO pictureVO = this.uploadPicture(pictureUploadRequest, fileUrl, loginUser);
				log.info("图片上传成功, id = {}", pictureVO.getId());
				uploadCount++;
			} catch (Exception e) {
				log.error("图片上传失败", e);
				continue;
			}
			if (uploadCount >= count) {
				break;
			}
		}
		return uploadCount;
	}
}