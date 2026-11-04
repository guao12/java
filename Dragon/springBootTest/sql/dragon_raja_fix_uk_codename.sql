-- ----------------------------
-- 修复：dr_user.codename 缺少唯一索引
--
-- 背景：注册逻辑（AuthServiceImpl#register）会先做 selectCount 查重，并在注释中
--       声明「codename 已加唯一索引，并发冲突由唯一键兜底」。但建表脚本只有
--       uk_account，没有 uk_codename —— 并发注册同名代号时可以写入重复行，
--       之后 selectByCodename(... LIMIT 1) 会返回不确定的某一行，
--       导致「登录 / 重置密码」可能落到另一个同名账号上（越权风险）。
--
-- 执行：mysql -uroot -p123456 dragon_raja < sql/dragon_raja_fix_uk_codename.sql
-- 说明：脚本可重复执行；若已存在同名索引则跳过。
-- ----------------------------
USE `dragon_raja`;

-- 1) 先检查是否已存在重复代号（有结果说明必须先人工处理，否则加索引会失败）
SELECT `codename`, COUNT(*) AS cnt, GROUP_CONCAT(`id` ORDER BY `id`) AS ids
FROM `dr_user`
WHERE `deleted` = 0
GROUP BY `codename`
HAVING COUNT(*) > 1;

-- 2) 加唯一索引（幂等）
SET @idx_exists := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'dr_user'
    AND index_name = 'uk_codename'
);

SET @ddl := IF(@idx_exists = 0,
  'ALTER TABLE `dr_user` ADD UNIQUE INDEX `uk_codename`(`codename` ASC) USING BTREE',
  'SELECT ''uk_codename 已存在，跳过'' AS result');

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
