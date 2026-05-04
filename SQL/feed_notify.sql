-- Feed 流通知功能 DDL 变更脚本
-- 执行前请备份 sys_notice 表
-- 适用数据库：MySQL 5.7+

-- 1. 为 sys_notice 表新增 type 字段（通知类型）
--    对应 NoticeTypeEnum.value: like / comment / system / system_clear / picture_upload
ALTER TABLE `sys_notice`
    ADD COLUMN `type` VARCHAR(32) NULL DEFAULT NULL COMMENT '通知类型：like/comment/system/system_clear/picture_upload' AFTER `relatedId`;

-- 2. 为 type 字段建立索引，加速按类型查询
CREATE INDEX `idx_sys_notice_type` ON `sys_notice` (`type`);

-- 3. 为 userId + isRead 建立联合索引，加速未读通知查询（如已存在可跳过）
-- CREATE INDEX `idx_sys_notice_user_read` ON `sys_notice` (`userId`, `isRead`);

-- 说明：
--   Feed 流数据（收件箱/发件箱）完全存储在 Redis ZSet 中，无需新建数据库表。
--   RocketMQ Topic: feed-notify-topic（需在 Broker 端创建，或开启 autoCreateTopicEnable=true）

-- 4. 为 sys_notice 添加唯一索引，防止 Consumer 重试导致重复通知
--    对图片上传通知场景：userId > 0（具体用户），relatedId = pictureId（非空），type = 'picture_upload'（非空）
--    三字段均有值，唯一索引正常工作
ALTER TABLE `sys_notice`
    ADD UNIQUE INDEX `uk_user_related_type` (`userId`, `relatedId`, `type`);
