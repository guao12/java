-- ----------------------------
-- 用户权限进度表：让权限点跟进账号而不是浏览器
-- 说明：原先权限点只存浏览器 localStorage，换电脑/换浏览器即丢失；
--       此表按用户维度持久化，登录后由前端自动同步。
-- 执行方式：mysql -uroot -p123456 dragon_raja < sql/dragon_raja_clearance.sql
-- ----------------------------
USE `dragon_raja`;

CREATE TABLE IF NOT EXISTS `dr_user_clearance` (
  `id`             bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`        bigint       NOT NULL COMMENT 'dr_user.id',
  `account`        varchar(20)  NULL DEFAULT NULL COMMENT '冗余学号，便于排查',
  `points`         int          NOT NULL DEFAULT 0 COMMENT '累计权限点',
  `completed_json` longtext     NULL COMMENT '已完成任务编号数组 JSON',
  `records_json`   longtext     NULL COMMENT '已完成任务卡快照数组 JSON',
  `create_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_id` (`user_id` ASC) USING BTREE,
  INDEX `idx_account` (`account` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '用户权限进度（权限点 / 已完成任务）' ROW_FORMAT = DYNAMIC;
