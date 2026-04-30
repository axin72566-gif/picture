-- 用户表
create table if not exists user
(
    id           bigint auto_increment comment 'id' primary key,
    userAccount  varchar(256)                           not null comment '账号',
    userPassword varchar(512)                           not null comment '密码',
    userName     varchar(256)                           null comment '用户昵称',
    userAvatar   varchar(1024)                          null comment '用户头像',
    userProfile  varchar(512)                           null comment '用户简介',
    userRole     varchar(256) default 'user'            not null comment '用户角色：user/admin',
    editTime     datetime     default CURRENT_TIMESTAMP not null comment '编辑时间',
    createTime   datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint      default 0                 not null comment '是否删除',
    UNIQUE KEY uk_userAccount (userAccount),
    INDEX idx_userName (userName)
) comment '用户' collate = utf8mb4_unicode_ci;

-- 图片表
create table if not exists picture
(
    id           bigint auto_increment comment 'id' primary key,
    url          varchar(512)                       not null comment '图片 url',
    name         varchar(128)                       not null comment '图片名称',
    introduction varchar(512)                       null comment '简介',
    category     varchar(64)                        null comment '分类',
    tags         varchar(512)                       null comment '标签（JSON 数组）',
    picSize      bigint                             null comment '图片体积',
    picWidth     int                                null comment '图片宽度',
    picHeight    int                                null comment '图片高度',
    picScale     double                             null comment '图片宽高比例',
    picFormat    varchar(32)                        null comment '图片格式',
    userId       bigint                             not null comment '创建用户 id',
    createTime   datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    editTime     datetime default CURRENT_TIMESTAMP not null comment '编辑时间',
    updateTime   datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint  default 0                 not null comment '是否删除',
    INDEX idx_name (name),                 -- 提升基于图片名称的查询性能
    INDEX idx_introduction (introduction), -- 用于模糊搜索图片简介
    INDEX idx_category (category),         -- 提升基于分类的查询性能
    INDEX idx_tags (tags),                 -- 提升基于标签的查询性能
    INDEX idx_userId (userId)              -- 提升基于用户 ID 的查询性能
) comment '图片' collate = utf8mb4_unicode_ci;

ALTER TABLE picture
    -- 添加新列
    ADD COLUMN reviewStatus  INT DEFAULT 0 NOT NULL COMMENT '审核状态：0-待审核; 1-通过; 2-拒绝',
    ADD COLUMN reviewMessage VARCHAR(512)  NULL COMMENT '审核信息',
    ADD COLUMN reviewerId    BIGINT        NULL COMMENT '审核人 ID',
    ADD COLUMN reviewTime    DATETIME      NULL COMMENT '审核时间';

-- 创建基于 reviewStatus 列的索引
CREATE INDEX idx_reviewStatus ON picture (reviewStatus);

ALTER TABLE picture
    -- 添加新列
    ADD COLUMN thumbnailUrl varchar(512) NULL COMMENT '缩略图 url';

-- 空间表
create table if not exists space
(
    id         bigint auto_increment comment 'id' primary key,
    spaceName  varchar(128)                       null comment '空间名称',
    spaceLevel int      default 0                 null comment '空间级别：0-普通版 1-专业版 2-旗舰版',
    maxSize    bigint   default 0                 null comment '空间图片的最大总大小',
    maxCount   bigint   default 0                 null comment '空间图片的最大数量',
    totalSize  bigint   default 0                 null comment '当前空间下图片的总大小',
    totalCount bigint   default 0                 null comment '当前空间下的图片数量',
    userId     bigint                             not null comment '创建用户 id',
    createTime datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    editTime   datetime default CURRENT_TIMESTAMP not null comment '编辑时间',
    updateTime datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete   tinyint  default 0                 not null comment '是否删除',
    -- 索引设计
    index idx_userId (userId),        -- 提升基于用户的查询效率
    index idx_spaceName (spaceName),  -- 提升基于空间名称的查询效率
    index idx_spaceLevel (spaceLevel) -- 提升按空间级别查询的效率
) comment '空间' collate = utf8mb4_unicode_ci;

-- 添加新列
ALTER TABLE picture
    ADD COLUMN spaceId bigint null comment '空间 id（为空表示公共空间）';

-- 创建索引
CREATE INDEX idx_spaceId ON picture (spaceId);

ALTER TABLE space
    ADD COLUMN spaceType int default 0 not null comment '空间类型：0-私有 1-团队';

CREATE INDEX idx_spaceType ON space (spaceType);

-- 空间成员表
create table if not exists space_user
(
    id         bigint auto_increment comment 'id' primary key,
    spaceId    bigint                                 not null comment '空间 id',
    userId     bigint                                 not null comment '用户 id',
    spaceRole  varchar(128) default 'viewer'          null comment '空间角色：viewer/editor/admin',
    createTime datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    -- 索引设计
    UNIQUE KEY uk_spaceId_userId (spaceId, userId), -- 唯一索引，用户在一个空间中只能有一个角色
    INDEX idx_spaceId (spaceId),                    -- 提升按空间查询的性能
    INDEX idx_userId (userId)                       -- 提升按用户查询的性能
) comment '空间用户关联' collate = utf8mb4_unicode_ci;

-- 添加新列
alter table picture
    add viewCount int default 0 not null comment '访问次数';

-- 添加点赞数列
ALTER TABLE picture
    ADD COLUMN likeCount bigint DEFAULT 0 NOT NULL COMMENT '点赞数';

-- 图片点赞关联表
CREATE TABLE IF NOT EXISTS `picture_like`
(
    `id`         bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `userId`     bigint          NOT NULL COMMENT '点赞用户ID',
    `pictureId`  bigint          NOT NULL COMMENT '被点赞图片ID',
    `createTime` datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_userId_pictureId` (`userId`, `pictureId`),
    INDEX `idx_pictureId` (`pictureId`),
    INDEX `idx_userId` (`userId`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT '图片点赞关联表';

CREATE TABLE `sys_notice` (
                              `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID，自增',
                              `userId` bigint NOT NULL COMMENT '接收通知的用户ID（系统公告填0表示全体用户）',
                              `title` varchar(100) DEFAULT '' COMMENT '通知标题（系统公告必填，互动通知可选）',
                              `content` varchar(500) NOT NULL COMMENT '通知内容（如：XXX点赞了你的文章）',
                              `relatedId` bigint DEFAULT 0 COMMENT '关联业务ID（如点赞的文章ID、评论ID）',
                              `isRead` tinyint NOT NULL DEFAULT 0 COMMENT '阅读状态：0-未读 1-已读',
                              `readTime` datetime DEFAULT NULL COMMENT '阅读时间（标记已读时更新）',
                              `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '通知创建时间',
                              `updateTime` datetime DEFAULT NULL COMMENT '删除时间（软删除时更新）',
                              `isDeleted` tinyint NOT NULL DEFAULT 0 COMMENT '删除状态：0-正常 1-已删除（软删除）',
                              PRIMARY KEY (`id`),
    -- 核心索引：优化「按用户查未读通知、按时间排序」的高频查询
                              KEY `idx_user_read_time` (`userId`,`isRead`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户通知主表';

-- 图片评论表
CREATE TABLE IF NOT EXISTS `comment`
(
    `id`         bigint       NOT NULL COMMENT '评论ID（雪花算法）',
    `pictureId`  bigint       NOT NULL COMMENT '所属图片ID',
    `userId`     bigint       NOT NULL COMMENT '评论者用户ID',
    `parentId`   bigint       NOT NULL DEFAULT 0 COMMENT '父评论ID，0表示一级评论',
    `content`    varchar(1000) NOT NULL COMMENT '评论内容',
    `createTime` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete`   tinyint      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常 1-已删除',
    PRIMARY KEY (`id`),
    INDEX `idx_pictureId` (`pictureId`),
    INDEX `idx_userId` (`userId`),
    INDEX `idx_parentId` (`parentId`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT '图片评论表';