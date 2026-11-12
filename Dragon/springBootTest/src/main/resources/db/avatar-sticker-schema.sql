-- ----------------------------
-- 头像 + 表情包 模块：补列 / 建表脚本（随应用启动自动执行，幂等）
--
-- 与 friend-schema.sql 同一个理由：换电脑只跑 sql/dragon_raja.sql 会漏掉这些列，
-- 结果头像上传 / 表情消息一用就 500，所以随启动补一次。
--
-- MySQL 8 不支持 ALTER TABLE ... ADD COLUMN IF NOT EXISTS，这里用
-- information_schema 探测 + PREPARE 动态执行实现幂等补列，重复执行无副作用。
-- ----------------------------

-- ① dr_user 补 avatar 列：存头像的站点相对地址（/uploads/avatar/xxx），NULL = 从未上传
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dr_user' AND COLUMN_NAME = 'avatar');
SET @ddl := IF(@has = 0,
  'ALTER TABLE `dr_user` ADD COLUMN `avatar` varchar(255) NULL DEFAULT NULL COMMENT ''头像地址（站点相对路径），NULL=默认头像'' AFTER `spirit`',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ② dr_message 补 msg_type 列：0=纯文本 1=表情包（content 存表情编码）
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dr_message' AND COLUMN_NAME = 'msg_type');
SET @ddl := IF(@has = 0,
  'ALTER TABLE `dr_message` ADD COLUMN `msg_type` tinyint NOT NULL DEFAULT 0 COMMENT ''消息类型：0=文本 1=表情包'' AFTER `content`',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ③ 内置表情包目录表。
--
-- 设计说明：表情包图片是站点内置静态资源（前端 public/img/stickers/），
-- 这里只存「编码 + 名称 + 图片路径」的目录，供 /api/friend/stickers 下发；
-- 消息表只存编码（content = code），不存图片二进制 —— 表情是全员共用的
-- 固定资源，每条消息冗余一份图片地址没有意义，存编码还能让目录热更新。
CREATE TABLE IF NOT EXISTS `dr_sticker` (
  `id`          bigint      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `code`        varchar(32) NOT NULL COMMENT '表情编码，消息 content 引用它',
  `name`        varchar(32) NOT NULL COMMENT '显示名',
  `image`       varchar(128) NOT NULL COMMENT '图片路径（站点静态资源）',
  `sort_no`     int         NOT NULL DEFAULT 0 COMMENT '排序',
  `create_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入册时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_code` (`code`) ASC USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci
  COMMENT = '内置表情包目录' ROW_FORMAT = DYNAMIC;

-- 内置表情包种子（固定主键 + INSERT IGNORE：重复启动只灌一次，不覆盖后续改动）
INSERT IGNORE INTO `dr_sticker` (`id`, `code`, `name`, `image`, `sort_no`) VALUES
  (1,  's01', '得意',   '/img/stickers/s01.svg', 1),
  (2,  's02', '大笑',   '/img/stickers/s02.svg', 2),
  (3,  's03', '哭泣',   '/img/stickers/s03.svg', 3),
  (4,  's04', '愤怒',   '/img/stickers/s04.svg', 4),
  (5,  's05', '惊讶',   '/img/stickers/s05.svg', 5),
  (6,  's06', '比心',   '/img/stickers/s06.svg', 6),
  (7,  's07', '加油',   '/img/stickers/s07.svg', 7),
  (8,  's08', '困了',   '/img/stickers/s08.svg', 8),
  (9,  's09', '流汗',   '/img/stickers/s09.svg', 9),
  (10, 's10', '思考',   '/img/stickers/s10.svg', 10),
  (11, 's11', '挥手',   '/img/stickers/s11.svg', 11),
  (12, 's12', '火焰',   '/img/stickers/s12.svg', 12);
