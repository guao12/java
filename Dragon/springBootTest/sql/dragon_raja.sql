-- ============================================================
--  卡塞尔学院 · 龙族身份认证系统  数据库初始化脚本
--  对接前端：DragonRaja-Vue（Vue 3 + Vite，代理 /api -> 127.0.0.1:8081）
--  后端：Spring Boot 3 + MyBatis-Plus + MySQL 8
--
--  执行方式（命令行）：
--    "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe" -uroot -p123456 < sql/dragon_raja.sql
--  或在 IDEA 的 Database 工具窗口中直接执行本文件
-- ============================================================

CREATE DATABASE IF NOT EXISTS `dragon_raja`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

-- 测试环境用的库（对应 application-test.properties）
CREATE DATABASE IF NOT EXISTS `dragon_raja_test`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE `dragon_raja`;

-- ------------------------------------------------------------
-- 用户表：卡塞尔学院在册混血种（身份卡）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `dr_user`;
CREATE TABLE `dr_user`
(
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `account`      VARCHAR(20)  NOT NULL COMMENT '学院编号（学号），如 CC20260001',
    `codename`     VARCHAR(50)  NOT NULL COMMENT '姓名 / 行动代号',
    `password`     VARCHAR(100) NOT NULL COMMENT 'BCrypt 加密后的言灵密钥',
    `identity`     VARCHAR(20)  NOT NULL DEFAULT 'freshman' COMMENT '在校身份 code：freshman/student/executor/teacher/guest',
    `spirit`       VARCHAR(20)  NOT NULL DEFAULT 'none' COMMENT '言灵 code：none/junyan/bronze/shiling/fengwang/zhulong/wangquan',
    `status`       TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1=在册 0=封印（禁用）',
    `deleted`      TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=未删除 1=已删除',
    `login_count`  INT          NOT NULL DEFAULT 0 COMMENT '累计登录次数',
    `last_login_at` DATETIME    NULL COMMENT '最近登录时间',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登记时间',
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_account` (`account`),
    KEY `idx_codename` (`codename`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='混血种（用户）表';

-- ------------------------------------------------------------
-- 密钥重置验证码表：验证龙文（5 分钟有效，60 秒冷却）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `dr_reset_code`;
CREATE TABLE `dr_reset_code`
(
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `account`     VARCHAR(20) NOT NULL COMMENT '对应的学院编号（学号）',
    `code`        VARCHAR(6)  NOT NULL COMMENT '6 位数字验证龙文',
    `expire_time` DATETIME    NOT NULL COMMENT '过期时间',
    `used`        TINYINT     NOT NULL DEFAULT 0 COMMENT '0=未使用 1=已使用',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下发时间',
    PRIMARY KEY (`id`),
    KEY `idx_account` (`account`, `create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='密钥重置验证码表';

-- ------------------------------------------------------------
-- 演示账号（三个），言灵密钥统一为：Dragon@2026
-- 登录仅用学院编号（学号），例如 CC20260001  +  Dragon@2026
-- ------------------------------------------------------------
INSERT INTO `dr_user` (`account`, `codename`, `password`, `identity`, `spirit`, `login_count`, `create_time`)
VALUES ('CC20260001', '路明非',
        '$2b$10$zo3PuzhEXRCmi/CgCZzvnOhCTVo8X9Bb7SDjnSfJkO6VZOuksD5W2',
        'freshman', 'junyan', 0, NOW()),
       ('CC20260002', '楚子航',
        '$2b$10$EhhQtz0J7vukNhH3zCoBiew.9/FOuG9I/pE3uKWRR.7JcZxq150ou',
        'executor', 'shiling', 0, NOW()),
       ('CC20260003', '芬格尔',
        '$2b$10$aamVUE83rOCVisNGuH2TbOJHUrEfbXNcNzvwSOsmnASalROlxKlAC',
        'student', 'fengwang', 0, NOW());

-- 测试库结构保持一致（不含演示数据）
USE `dragon_raja_test`;
DROP TABLE IF EXISTS `dr_user`;
CREATE TABLE `dr_user`
(
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `account`      VARCHAR(20)  NOT NULL COMMENT '学院编号（学号）',
    `codename`     VARCHAR(50)  NOT NULL COMMENT '姓名 / 行动代号',
    `password`     VARCHAR(100) NOT NULL COMMENT 'BCrypt 密文',
    `identity`     VARCHAR(20)  NOT NULL DEFAULT 'freshman' COMMENT '在校身份 code',
    `spirit`       VARCHAR(20)  NOT NULL DEFAULT 'none' COMMENT '言灵 code',
    `status`       TINYINT      NOT NULL DEFAULT 1 COMMENT '1=在册 0=封印',
    `deleted`      TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `login_count`  INT          NOT NULL DEFAULT 0 COMMENT '累计登录',
    `last_login_at` DATETIME    NULL COMMENT '最近登录',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登记时间',
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_account` (`account`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='混血种（用户）表';

DROP TABLE IF EXISTS `dr_reset_code`;
CREATE TABLE `dr_reset_code`
(
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `account`     VARCHAR(20) NOT NULL COMMENT '学院编号（学号）',
    `code`        VARCHAR(6)  NOT NULL COMMENT '6 位数字验证龙文',
    `expire_time` DATETIME    NOT NULL COMMENT '过期时间',
    `used`        TINYINT     NOT NULL DEFAULT 0 COMMENT '0=未使用 1=已使用',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下发时间',
    PRIMARY KEY (`id`),
    KEY `idx_account` (`account`, `create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='密钥重置验证码表';

USE `dragon_raja`;
SELECT 'dragon_raja 数据库初始化完成：dr_user / dr_reset_code 已建表，3 个演示账号密钥均为 Dragon@2026' AS result;
