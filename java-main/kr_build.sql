/*
 Navicat Premium Dump SQL

 Source Server         : test
 Source Server Type    : MySQL
 Source Server Version : 80039 (8.0.39)
 Source Host           : localhost:3306
 Source Schema         : kr_build

 Target Server Type    : MySQL
 Target Server Version : 80039 (8.0.39)
 File Encoding         : 65001

 Date: 09/10/2026 14:51:50
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for kr_user
-- ----------------------------
DROP TABLE IF EXISTS `kr_user`;
CREATE TABLE `kr_user`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '骑士名 / 登录账号',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '召集令邮箱',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'BCrypt 加密后的密码',
  `form_key` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'rabbittank' COMMENT '当前形态：rabbittank/hazard/genius',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1=正常 0=禁用',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=未删除 1=已删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_username`(`username` ASC) USING BTREE,
  UNIQUE INDEX `uk_email`(`email` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '骑士（用户）表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of kr_user
-- ----------------------------
INSERT INTO `kr_user` VALUES (1, 'build_test', 'build@test.com', '$2a$10$fneuv0Pi38sKvPPykgvJ1extvKKW4Utk0i96Ho4uWPU6KqjS0Q9f6', 'rabbittank', 1, 0, '2026-10-09 14:39:27', '2026-10-09 14:39:27');

SET FOREIGN_KEY_CHECKS = 1;
