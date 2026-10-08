-- ============================================================
--  假面骑士创骑 · 骑士系统  数据库初始化脚本
--  MySQL 8.0+  执行方式（命令行）：
--    mysql -uroot -p123456 < db/init.sql
--  或在 IDEA 的 Database 工具窗口中直接执行本文件
-- ============================================================

CREATE DATABASE IF NOT EXISTS `kr_build`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

-- 测试环境用的库（对应 application-test.properties）
CREATE DATABASE IF NOT EXISTS `kr_build_test`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE `kr_build`;

DROP TABLE IF EXISTS `kr_user`;
CREATE TABLE `kr_user`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`    VARCHAR(50)  NOT NULL COMMENT '骑士名 / 登录账号',
    `email`       VARCHAR(100) NOT NULL COMMENT '召集令邮箱',
    `password`    VARCHAR(100) NOT NULL COMMENT 'BCrypt 加密后的密码',
    `form_key`    VARCHAR(20)           DEFAULT 'rabbittank' COMMENT '当前形态：rabbittank/hazard/genius',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1=正常 0=禁用',
    `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=未删除 1=已删除',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_email` (`email`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='骑士（用户）表';

-- 测试库结构保持一致
USE `kr_build_test`;
DROP TABLE IF EXISTS `kr_user`;
CREATE TABLE `kr_user`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`    VARCHAR(50)  NOT NULL COMMENT '骑士名 / 登录账号',
    `email`       VARCHAR(100) NOT NULL COMMENT '召集令邮箱',
    `password`    VARCHAR(100) NOT NULL COMMENT 'BCrypt 加密后的密码',
    `form_key`    VARCHAR(20)           DEFAULT 'rabbittank' COMMENT '当前形态',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1=正常 0=禁用',
    `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=未删除 1=已删除',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_email` (`email`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='骑士（用户）表';

USE `kr_build`;
SELECT 'kr_build 数据库与 kr_user 表初始化完成' AS result;
