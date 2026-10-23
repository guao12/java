/*
 Navicat Premium Dump SQL

 Source Server         : test
 Source Server Type    : MySQL
 Source Server Version : 80039 (8.0.39)
 Source Host           : localhost:3306
 Source Schema         : dragon_raja_test

 Target Server Type    : MySQL
 Target Server Version : 80039 (8.0.39)
 File Encoding         : 65001

 Date: 23/10/2026 15:06:57
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for dr_archive
-- ----------------------------
DROP TABLE IF EXISTS `dr_archive`;
CREATE TABLE `dr_archive`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '卷宗编号',
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '标题',
  `summary` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '摘要',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '正文',
  `top_category_id` bigint NOT NULL COMMENT '一级分类 ID',
  `category_id` bigint NOT NULL COMMENT '二级分类 ID',
  `security_level` tinyint NOT NULL DEFAULT 1 COMMENT '密级 1-4',
  `rank_label` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '评级标签',
  `tags` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '标签',
  `source` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '来源',
  `view_count` int NOT NULL DEFAULT 0 COMMENT '浏览量',
  `heat` int NOT NULL DEFAULT 0 COMMENT '热度',
  `spotlight` tinyint NOT NULL DEFAULT 0 COMMENT '精选',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '1=上架',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_code`(`code` ASC) USING BTREE,
  INDEX `idx_top_cat`(`top_category_id` ASC, `security_level` ASC) USING BTREE,
  INDEX `idx_cat`(`category_id` ASC, `security_level` ASC) USING BTREE,
  INDEX `idx_heat`(`heat` ASC) USING BTREE,
  INDEX `idx_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '档案主表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of dr_archive
-- ----------------------------

-- ----------------------------
-- Table structure for dr_archive_category
-- ----------------------------
DROP TABLE IF EXISTS `dr_archive_category`;
CREATE TABLE `dr_archive_category`  (
  `id` bigint NOT NULL COMMENT '主键',
  `parent_id` bigint NOT NULL DEFAULT 0 COMMENT '父级分类 ID，0 = 一级分类',
  `code` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分类 code',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分类名称',
  `icon` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'archive' COMMENT '图标名',
  `sort_no` int NOT NULL DEFAULT 0 COMMENT '排序号',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '1=启用 0=停用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_code`(`code` ASC) USING BTREE,
  INDEX `idx_parent`(`parent_id` ASC, `sort_no` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '档案分类表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of dr_archive_category
-- ----------------------------

-- ----------------------------
-- Table structure for dr_hot_keyword
-- ----------------------------
DROP TABLE IF EXISTS `dr_hot_keyword`;
CREATE TABLE `dr_hot_keyword`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `keyword` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '检索词',
  `heat` int NOT NULL DEFAULT 0 COMMENT '热度',
  `sort_no` int NOT NULL DEFAULT 0 COMMENT '排序号',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '启用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_keyword`(`keyword` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '热门检索词' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of dr_hot_keyword
-- ----------------------------

-- ----------------------------
-- Table structure for dr_notice
-- ----------------------------
DROP TABLE IF EXISTS `dr_notice`;
CREATE TABLE `dr_notice`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '公告编号',
  `title` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '标题',
  `content` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '正文',
  `level` tinyint NOT NULL DEFAULT 1 COMMENT '1普通 2重要 3紧急',
  `pinned` tinyint NOT NULL DEFAULT 0 COMMENT '置顶',
  `publisher` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '学院教务厅' COMMENT '发布部门',
  `publish_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
  `expire_at` datetime NULL DEFAULT NULL COMMENT '失效时间',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '1=已发布',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_code`(`code` ASC) USING BTREE,
  INDEX `idx_publish`(`pinned` ASC, `publish_at` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '首页公告' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of dr_notice
-- ----------------------------

-- ----------------------------
-- Table structure for dr_reset_code
-- ----------------------------
DROP TABLE IF EXISTS `dr_reset_code`;
CREATE TABLE `dr_reset_code`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `account` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '学院编号（学号）',
  `code` varchar(6) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '6 位数字验证龙文',
  `expire_time` datetime NOT NULL COMMENT '过期时间',
  `used` tinyint NOT NULL DEFAULT 0 COMMENT '0=未使用 1=已使用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下发时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_account`(`account` ASC, `create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '密钥重置验证码表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of dr_reset_code
-- ----------------------------

-- ----------------------------
-- Table structure for dr_search_log
-- ----------------------------
DROP TABLE IF EXISTS `dr_search_log`;
CREATE TABLE `dr_search_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NULL DEFAULT NULL COMMENT '检索人 ID',
  `keyword` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '关键词',
  `top_category` bigint NULL DEFAULT NULL COMMENT '一级分类',
  `category_id` bigint NULL DEFAULT NULL COMMENT '二级分类',
  `level` tinyint NULL DEFAULT NULL COMMENT '密级',
  `result_count` int NOT NULL DEFAULT 0 COMMENT '命中数',
  `client_ip` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '来源 IP',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '检索时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_keyword`(`keyword` ASC) USING BTREE,
  INDEX `idx_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '检索日志' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of dr_search_log
-- ----------------------------

-- ----------------------------
-- Table structure for dr_user
-- ----------------------------
DROP TABLE IF EXISTS `dr_user`;
CREATE TABLE `dr_user`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `account` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '学院编号（学号）',
  `codename` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '姓名 / 行动代号',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'BCrypt 密文',
  `identity` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'freshman' COMMENT '在校身份 code',
  `spirit` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'none' COMMENT '言灵 code',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '1=在册 0=封印',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `login_count` int NOT NULL DEFAULT 0 COMMENT '累计登录',
  `last_login_at` datetime NULL DEFAULT NULL COMMENT '最近登录',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登记时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_account`(`account` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '混血种（用户）表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of dr_user
-- ----------------------------

SET FOREIGN_KEY_CHECKS = 1;
