-- 会员功能 DDL 变更脚本
-- 新建 orders 订单表 和 user_vip 用户会员表

-- 1. 订单表
CREATE TABLE IF NOT EXISTS `orders`
(
    `id`          bigint       NOT NULL COMMENT '订单ID（雪花算法）',
    `orderNo`     varchar(32)  NOT NULL COMMENT '订单编号（系统生成唯一）',
    `userId`      bigint       NOT NULL COMMENT '下单用户ID',
    `productType` varchar(16)  NOT NULL COMMENT '商品类型：MONTH_VIP',
    `amount`      decimal(10, 2) NOT NULL COMMENT '订单金额（实付金额）',
    `originalAmount` decimal(10, 2) NULL COMMENT '原价（未抵扣前）',
    `couponDiscount` decimal(10, 2) NULL COMMENT '优惠券抵扣金额',
    `couponId`    bigint       NULL COMMENT '使用的优惠券ID（可为空）',
    `status`      varchar(16)  NOT NULL DEFAULT 'PENDING' COMMENT '订单状态：PENDING-待支付 PAID-已支付 CANCELLED-已取消',
    `payTime`     datetime     NULL COMMENT '支付时间',
    `createTime`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete`    tinyint      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_orderNo` (`orderNo`),
    INDEX `idx_userId` (`userId`),
    INDEX `idx_status` (`status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT '订单表';

-- 2. 用户会员表
CREATE TABLE IF NOT EXISTS `user_vip`
(
    `id`         bigint   NOT NULL COMMENT '主键ID（雪花算法）',
    `userId`     bigint   NOT NULL COMMENT '用户ID',
    `vipLevel`   int      NOT NULL DEFAULT 1 COMMENT 'VIP等级：1-专业版',
    `expireTime` datetime NOT NULL COMMENT 'VIP到期时间',
    `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete`   tinyint  NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_userId` (`userId`),
    INDEX `idx_expireTime` (`expireTime`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT '用户会员表';
