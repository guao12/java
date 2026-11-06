-- ----------------------------
-- 权限进度表：随应用启动自动建（幂等）
--
-- 为什么放在这里：这张表原先只写在手工脚本 sql/dragon_raja_clearance.sql 里
-- （该脚本已移除，内容与本文件一致），换一台电脑初始化数据库时没人会想起来单独执行它 —— 表不存在，
-- /api/user/clearance 直接 500，前端又静默降级成本地存储，
-- 表现就是「换电脑权限就没了」。现在随启动自动补齐。
--
-- 只做 CREATE TABLE IF NOT EXISTS：不会 DROP、不会清理既有进度。
-- ----------------------------
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
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci
  COMMENT = '用户权限进度（权限点 / 已完成任务）' ROW_FORMAT = DYNAMIC;
