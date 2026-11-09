-- ----------------------------
-- 好友 / 私信模块建表脚本：随应用启动自动执行（幂等）
--
-- 为什么放在这里而不是只写进 sql/dragon_raja.sql：
-- 这三张表是新增模块，换电脑初始化数据库时很容易只跑主 dump 而漏掉，
-- 结果一进 /friends 就 500 —— 和论坛表是同一个坑，所以同样随启动补一次。
--
-- 只做 CREATE TABLE IF NOT EXISTS：不 DROP、不动既有数据。
-- ----------------------------

-- 好友申请
CREATE TABLE IF NOT EXISTS `dr_friend_request` (
  `id`           bigint      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `from_user_id` bigint      NOT NULL COMMENT '申请人 dr_user.id',
  `to_user_id`   bigint      NOT NULL COMMENT '被申请人 dr_user.id',
  `message`      varchar(120) NOT NULL DEFAULT '' COMMENT '申请附言',
  `status`       tinyint     NOT NULL DEFAULT 0 COMMENT '0=待处理 1=已同意 2=已拒绝',
  `handled_at`   datetime    NULL DEFAULT NULL COMMENT '处理时间',
  `create_time`  datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  `update_time`  datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  -- 同一方向只保留一条记录：重复申请走 UPDATE 置回待处理，不堆历史垃圾
  UNIQUE INDEX `uk_pair` (`from_user_id` ASC, `to_user_id` ASC) USING BTREE,
  INDEX `idx_inbox` (`to_user_id` ASC, `status` ASC, `create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci
  COMMENT = '好友申请' ROW_FORMAT = DYNAMIC;

-- 好友关系
--
-- 设计说明：**一个方向一行**（A 加 B 存 A→B 和 B→A 两行）。
-- 冗余换来的好处是「查我的好友」「查是否好友」都只走单表索引，
-- 不需要 OR 拼双向条件（那样会让 idx 失效）；
-- 代价是删除时必须两行一起删，这项约束由 Service 统一保证。
CREATE TABLE IF NOT EXISTS `dr_friendship` (
  `id`          bigint      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`     bigint      NOT NULL COMMENT '归属人 dr_user.id',
  `friend_id`   bigint      NOT NULL COMMENT '好友 dr_user.id',
  `remark`      varchar(32) NOT NULL DEFAULT '' COMMENT '好友备注（暂未开放编辑）',
  `create_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '成为好友的时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_friend` (`user_id` ASC, `friend_id` ASC) USING BTREE,
  INDEX `idx_user` (`user_id` ASC, `create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci
  COMMENT = '好友关系（双向各一行）' ROW_FORMAT = DYNAMIC;

-- 私信
--
-- 会话不做单独的会话表：一对一私聊的会话就是 (sender_id, receiver_id) 的组合，
-- 列表页用相关子查询取「最后一条」，在千级数据量下比维护一张会话表更省事，
-- 也不会出现会话表与消息表不一致的问题。
CREATE TABLE IF NOT EXISTS `dr_message` (
  `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `sender_id`   bigint       NOT NULL COMMENT '发送者 dr_user.id',
  `receiver_id` bigint       NOT NULL COMMENT '接收者 dr_user.id',
  `content`     varchar(1000) NOT NULL COMMENT '内容（纯文本，前端按转义文本渲染）',
  `read_flag`   tinyint      NOT NULL DEFAULT 0 COMMENT '0=未读 1=已读',
  `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_pair` (`sender_id` ASC, `receiver_id` ASC, `id` ASC) USING BTREE,
  INDEX `idx_inbox` (`receiver_id` ASC, `read_flag` ASC, `id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci
  COMMENT = '私信消息' ROW_FORMAT = DYNAMIC;
