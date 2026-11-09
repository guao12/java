-- ----------------------------
-- 论坛模块建表脚本：随应用启动自动执行（幂等）
--
-- 为什么放在这里：论坛的四张表是新增模块，换电脑初始化数据库时
-- 很容易只跑了 sql/dragon_raja.sql 而漏掉论坛表 —— 结果一进 /forum
-- 就是 500。这里随启动补一次（report / friend 两个模块同理，各自有脚本）。
--
-- 只做 CREATE TABLE IF NOT EXISTS：不会 DROP、不会动既有数据。
-- ----------------------------

-- 版块
CREATE TABLE IF NOT EXISTS `dr_board` (
  `id`                bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `code`              varchar(32)  NOT NULL COMMENT '版块标识，如 canon',
  `name`              varchar(64)  NOT NULL COMMENT '版块名称',
  `subtitle`          varchar(80)  NOT NULL DEFAULT '' COMMENT '英文副标题',
  `description`       varchar(255) NOT NULL DEFAULT '' COMMENT '版块简介',
  `icon`              varchar(24)  NOT NULL DEFAULT 'archive' COMMENT '图标名（对应前端 AppIcon）',
  `sort_no`           int          NOT NULL DEFAULT 0 COMMENT '排序，越小越前',
  `min_identity`      varchar(16)  NOT NULL DEFAULT 'guest' COMMENT '可见所需最低身份 guest/student/executor/teacher',
  `min_points`        int          NOT NULL DEFAULT 0 COMMENT '历史遗留：任务系统下线后不再参与可见性判定，保留列避免老库报错',
  `post_min_identity` varchar(16)  NOT NULL DEFAULT 'student' COMMENT '发帖所需最低身份',
  `read_only`         tinyint      NOT NULL DEFAULT 0 COMMENT '1=只读（公告版），仅 teacher 可发',
  `status`            tinyint      NOT NULL DEFAULT 1 COMMENT '1=开放 0=关闭',
  `create_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_code` (`code` ASC) USING BTREE,
  INDEX `idx_sort` (`status` ASC, `sort_no` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci
  COMMENT = '论坛版块' ROW_FORMAT = DYNAMIC;

-- 主题（帖子）
CREATE TABLE IF NOT EXISTS `dr_thread` (
  `id`              bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `board_id`        bigint       NOT NULL COMMENT 'dr_board.id',
  `author_id`       bigint       NOT NULL DEFAULT 0 COMMENT 'dr_user.id，0=馆藏旧帖/系统',
  `author_name`     varchar(64)  NOT NULL DEFAULT '' COMMENT '发帖时代号快照',
  `author_identity` varchar(16)  NOT NULL DEFAULT '' COMMENT '发帖时身份快照',
  `title`           varchar(120) NOT NULL COMMENT '标题',
  `content`         mediumtext   NULL COMMENT '正文（纯文本，前端按段落渲染）',
  `security_level`  tinyint      NOT NULL DEFAULT 1 COMMENT '密级 1-4，判定逻辑同档案',
  `reply_visible`   tinyint      NOT NULL DEFAULT 0 COMMENT '1=回复后可见正文',
  `pinned`          tinyint      NOT NULL DEFAULT 0 COMMENT '1=镇馆（置顶）',
  `essence`         tinyint      NOT NULL DEFAULT 0 COMMENT '1=入藏（精华）',
  `view_count`      int          NOT NULL DEFAULT 0 COMMENT '浏览量',
  `reply_count`     int          NOT NULL DEFAULT 0 COMMENT '批注数（回帖数）',
  `like_count`      int          NOT NULL DEFAULT 0 COMMENT '点赞数',
  `heat`            int          NOT NULL DEFAULT 0 COMMENT '热度 = 浏览 + 批注*3 + 点赞*5',
  `status`          tinyint      NOT NULL DEFAULT 1 COMMENT '1=正常 0=待审 -1=已下线',
  `last_reply_at`   datetime     NULL DEFAULT NULL COMMENT '最后一条批注时间',
  `deleted`         tinyint      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=未删除 1=已删除',
  `create_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_board_time` (`board_id` ASC, `status` ASC, `deleted` ASC, `pinned` ASC, `last_reply_at` ASC) USING BTREE,
  INDEX `idx_last_reply` (`status` ASC, `deleted` ASC, `last_reply_at` ASC) USING BTREE,
  INDEX `idx_author` (`author_id` ASC, `status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci
  COMMENT = '论坛主题' ROW_FORMAT = DYNAMIC;

-- 批注（回帖）
CREATE TABLE IF NOT EXISTS `dr_reply` (
  `id`              bigint      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `thread_id`       bigint      NOT NULL COMMENT 'dr_thread.id',
  `author_id`       bigint      NOT NULL DEFAULT 0 COMMENT 'dr_user.id，0=馆藏旧帖',
  `author_name`     varchar(64) NOT NULL DEFAULT '' COMMENT '代号快照',
  `author_identity` varchar(16) NOT NULL DEFAULT '' COMMENT '身份快照',
  `content`         text        NULL COMMENT '批注正文（纯文本）',
  `quote_id`        bigint      NOT NULL DEFAULT 0 COMMENT '引用的批注 id，0=不引用',
  `floor_no`        int         NOT NULL DEFAULT 1 COMMENT '楼层号，从 1 开始',
  `status`          tinyint     NOT NULL DEFAULT 1 COMMENT '1=正常 -1=已折叠',
  `deleted`         tinyint     NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_time`     datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_thread` (`thread_id` ASC, `status` ASC, `floor_no` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci
  COMMENT = '论坛批注（回帖）' ROW_FORMAT = DYNAMIC;

-- 互动：点赞 / 收藏 / 举报合并到一张表
CREATE TABLE IF NOT EXISTS `dr_thread_action` (
  `id`          bigint      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `thread_id`   bigint      NOT NULL COMMENT 'dr_thread.id',
  `user_id`     bigint      NOT NULL COMMENT 'dr_user.id',
  `type`        varchar(16) NOT NULL COMMENT 'like / collect / report',
  `create_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_thread_user_type` (`thread_id` ASC, `user_id` ASC, `type` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci
  COMMENT = '主题互动（点赞/收藏/举报）' ROW_FORMAT = DYNAMIC;
