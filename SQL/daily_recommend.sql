-- 每日推荐表（持久化兜底，Redis 过期后可从此表恢复）
CREATE TABLE IF NOT EXISTS `daily_recommend`
(
    `id`            bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `recommendDate` date            NOT NULL COMMENT '推荐日期（展示日期，非生成日期）',
    `pictureIds`    text            NOT NULL COMMENT '推荐图片ID列表（JSON数组）',
    `pictureData`   mediumtext      NOT NULL COMMENT '完整PictureVO JSON数据',
    `createTime`    datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime`    datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_recommendDate` (`recommendDate`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT '每日推荐图片持久化表';
