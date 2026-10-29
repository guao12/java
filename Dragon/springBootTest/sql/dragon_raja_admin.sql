-- ============================================================
--  卡塞尔学院 · 后台管理系统  数据库初始化脚本
--  内容：管理员表 dr_admin + 言灵图鉴表 dr_spirit + 演示数据
--
--  执行方式（命令行）：
--    "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" --default-character-set=utf8mb4 -uroot -p123456 < sql/dragon_raja_admin.sql
--  或直接运行 DragonRaja-Vue\初始化后台管理.bat
--
--  安全说明：全部是 CREATE TABLE IF NOT EXISTS + INSERT IGNORE，
--  不会 DROP 任何既有表，已修改过的言灵数据不会被覆盖，可重复执行。
-- ============================================================

USE `dragon_raja`;

-- ------------------------------------------------------------
-- 管理员表：后台管理系统登录账号（只有登录，没有注册入口）
-- 初始账号：admin / Dragon@2026（密文与演示学员 CC20260001 同源）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `dr_admin`
(
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`     VARCHAR(50)  NOT NULL COMMENT '登录账号',
    `password`     VARCHAR(100) NOT NULL COMMENT 'BCrypt 密文',
    `nickname`     VARCHAR(50)  NOT NULL DEFAULT '管理员' COMMENT '显示名',
    `status`       TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1=在册 0=封印（禁用）',
    `deleted`      TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=未删除 1=已删除',
    `login_count`  INT          NOT NULL DEFAULT 0 COMMENT '累计登录次数',
    `last_login_at` DATETIME    NULL COMMENT '最近登录时间',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='后台管理员表';

INSERT IGNORE INTO `dr_admin` (`username`, `password`, `nickname`, `create_time`)
VALUES ('admin',
        '$2b$10$zo3PuzhEXRCmi/CgCZzvnOhCTVo8X9Bb7SDjnSfJkO6VZOuksD5W2',
        '执行部管理员',
        NOW());

-- ------------------------------------------------------------
-- 言灵图鉴表：首页「言灵图鉴」区块 + /spirit/:key 详情页的数据源
-- spirit_key 唯一；status=0 时前台隐藏；sort 越小越靠前
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `dr_spirit`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `spirit_key`  VARCHAR(50)  NOT NULL COMMENT '路由 key，如 junyan（对应 /spirit/junyan 与插画 /img/spirit/junyan.webp）',
    `name`        VARCHAR(50)  NOT NULL COMMENT '言灵名',
    `element`     VARCHAR(20)  NOT NULL COMMENT '元素系别：火/冰/风/重力/时间/精神/暗影/雷/威压…',
    `level`       VARCHAR(20)  NOT NULL DEFAULT '内部' COMMENT '密级：内部/机密/高危/绝密',
    `effect`      VARCHAR(100) NOT NULL COMMENT '言灵效果（一句话）',
    `intro`       TEXT         NOT NULL COMMENT '详细介绍',
    `holder`      VARCHAR(100) NOT NULL DEFAULT '' COMMENT '典型持有者',
    `note`        VARCHAR(200) NOT NULL DEFAULT '' COMMENT '使用禁忌 / 备注',
    `image_url`   VARCHAR(500) NOT NULL DEFAULT '' COMMENT '插画图片网址（http/https），非空时优先于本地 /img/spirit/<key>.webp',
    `sort`        INT          NOT NULL DEFAULT 0 COMMENT '展示排序，越小越靠前',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '1=展示 0=隐藏',
    `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=未删除 1=已删除',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_spirit_key` (`spirit_key`),
    KEY `idx_sort` (`sort`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='言灵图鉴表';

-- ------------------------------------------------------------
-- 演示数据：与前端原静态 SPIRIT_GALLERY 完全一致（INSERT IGNORE，可重复执行）
-- ------------------------------------------------------------
INSERT IGNORE INTO `dr_spirit`
    (`spirit_key`, `name`, `element`, `level`, `effect`, `intro`, `holder`, `note`, `sort`)
VALUES ('junyan', '君焰', '火', '高危', '领域内可燃物瞬间爆燃',
        '以血统呼唤高阶火焰，在领域内形成绝对压制。稳定释放时可将空气加热至燃点，是正面攻坚中最具破坏力的言灵之一。相传释放者眼中会燃起金色竖瞳。',
        '攻坚型混血种 · 多见于战斗编制',
        '持续释放会灼伤自身血脉，超过三分钟即有失控风险。', 1),
       ('shiling', '时零', '时间', '机密', '万物迟滞，唯我不动',
        '让领域内的时间流速放缓，使用者几乎获得一段「静止时间」的观察与行动窗口。对精神与体力消耗极大，是执行部部长的招牌言灵。',
        '执行部高级专员 · 需 S 级以上血统',
        '每次释放都在燃烧寿命，频繁使用者普遍早衰。', 2),
       ('fengwang', '风王之瞳', '风', '高危', '风刃无形，触之即断',
        '凝聚风之王的竖瞳，领域内生成无色无形的风刃，可洞穿一切实体。因无法被肉眼捕捉，是最难防御的突袭类言灵。',
        '突袭型混血种 · 擅长单兵作战',
        '风刃不分敌我，混战中极易误伤同伴。', 3),
       ('bronze', '青铜御座', '重力', '机密', '重力由我定义',
        '以自身为中心重构重力场，可抬升万钧之物，也可让敌人当场跪伏。传说中曾用于托起整艘倾覆的巨轮。',
        '重装型混血种 · 常任现场指挥',
        '重力反转的瞬间使用者自身骨骼同样承受数倍负荷。', 4),
       ('zhulong', '烛龙', '冰', '绝密', '千里冰封',
        '极寒吐息所过之处一切冻结。传说中烛龙睁眼为昼、闭目为夜，此言灵一旦失控将引发区域性极寒灾害，故编列为绝密级。',
        '禁忌级持有者 · 全学院记录在册者不足三人',
        '释放后体温骤降，须在医疗部监护下复苏。', 5),
       ('wangquan', '王权', '威压', '绝密', '血统即律法',
        '血统的绝对压制。领域内一切低阶混血种会因本能的臣服而跪倒，无法直视使用者。它不属于任何元素系，是「王」的证明。',
        '龙王血裔 · 非传承不可习得',
        '对同阶及以上血统无效，一旦失效将遭到反噬。', 6),
       ('lianyou', '镰鼬', '风', '高危', '三道真空风刃，肉眼不可见',
        '以极细的真空刃切开空气，形成三道交错的弧形轨迹。被斩者往往在察觉疼痛之前就已负伤，是潜行与伏击中最难预警的言灵。',
        '侦察 / 追踪专员',
        '觉醒后听觉被永久强化，畏强噪音，须长期佩戴护具。', 7),
       ('chiri', '炽日', '火', '绝密', '凝出微型恒星，领域内一切汽化',
        '把释放者的血统燃烧到极致，在掌心凝出一颗白炽的微型恒星。其光芒可穿透一切遮蔽，其热度足以让钢铁在瞬间蒸发。',
        '屠龙级战力 · 需学院最高会议授权方可记录',
        '释放后使用者会陷入长达数日的血统衰竭，期间形同废人。', 8),
       ('jingmi', '静谧', '精神', '机密', '领域内一切声音湮灭',
        '以精神力抹平空气的振动，让领域内的声音彻底消失。可用于潜行，也可让敌人在无声中丧失方位感与协同能力。',
        '潜入 / 暗杀专员',
        '长期使用会侵蚀使用者的听觉与语言能力。', 9),
       ('mingzhao', '冥照', '暗影', '绝密', '绝对黑暗领域，剥夺一切视觉',
        '吞噬领域内的全部光线，只留下释放者视野中的轮廓。在黑暗中，唯有血脉相近者能彼此辨认，其余人皆沦为盲者。',
        '情报 / 肃清专员',
        '黑暗中视物的代价，是使用者会逐渐失去对色彩的感知。', 10),
       ('binghai', '冰海', '冰', '机密', '瞳孔所及，皆成冰原',
        '视线扫过之处海水凝滞、空气结晶，可将一整片海湾冻结成白原。与烛龙同源而威能远逊，胜在消耗可控，是极地任务中的战略级支援言灵。',
        '极地行动编制 · 冷区特训合格者',
        '冻结范围内使用者自身同样畏寒，须装备恒温护甲执行。', 11),
       ('xinwang', '心网', '精神', '内部', '听见方圆百米的心跳',
        '以精神力织成一张无形之网，网内一切生物的心跳、情绪与杀意都会在释放者脑中成形。无法读取具体思维，但谎言在它面前毫无意义。',
        '审讯 / 监察编制 · 风纪委员会外聘',
        '人群密集处长时间开启会引发精神过载，须限时使用。', 12),
       ('guzhong', '骨冢', '暗影', '绝密', '白骨为冢，亡者为兵',
        '让领域内残存的骨骼应声而起，听凭释放者驱使。传说最高纪录曾唤起一整支沉没战船的骸骨军团，故被列为本代封存级言灵。',
        '禁忌级持有者 · 记录在案者仅一人',
        '被驱使的亡骨会缓慢反噬操纵者的血肉，每次使用都在折损寿数。', 13),
       ('yulei', '御雷', '雷', '高危', '雷霆为我执剑',
        '在高空电离层与释放者之间架起「引线」，将落雷引为己用。导引精度高时可将雷霆约束成一道笔直的惩罚之剑，是压制成建制冷兵器的言灵。',
        '攻坚型混血种 · 需雷暴环境或天气支援',
        '雷雨天气威力翻倍，无云晴天几乎不可释放。', 14);

-- 校验
SELECT (SELECT COUNT(*) FROM `dr_admin`) AS 管理员数, (SELECT COUNT(*) FROM `dr_spirit`) AS 言灵数;
