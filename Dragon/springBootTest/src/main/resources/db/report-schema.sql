-- ----------------------------
-- 举报记录（幂等）
--
-- 为什么单独建表，而不是复用 dr_thread_action：
-- 1. 举报要能指向「某一层楼」，而 action 的唯一键是 (thread_id, user_id, type)，
--    同一个人无法对同一帖里的两层楼各举报一次；
-- 2. 举报是带状态的工单（待处理 / 已下架 / 已驳回），不是点赞那种「开关」语义，
--    混进 action 表会让「取消点赞」和「撤回举报」纠缠在一起。
--
-- 唯一键 (target_type, target_id, reporter_id)：同一个人对同一对象只能举报一次，
-- 防止刷单式重复举报把队列冲爆；重复提交由 Service 捕获 DuplicateKeyException 处理。
-- ----------------------------
CREATE TABLE IF NOT EXISTS `dr_report`
(
    `id`            bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `target_type`   varchar(8)   NOT NULL COMMENT '被举报对象类型：thread / reply',
    `target_id`     bigint       NOT NULL COMMENT '被举报对象 id（主题 id 或楼层 id）',
    `thread_id`     bigint       NOT NULL COMMENT '所属主题 id，便于按帖聚合查看上下文',
    `reporter_id`   bigint       NOT NULL COMMENT 'dr_user.id',
    `reporter_name` varchar(64)  DEFAULT NULL COMMENT '举报人代号快照（用户改名后仍可追溯）',
    `reason`        varchar(16)  NOT NULL COMMENT 'spam / spoiler / copyright / abuse / other',
    `detail`        varchar(500) DEFAULT NULL COMMENT '补充说明（选填）',
    `status`        tinyint      NOT NULL DEFAULT 0 COMMENT '0 待处理 / 1 已下架 / 2 已驳回',
    `handler_id`    bigint       DEFAULT NULL COMMENT '处理人 dr_admin.id',
    `handle_note`   varchar(255) DEFAULT NULL COMMENT '处理备注',
    `create_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '举报时间',
    `handle_time`   datetime     DEFAULT NULL COMMENT '处理时间',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_target_reporter` (`target_type` ASC, `target_id` ASC, `reporter_id` ASC) USING BTREE,
    INDEX `idx_status_time` (`status` ASC, `create_time` ASC) USING BTREE,
    INDEX `idx_thread` (`thread_id` ASC) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '内容举报工单'
  ROW_FORMAT = DYNAMIC;
