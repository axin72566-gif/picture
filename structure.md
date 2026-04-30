# picture-backend 项目结构说明

## 一、项目基本信息

| 属性 | 值 |
|---|---|
| **GroupId** | `com.axin` |
| **ArtifactId** | `picture-backend` |
| **版本** | `0.0.1-SNAPSHOT` |
| **Java 版本** | JDK 1.8 |
| **Spring Boot 版本** | 2.7.6 |
| **服务端口** | 8123 |
| **Context Path** | `/api` |

---

## 二、主要技术栈

| 技术 | 说明 |
|---|---|
| Spring Boot 2.7.6 | 应用框架 |
| MyBatis-Plus 3.5.9 | ORM 框架，含分页插件 |
| MySQL | 关系型数据库 |
| Redis + Spring Session | 会话存储 & 缓存 |
| Caffeine | 本地内存缓存 |
| Sa-Token 1.39.0 | 权限认证框架（多账号体系） |
| 腾讯云 COS | 对象存储（图片文件） |
| WebSocket | 图片协同编辑实时通信 |
| Disruptor 3.4.2 | 高性能无锁队列（WebSocket 消息处理） |
| Knife4j 4.4.0 | 接口文档（基于 Swagger2） |
| Jsoup | HTML 解析（批量抓取图片） |
| Hutool | Java 工具库 |
| Gson | JSON 序列化工具 |
| Lombok | 代码简化 |

---

## 三、完整目录结构

```
picture-backend/
├── pom.xml                                              # Maven 项目配置
├── structure.md                                         # 本文件（项目结构说明）
├── SQL/
│   └── create_tables.sql                                # 数据库建表脚本
└── src/
    ├── main/
    │   ├── java/com/axin/picturebackend/
    │   │   ├── PictureBackendApplication.java           # 主启动类
    │   │   │
    │   │   ├── annotation/                              # 自定义注解
    │   │   │   └── RoleCheck.java                       # 角色校验注解
    │   │   │
    │   │   ├── aspect/                                  # AOP 切面
    │   │   │   └── RoleCheckAspect.java                 # 角色校验切面
    │   │   │
    │   │   ├── common/                                  # 公共响应封装
    │   │   │   ├── BaseResponse.java                    # 统一响应体
    │   │   │   ├── DeleteRequest.java                   # 通用删除请求
    │   │   │   ├── PageRequest.java                     # 通用分页请求
    │   │   │   └── ResultUtils.java                     # 响应构造工具类
    │   │   │
    │   │   ├── config/                                  # 配置类
    │   │   │   ├── CorsConfig.java                      # 跨域配置
    │   │   │   ├── CosClientConfig.java                 # 腾讯云 COS 客户端配置
    │   │   │   ├── HttpRequestWrapperFilter.java        # HTTP 请求包装过滤器
    │   │   │   ├── JsonConfig.java                      # Jackson Long精度丢失配置
    │   │   │   ├── MyBatisPlusConfig.java               # MP 分页拦截器配置
    │   │   │   ├── PictureEditEventDisruptorConfig.java # Disruptor 无锁队列配置
    │   │   │   ├── RequestWrapper.java                  # 请求体可重复读包装类
    │   │   │   └── WebSocketConfig.java                 # WebSocket 处理器注册
    │   │   │
    │   │   ├── constant/                                # 常量类
    │   │   │   ├── PictureConstant.java                 # 图片相关常量
    │   │   │   ├── RedisConstant.java                   # Redis Key 前缀常量
    │   │   │   └── UserConstant.java                    # 用户角色常量
    │   │   │
    │   │   ├── controller/                              # REST 接口控制器
    │   │   │   ├── PictureController.java               # 图片接口（上传/编辑/查询/审核/点赞）
    │   │   │   ├── SpaceController.java                 # 空间接口（创建/编辑/查询/删除）
    │   │   │   ├── SpaceUserController.java             # 空间成员接口（添加/编辑/查询/删除成员）
    │   │   │   ├── UserController.java                  # 用户接口（注册/登录/注销/管理）
    │   │   │   ├── CommentController.java               # 评论接口（发布/查询/删除）
    │   │   │   └── SysNoticeController.java             # 系统通知接口（查询/标记已读）
    │   │   │
    │   │   ├── exception/                               # 异常处理
    │   │   │   ├── BusinessException.java               # 自定义业务异常
    │   │   │   ├── ErrorCode.java                       # 错误码枚举
    │   │   │   ├── GlobalExceptionHandler.java          # 全局异常处理器（@RestControllerAdvice）
    │   │   │   └── ThrowUtils.java                      # 条件抛异常工具类
    │   │   │
    │   │   ├── manager/                                 # 核心业务管理器
    │   │   │   ├── CosManager.java                      # 腾讯云 COS 文件操作（上传/删除）
    │   │   │   │
    │   │   │   ├── auth/                                # 权限认证模块（Sa-Token）
    │   │   │   │   ├── SaSpaceCheckPermission.java      # 空间权限校验注解（AOP）
    │   │   │   │   ├── SaTokenConfig.java               # Sa-Token 全局配置
    │   │   │   │   ├── SpaceUserAuthManager.java        # 空间用户权限解析管理器
    │   │   │   │   ├── StpInterfaceImpl.java            # Sa-Token 权限接口实现
    │   │   │   │   ├── StpKit.java                      # Sa-Token 多账号体系工具类
    │   │   │   │   └── model/                           # 权限数据模型
    │   │   │   │       ├── SpaceUserAuthConfig.java     # 角色-权限映射配置
    │   │   │   │       ├── SpaceUserAuthContext.java    # 权限校验上下文
    │   │   │   │       ├── SpaceUserPermission.java     # 权限实体
    │   │   │   │       ├── SpaceUserPermissionConstant.java # 权限字符串常量
    │   │   │   │       └── SpaceUserRole.java           # 角色实体
    │   │   │   │
    │   │   │   ├── pictureClear/                        # 图片定时清理模块
    │   │   │   │   ├── PublicPictureClearManager.java   # 公共图库定时清理（审核拒绝/过期图片）
    │   │   │   │   ├── PrivatePictureClearManager.java  # 私有空间定时清理（超容量淘汰/冷门清理）
    │   │   │   │   └── TeamPictureClearManager.java     # 团队空间定时清理
    │   │   │   │
    │   │   │   ├── sharding/                            # 分库分表模块（当前已注释/禁用）
    │   │   │   │   ├── DynamicShardingManager.java      # 动态分表管理器
    │   │   │   │   └── PictureShardingAlgorithm.java    # 自定义分片算法（按 spaceId）
    │   │   │   │
    │   │   │   ├── upload/                              # 图片上传模块（模板方法模式）
    │   │   │   │   ├── PictureUploadTemplate.java       # 上传抽象模板（校验/解析/上传流程）
    │   │   │   │   ├── FilePictureUpload.java           # 文件上传实现
    │   │   │   │   └── UrlPictureUpload.java            # URL 上传实现
    │   │   │   │
    │   │   │   └── websocket/                           # WebSocket 协同编辑模块
    │   │   │       ├── PictureEditHandler.java          # 图片编辑 WebSocket 消息处理器
    │   │   │       ├── WsHandshakeInterceptor.java      # WebSocket 握手拦截器（身份校验）
    │   │   │       ├── disruptor/                       # Disruptor 消息队列（异步解耦）
    │   │   │       │   ├── PictureEditEvent.java        # 事件数据体
    │   │   │       │   ├── PictureEditEventProducer.java  # 事件生产者
    │   │   │       │   └── PictureEditEventWorkHandler.java # 事件消费处理器
    │   │   │       └── model/                           # WebSocket 消息模型
    │   │   │           ├── PictureEditActionEnum.java   # 编辑动作枚举
    │   │   │           ├── PictureEditMessageTypeEnum.java # 消息类型枚举
    │   │   │           ├── PictureEditRequestMessage.java  # 客户端请求消息
    │   │   │           └── PictureEditResponseMessage.java # 服务端响应消息
    │   │   │
    │   │   ├── mapper/                                  # MyBatis-Plus Mapper 接口
    │   │   │   ├── PictureMapper.java                   # 图片数据访问
    │   │   │   ├── PictureLikeMapper.java               # 图片点赞数据访问
    │   │   │   ├── SpaceMapper.java                     # 空间数据访问
    │   │   │   ├── SpaceUserMapper.java                 # 空间成员数据访问
    │   │   │   ├── SysNoticeMapper.java                 # 系统通知数据访问
    │   │   │   ├── CommentMapper.java                   # 评论数据访问
    │   │   │   └── UserMapper.java                      # 用户数据访问
    │   │   │
    │   │   ├── model/                                   # 数据模型层
    │   │   │   ├── entity/                              # 数据库实体类
    │   │   │   │   ├── User.java                        # 用户
    │   │   │   │   ├── Picture.java                     # 图片
    │   │   │   │   ├── Space.java                       # 空间
    │   │   │   │   ├── SpaceUser.java                   # 空间-用户关联
    │   │   │   │   ├── SysNotice.java                   # 系统通知
    │   │   │   │   ├── Comment.java                     # 评论
    │   │   │   │   └── PictureLike.java                 # 图片点赞关联
    │   │   │   │
    │   │   │   ├── vo/                                  # 视图对象（返回给前端）
    │   │   │   │   ├── UserVO.java                      # 用户视图
    │   │   │   │   ├── LoginUserVO.java                 # 登录用户视图
    │   │   │   │   ├── PictureVO.java                   # 图片视图
    │   │   │   │   ├── SpaceVO.java                     # 空间视图
    │   │   │   │   ├── SpaceUserVO.java                 # 空间成员视图
    │   │   │   │   ├── NoticeVO.java                    # 通知视图
    │   │   │   │   └── CommentVO.java                   # 评论视图
    │   │   │   │
    │   │   │   ├── dto/                                 # 请求数据传输对象
    │   │   │   │   ├── File/
    │   │   │   │   │   └── UploadPictureResult.java     # 上传图片结果 DTO
    │   │   │   │   ├── picture/
    │   │   │   │   │   ├── PictureUploadRequest.java    # 图片上传请求
    │   │   │   │   │   ├── PictureUploadByBatchRequest.java # 批量抓取上传请求
    │   │   │   │   │   ├── PictureEditRequest.java      # 图片编辑请求
    │   │   │   │   │   ├── PictureUpdateRequest.java    # 图片更新请求（管理员）
    │   │   │   │   │   ├── PictureQueryRequest.java     # 图片查询请求
    │   │   │   │   │   ├── PictureReviewRequest.java    # 图片审核请求
    │   │   │   │   │   ├── PictureLikeRequest.java      # 图片点赞请求
    │   │   │   │   │   └── PictureTagCategory.java      # 图片标签分类数据
    │   │   │   │   ├── space/
    │   │   │   │   │   ├── SpaceAddRequest.java         # 创建空间请求
    │   │   │   │   │   ├── SpaceEditRequest.java        # 编辑空间请求
    │   │   │   │   │   ├── SpaceUpdateRequest.java      # 更新空间请求（管理员）
    │   │   │   │   │   ├── SpaceQueryRequest.java       # 空间查询请求
    │   │   │   │   │   └── SpaceLevel.java              # 空间级别数据
    │   │   │   │   ├── spaceuser/
    │   │   │   │   │   ├── SpaceUserAddRequest.java     # 添加成员请求
    │   │   │   │   │   ├── SpaceUserEditRequest.java    # 编辑成员请求
    │   │   │   │   │   └── SpaceUserQueryRequest.java   # 成员查询请求
    │   │   │   │   ├── user/
    │   │   │   │   │   ├── UserRegisterRequest.java     # 用户注册请求
    │   │   │   │   │   ├── UserLoginRequest.java        # 用户登录请求
    │   │   │   │   │   ├── UserAddRequest.java          # 添加用户请求（管理员）
    │   │   │   │   │   ├── UserUpdateRequest.java       # 更新用户请求
    │   │   │   │   │   └── UserQueryRequest.java        # 用户查询请求
    │   │   │   │   ├── comment/
    │   │   │   │   │   ├── CommentAddRequest.java       # 发布评论请求
    │   │   │   │   │   └── CommentQueryRequest.java     # 查询评论请求
    │   │   │   │   └── notice/
    │   │   │   │       ├── NoticeQueryRequest.java      # 查询通知请求
    │   │   │   │       └── NoticeReadRequest.java       # 标记已读请求
    │   │   │   │
    │   │   │   └── Enum/                                # 枚举类
    │   │   │       ├── UserRoleEnum.java                # 用户角色枚举
    │   │   │       ├── PictureReviewStatusEnum.java     # 图片审核状态枚举
    │   │   │       ├── SpaceLevelEnum.java              # 空间级别枚举
    │   │   │       ├── SpaceTypeEnum.java               # 空间类型枚举
    │   │   │       ├── SpaceRoleEnum.java               # 空间角色枚举
    │   │   │       └── NoticeTypeEnum.java              # 通知类型枚举
    │   │   │
    │   │   ├── service/                                 # 业务逻辑接口
    │   │   │   ├── UserService.java
    │   │   │   ├── PictureService.java
    │   │   │   ├── PictureLikeService.java
    │   │   │   ├── SpaceService.java
    │   │   │   ├── SpaceUserService.java
    │   │   │   ├── CommentService.java
    │   │   │   ├── SysNoticeService.java
    │   │   │   └── impl/                                # 业务逻辑实现类
    │   │   │       ├── UserServiceImpl.java             # 用户服务实现
    │   │   │       ├── PictureServiceImpl.java          # 图片服务实现（最核心，含缓存/审核/批量抓取）
    │   │   │       ├── PictureLikeServiceImpl.java      # 图片点赞服务实现（Redis计数+异步持久化）
    │   │   │       ├── SpaceServiceImpl.java            # 空间服务实现
    │   │   │       ├── SpaceUserServiceImpl.java        # 空间成员服务实现
    │   │   │       ├── CommentServiceImpl.java          # 评论服务实现（树形结构）
    │   │   │       └── SysNoticeServiceImpl.java        # 系统通知服务实现
    │   │   │
    │   │   └── utils/                                   # 工具类
    │   │       └── GsonUtils.java                       # Gson 序列化工具
    │   │
    │   └── resources/
    │       ├── application.yml                          # 主配置文件
    │       ├── application-local.yml                    # 本地环境配置（COS 密钥等）
    │       ├── biz/
    │       │   └── spaceUserAuthConfig.json             # 空间角色-权限映射配置文件
    │       └── mapper/                                  # MyBatis XML 映射文件
    │           ├── PictureMapper.xml
    │           ├── PictureLikeMapper.xml
    │           ├── SpaceMapper.xml
    │           ├── SpaceUserMapper.xml
    │           ├── SysNoticeMapper.xml
    │           └── UserMapper.xml
    │
    └── test/
        └── java/com/axin/picturebackend/
            └── PictureBackendApplicationTests.java      # 单元测试入口
```

---

## 四、数据库表结构

| 表名 | 说明 | 核心字段 |
|---|---|---|
| `user` | 用户表 | id、userAccount、userPassword、userName、userAvatar、userRole、isDelete |
| `picture` | 图片表 | id、url、thumbnailUrl、name、category、tags、spaceId、userId、reviewStatus、viewCount、likeCount、isDelete |
| `space` | 空间表 | id、spaceName、spaceLevel、spaceType（0私有/1团队）、maxSize、maxCount、totalSize、totalCount、userId |
| `space_user` | 空间-用户关联表 | id、spaceId、userId、spaceRole（viewer/editor/admin） |
| `picture_like` | 图片点赞关联表 | id、userId、pictureId、createTime |
| `sys_notice` | 系统通知表 | id、userId、title、content、relatedId、isRead、readTime、isDeleted |
| `comment` | 图片评论表 | id（雪花）、pictureId、userId、parentId（0=一级评论）、content、isDelete |

---

## 五、核心功能模块说明

### 5.1 用户模块 (`/api/user`)

| 接口 | 方法 | 权限 | 说明 |
|---|---|---|---|
| `/register` | POST | 公开 | 用户注册 |
| `/login` | POST | 公开 | 用户登录 |
| `/logout` | POST | 登录 | 用户注销 |
| `/get/login` | GET | 登录 | 获取当前登录用户 |
| `/delete` | POST | admin | 删除用户 |
| `/list/page/vo` | GET | admin | 分页查询用户列表 |

### 5.2 图片模块 (`/api/picture`)

| 接口 | 方法 | 权限 | 说明 |
|---|---|---|---|
| `/upload` | POST | 空间上传权限 | 文件方式上传图片 |
| `/upload/url` | POST | 空间上传权限 | URL 方式上传图片 |
| `/upload/batch` | POST | admin | 批量抓取上传图片 |
| `/delete` | POST | 空间删除权限 | 删除图片 |
| `/edit` | POST | 空间编辑权限 | 编辑图片（普通用户） |
| `/update` | POST | admin | 更新图片（管理员） |
| `/get/vo` | GET | 登录 | 获取图片 VO |
| `/list/page/vo` | POST | 登录 | 分页获取图片 VO 列表 |
| `/get` | GET | admin | 获取图片原始信息 |
| `/list/page` | POST | admin | 分页获取图片列表 |
| `/tag_category` | GET | 公开 | 获取图片标签和分类 |
| `/review` | POST | admin | 图片审核 |
| `/like` | POST | 登录 | 点赞 / 取消点赞 |

### 5.3 空间模块 (`/api/space`)

- 创建空间（自动初始化配额，团队空间自动添加创建者为管理员）
- 编辑/更新/删除空间
- 分页查询空间列表
- 获取空间级别配额信息

### 5.4 空间成员模块 (`/api/spaceUser`)

- 添加/编辑/删除空间成员
- 查询空间成员列表
- 获取当前用户在空间中的权限信息

### 5.5 评论模块 (`/api/comment`)

- 发布评论（支持一级评论和回复）
- 查询图片下的评论列表（树形结构）
- 删除评论

### 5.6 系统通知模块 (`/api/notice`)

- 查询当前用户的通知列表
- 标记通知为已读
- 获取未读通知数量

---

## 六、核心设计亮点

### 6.1 权限控制（双层）

- **角色权限**：`@RoleCheck` 注解 + `RoleCheckAspect` AOP 切面，用于校验 `user/admin` 角色
- **空间权限**：`@SaSpaceCheckPermission` 注解 + Sa-Token 多账号体系，细化到 `viewer/editor/admin` 三种空间角色，支持 `PICTURE_VIEW / PICTURE_UPLOAD / PICTURE_EDIT / PICTURE_DELETE` 等粒度的权限控制

### 6.2 图片上传（模板方法模式）

`PictureUploadTemplate` 定义上传流程骨架（校验 → 下载/读取 → 解析元信息 → 上传 COS → 入库），`FilePictureUpload` 和 `UrlPictureUpload` 分别实现文件和 URL 两种上传方式。

### 6.3 缓存策略（多级缓存）

`PictureServiceImpl` 中结合 **Caffeine 本地缓存** 和 **Redis 分布式缓存** 实现图片列表的多级缓存，提升查询性能，并用 Redis 存储图片浏览次数（`picture:view:{id}`）和点赞数（`picture:like:{id}`）。

### 6.4 图片协同编辑（WebSocket + Disruptor）

- `WsHandshakeInterceptor`：握手时校验用户身份和图片权限
- `PictureEditHandler`：维护每张图片的编辑会话池（`ConcurrentHashMap`），处理进入编辑、编辑动作广播、退出编辑等消息
- `Disruptor` 无锁队列：解耦 WebSocket 消息接收与处理，`PictureEditEventProducer` 生产消息，`PictureEditEventWorkHandler` 异步消费

### 6.5 图片定时清理

通过 `@Scheduled` 定时任务，对三类空间分别进行清理：

- **公共图库**（`PublicPictureClearManager`）：清理审核拒绝及长期待审核的图片
- **私有空间**（`PrivatePictureClearManager`）：超容量时淘汰最旧图片；清理 180 天未访问且体积 ≤1MB 的冷门图片
- **团队空间**（`TeamPictureClearManager`）：同私有空间策略，同时发送系统通知

### 6.6 批量图片抓取

`PictureServiceImpl.uploadPictureByBatch()` 使用 Jsoup 爬取指定关键词的必应图片搜索结果，批量入库公共图库（仅管理员可用）。

---

## 七、配置文件说明

### application.yml（主配置）

```yaml
server:
  port: 8123
  servlet:
    context-path: /api

spring:
  datasource:
    url: jdbc:mysql://192.168.60.131:3306/picture
  redis:
    host: 192.168.60.131
    port: 6379
  session:
    store-type: redis       # Session 存储于 Redis
  servlet:
    multipart:
      max-file-size: 10MB   # 最大上传文件大小

mybatis-plus:
  global-config:
    db-config:
      logic-delete-field: isDelete  # 逻辑删除字段
```

### application-local.yml（本地配置）

```yaml
cos:
  client:
    host: https://picture-1397275457.cos.ap-nanjing.myqcloud.com
    region: ap-nanjing
    bucket: picture-1397275457
    secretId: ***
    secretKey: ***
```

### spaceUserAuthConfig.json（权限配置）

定义三种空间角色（`viewer/editor/admin`）各自拥有的操作权限列表，由 `SpaceUserAuthManager` 在启动时加载。

---

## 八、Redis Key 规范

| Key | 说明 |
|---|---|
| `picture:{queryCondition}` | 图片列表查询缓存 |
| `picture:view:{pictureId}` | 图片浏览次数计数器 |
| `picture:like:{pictureId}` | 图片点赞数计数器 |
