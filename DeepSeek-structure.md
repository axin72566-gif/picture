# Picture Backend 项目架构图

## 项目概述

基于 **Spring Boot 2.7.6** 的图片管理系统后端，提供图片上传/下载、空间管理、协作编辑、用户关注等功能。

---

## 一、整体分层架构

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          表现层 (Presentation Layer)                      │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐   │
│  │ UserCtrl     │ │ PictureCtrl  │ │ SpaceCtrl    │ │ CommentCtrl  │   │
│  │ /user        │ │ /picture     │ │ /space       │ │ /comment     │   │
│  └──────────────┘ └──────────────┘ └──────────────┘ └──────────────┘   │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐                     │
│  │ SpaceUserCtrl│ │ NoticeCtrl   │ │ FollowCtrl   │                     │
│  │ /spaceUser   │ │ /notice      │ │ /user_follow │                     │
│  └──────────────┘ └──────────────┘ └──────────────┘                     │
├─────────────────────────────────────────────────────────────────────────┤
│                         切面 & 权限 (Cross-cutting)                       │
│  ┌──────────────────────┐  ┌──────────────────────────────────────┐    │
│  │ RoleCheckAspect      │  │ Sa-Token Space Auth (StpInterface)    │    │
│  │ (@RoleCheck AOP)     │  │ (@SaSpaceCheckPermission)             │    │
│  └──────────────────────┘  └──────────────────────────────────────┘    │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │ GlobalExceptionHandler (@RestControllerAdvice)                    │  │
│  └──────────────────────────────────────────────────────────────────┘  │
├─────────────────────────────────────────────────────────────────────────┤
│                          服务层 (Service Layer)                           │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐   │
│  │UserSvc   │ │PictureSvc│ │SpaceSvc  │ │CommentSvc│ │NoticeSvc │   │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘ └──────────┘   │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐                              │
│  │SpaceUser │ │LikeSvc   │ │FollowSvc │                              │
│  │Svc       │ │          │ │          │                              │
│  └──────────┘ └──────────┘ └──────────┘                              │
├─────────────────────────────────────────────────────────────────────────┤
│                          业务管理 (Manager Layer)                         │
│  ┌─────────────────────┐  ┌──────────────────────────────────────┐    │
│  │ Upload Managers      │  │ Picture Clear Managers                │    │
│  │ (Template Pattern)   │  │ (Scheduled Cleanup)                   │    │
│  │ ┌─────────────────┐  │  │ ┌────────────────────────────────┐   │    │
│  │ │FilePictureUpload│  │  │ │ PublicPictureClearManager      │   │    │
│  │ │UrlPictureUpload │  │  │ │ PrivatePictureClearManager     │   │    │
│  │ └─────────────────┘  │  │ │ TeamPictureClearManager        │   │    │
│  └─────────────────────┘  │ └────────────────────────────────┘   │    │
│  ┌─────────────────────┐  ┌──────────────────────────────────────┐    │
│  │ CosManager           │  │ WebSocket PictureEditHandler          │    │
│  │ (Tencent COS SDK)    │  │ + LMAX Disruptor Event System         │    │
│  └─────────────────────┘  └──────────────────────────────────────┘    │
├─────────────────────────────────────────────────────────────────────────┤
│                         持久层 (Persistence Layer)                        │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐   │
│  │UserMapper│ │PicMapper │ │SpaceMapper│ │ComMapper │ │NoticeMap │   │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘ └──────────┘   │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐                              │
│  │SpaceUser │ │LikeMapper│ │FollowMap │                              │
│  │Mapper    │ │          │ │          │                              │
│  └──────────┘ └──────────┘ └──────────┘                              │
│                    MyBatis-Plus (BaseMapper)                            │
├─────────────────────────────────────────────────────────────────────────┤
│                         基础设施 (Infrastructure)                         │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────────────────┐ │
│  │ MySQL    │  │ Redis    │  │ Caffeine │  │ Tencent COS (COS)    │ │
│  │picture DB│  │(缓存/会话)│  │(本地缓存)│  │(对象存储)             │ │
│  └──────────┘  └──────────┘  └──────────┘  └──────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 二、数据模型 ER 图

```
┌──────────────────────┐       ┌──────────────────────┐
│        User          │       │       Space          │
├──────────────────────┤       ├──────────────────────┤
│ id (PK) snowflake    │───┐   │ id (PK) auto         │
│ userAccount          │   │   │ spaceName            │
│ userPassword         │   │   │ spaceLevel (0/1/2)   │
│ userName             │   │   │ spaceType (0=私有)   │
│ userAvatar           │   │   │   (1=团队)           │
│ userProfile          │   │   │ maxSize / maxCount   │
│ userRole (user/admin)│   │   │ totalSize/totalCount │
│ isDelete             │   │   │ userId (FK)          │──┐
└──────────────────────┘   │   │ isDelete             │  │
          │                │   └──────────────────────┘  │
          │                │              │              │
          │                │   ┌──────────┴──────────┐   │
          │                │   │     SpaceUser       │   │
          │                │   ├─────────────────────┤   │
          │                │   │ id (PK) snowflake   │   │
          │                └───│ spaceId (FK)        │   │
          │                    │ userId (FK)         │───┘
          │                    │ spaceRole            │
          │                    │ (viewer/editor/admin)│
          │                    └──────────────────────┘
          │
          │         ┌──────────────────────┐
          │         │      Picture         │
          │         ├──────────────────────┤
          │         │ id (PK) snowflake    │
          ├─────────│ userId (FK)          │
          │         │ spaceId (FK)         │
          │         │ url / thumbnailUrl   │
          │         │ name / introduction  │
          │         │ category / tags      │
          │         │ reviewStatus         │
          │         │   (0=待审/1=通过/2=拒绝)│
          │         │ isDelete             │
          │         └──────────────────────┘
          │                   │
          │         ┌─────────┴──────────┐
          │         │                    │
          ▼         ▼                    ▼
┌──────────────────┐  ┌──────────────┐  ┌──────────────────┐
│    Comment       │  │ PictureLike  │  │    UserFollow    │
├──────────────────┤  ├──────────────┤  ├──────────────────┤
│ id (PK) snowflake│  │ id (PK) auto │  │ id (PK) auto     │
│ pictureId (FK)   │  │ userId (FK)  │  │ userId (FK)      │
│ userId (FK)      │  │ pictureId(FK)│  │ followUserId (FK)│
│ parentId (0=根)  │  │ createTime   │  │ createTime       │
│ content          │  └──────────────┘  └──────────────────┘
│ isDelete         │
└──────────────────┘

┌──────────────────────┐
│     SysNotice        │
├──────────────────────┤
│ id (PK) auto         │
│ userId (0=全站公告)   │
│ title / content      │
│ relatedId            │
│ isRead / readTime    │
│ isDeleted            │
└──────────────────────┘
```

---

## 三、权限认证架构

```
                          HTTP Request
                               │
                               ▼
                 ┌─────────────────────────┐
                 │   SaInterceptor          │
                 │   (Sa-Token Path Check)  │
                 └───────────┬─────────────┘
                             │
              ┌──────────────┴──────────────┐
              │                             │
              ▼                             ▼
    ┌─────────────────┐           ┌────────────────────┐
    │ @RoleCheck      │           │ @SaSpaceCheckPerm. │
    │ (AOP Aspect)    │           │ (Sa-Token Space)   │
    │                 │           │                    │
    │ 用户登录检查     │           │ 空间权限检查        │
    │ user / admin    │           │ viewer/editor/admin│
    └─────────────────┘           └────────────────────┘
              │                             │
              │                             ▼
              │              ┌─────────────────────────┐
              │              │ StpInterfaceImpl        │
              │              │ 解析URI+Body → 获取     │
              │              │ spaceId/pictureId       │
              │              │ → 查询角色 → 返回权限   │
              │              └─────────────────────────┘
              ▼                             ▼
    ┌─────────────────────────────────────────────┐
    │            Controller Method                │
    └─────────────────────────────────────────────┘
```

**权限配置** (spaceUserAuthConfig.json):

| 角色 | 权限 |
|------|------|
| **viewer** (浏览者) | `picture:view` |
| **editor** (编辑者) | `picture:view`, `picture:upload`, `picture:edit`, `picture:delete`, `picture:download` |
| **admin** (管理员) | editor 全部 + `spaceUser:manage` |

---

## 四、图片上传流程 (模板方法模式)

```
┌──────────────────────────────────────┐
│   PictureUploadTemplate (abstract)   │
│                                      │
│   uploadPicture(source, prefix) {    │
│     1. validPicture(source)     ───────── 子类实现
│     2. getOriginFilename(source) ───────── 子类实现
│     3. generateFilePath(prefix)       │
│     4. writeTempFile(source)    ───────── 子类实现
│     5. cos.putObject(key, file)       │
│     6. buildResult(...)               │
│     7. cleanTempFile(file)            │
│   }                                  │
└──────────────────────────────────────┘
         △                    △
         │                    │
┌────────┴────────┐  ┌───────┴────────┐
│FilePictureUpload│  │UrlPictureUpload│
│(MultipartFile)  │  │(URL download)  │
│max 2MB          │  │HEAD validate   │
│jpeg/jpg/png/webp│  │content-type    │
└─────────────────┘  └────────────────┘
         │                    │
         └──────────┬─────────┘
                    ▼
          ┌─────────────────┐
          │   CosManager    │
          │ Tencent COS SDK │
          │ 图片处理(万象)   │
          └─────────────────┘
```

---

## 五、WebSocket 协同编辑架构 (Disruptor 事件驱动)

```
┌──────────┐     ┌──────────┐     ┌──────────┐
│ Client A │     │ Client B │     │ Client C │
└────┬─────┘     └────┬─────┘     └────┬─────┘
     │                │                │
     │   ws://host/api/ws/picture/edit?pictureId=xxx      │
     │                │                │
     └────────────────┼────────────────┘
                      │
                      ▼
          ┌───────────────────────┐
          │ WsHandshakeInterceptor│  ← 校验 pictureId / 登录 / 空间权限
          └───────────┬───────────┘
                      │
                      ▼
          ┌───────────────────────────────────────┐
          │       PictureEditHandler              │
          │  (TextWebSocketHandler)               │
          │                                       │
          │  ConcurrentHashMap<pictureId, Session>│
          │  同一时刻只能一个用户编辑同一张图片     │
          │                                       │
          │  ENTER_EDIT → 进入编辑态               │
          │  EDIT_ACTION → 广播编辑操作            │
          │  EXIT_EDIT → 退出编辑态                │
          └───────────────────┬───────────────────┘
                              │
                              ▼ (异步事件)
          ┌───────────────────────────────────────┐
          │  PictureEditEventProducer             │
          │  → LMAX Disruptor RingBuffer          │
          │    (ringSize = 1024 × 256)            │
          └───────────────────┬───────────────────┘
                              │
                              ▼
          ┌───────────────────────────────────────┐
          │  PictureEditEventWorkHandler          │
          │  (Disruptor WorkHandler)              │
          │  → 按消息类型分发处理                  │
          └───────────────────────────────────────┘
```

---

## 六、定时清理策略

```
┌─────────────────────────────────────────────────────────────────┐
│                     定时任务调度 (3个Manager)                      │
├───────────────────┬──────────────────┬──────────────────────────┤
│ PublicClear       │ PrivateClear     │ TeamClear                │
│ (公共图片)         │ (私有空间)        │ (团队空间)               │
├───────────────────┼──────────────────┼──────────────────────────┤
│ 每天 3:00         │ 每天 2:00        │ 每天 2:30                │
│ └→ 清理审核拒绝   │ └→ 超限清理      │ └→ 审核拒绝清理          │
│                   │   (>=90%→<=80%) │                          │
│ 每周一 4:00       │                  │ 每天 3:30                │
│ └→ 冷数据清理     │ 每周日 3:00      │ └→ 超限清理              │
│   (<3浏览,>90天)  │ └→ 冷数据清理    │   (>=90%→<=80%)+通知    │
│                   │   (<2浏览,>180天)│                          │
│ 每月1/15日 0:00   │                  │                          │
│ └→ 大文件/超老清理 │                  │                          │
│  (总量>100万触发) │                  │                          │
└───────────────────┴──────────────────┴──────────────────────────┘
```

---

## 七、技术栈

| 类别 | 技术 | 版本 |
|------|------|------|
| 基础框架 | Spring Boot | 2.7.6 |
| ORM | MyBatis-Plus | 3.5.9 |
| 数据库 | MySQL | - |
| 缓存 | Redis + Caffeine | Caffeine 2.9.3 |
| 权限认证 | Sa-Token | 1.39.0 |
| 会话管理 | Spring Session Redis | - |
| 对象存储 | 腾讯云 COS SDK | 5.6.227 |
| 实时通信 | Spring WebSocket + LMAX Disruptor | Disruptor 3.4.2 |
| API文档 | Knife4j (Swagger) | 4.4.0 |
| 工具库 | Hutool | 5.8.43 |
| 构建工具 | Maven | - |
| Java版本 | JDK 1.8 | - |

---

## 八、模块依赖关系

```
picture-backend
├── controller/     ─── 依赖 ──▶  service/
│                                  │
├── aspect/         ─── 依赖 ──▶  service/ + annotation/
│
├── manager/        ─── 依赖 ──▶  mapper/ + model/ + config/
│   ├── auth/       ─── Sa-Token 空间权限
│   ├── upload/     ─── CosManager (模板方法模式)
│   ├── pictureClear/── 定时清理 (3个独立Manager)
│   └── websocket/  ─── Disruptor 事件驱动
│
├── service/impl/   ─── 依赖 ──▶  mapper/ + manager/
│
├── mapper/         ─── 依赖 ──▶  model/entity/ (MyBatis-Plus BaseMapper)
│
└── model/
    ├── entity/     ─── 8张数据库表映射
    ├── dto/        ─── 请求参数对象 (7组)
    ├── vo/         ─── 返回视图对象 (7个)
    └── Enum/       ─── 6个枚举类型
```
