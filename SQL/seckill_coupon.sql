-- 秒杀优惠券功能 DDL 变更脚本
-- 新建 seckill_activity 秒杀活动表 和 coupon 用户优惠券表

-- 1. 秒杀活动表
CREATE TABLE IF NOT EXISTS `seckill_activity`
(
    `id`          bigint         NOT NULL COMMENT '活动ID（雪花算法）',
    `name`        varchar(64)    NOT NULL COMMENT '活动名称',
    `totalStock`  int            NOT NULL COMMENT '总库存',
    `remainStock` int            NOT NULL COMMENT '剩余库存',
    `faceValue`   decimal(10, 2) NOT NULL COMMENT '券面值（元）',
    `salePrice`   decimal(10, 2) NOT NULL COMMENT '秒杀售价（元）',
    `startTime`   datetime       NOT NULL COMMENT '活动开始时间',
    `endTime`     datetime       NOT NULL COMMENT '活动结束时间',
    `createTime`  datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime`  datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete`    tinyint        NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常 1-已删除',
    PRIMARY KEY (`id`),
    INDEX `idx_startTime` (`startTime`),
    INDEX `idx_endTime` (`endTime`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT '秒杀活动表';

-- 2. 用户优惠券表
CREATE TABLE IF NOT EXISTS `coupon`
(
    `id`           bigint         NOT NULL COMMENT '优惠券ID（雪花算法）',
    `userId`       bigint         NOT NULL COMMENT '所属用户ID',
    `activityId`   bigint         NOT NULL COMMENT '来源秒杀活动ID',
    `couponNo`     varchar(32)    NOT NULL COMMENT '优惠券编号（系统唯一）',
    `faceValue`    decimal(10, 2) NOT NULL COMMENT '券面值（元）',
    `status`       varchar(16)    NOT NULL DEFAULT 'UNUSED' COMMENT '状态：UNUSED-未使用 USED-已使用 EXPIRED-已过期',
    `useTime`      datetime       NULL COMMENT '使用时间',
    `expireTime`   datetime       NULL COMMENT '过期时间（默认发放后30天）',
    `createTime`   datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime`   datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete`     tinyint        NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_couponNo` (`couponNo`),
    INDEX `idx_userId` (`userId`),
    INDEX `idx_status` (`status`),
    INDEX `idx_activityId` (`activityId`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT '用户优惠券表';
