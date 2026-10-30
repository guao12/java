/*
 Navicat Premium Dump SQL

 Source Server         : test
 Source Server Type    : MySQL
 Source Server Version : 80039 (8.0.39)
 Source Host           : localhost:3306
 Source Schema         : dragon_raja

 Target Server Type    : MySQL
 Target Server Version : 80039 (8.0.39)
 File Encoding         : 65001

 Date: 29/10/2026 15:10:22
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for dr_admin
-- ----------------------------
DROP TABLE IF EXISTS `dr_admin`;
CREATE TABLE `dr_admin`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '登录账号',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'BCrypt 密文',
  `nickname` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '管理员' COMMENT '显示名',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1=在册 0=封印（禁用）',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=未删除 1=已删除',
  `login_count` int NOT NULL DEFAULT 0 COMMENT '累计登录次数',
  `last_login_at` datetime NULL DEFAULT NULL COMMENT '最近登录时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_username`(`username` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '后台管理员表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of dr_admin
-- ----------------------------
INSERT INTO `dr_admin` VALUES (1, 'admin', '$2b$10$zo3PuzhEXRCmi/CgCZzvnOhCTVo8X9Bb7SDjnSfJkO6VZOuksD5W2', '执行部管理员', 1, 0, 9, '2026-10-26 09:24:06', '2026-10-23 14:52:37', '2026-10-26 09:24:05');
INSERT INTO `dr_admin` VALUES (3, '孤傲', '$2a$10$q8Im82jt0rLviJcbujD1POsUJAqX5QrLTEqGN1ZdpR9QnUpcK2Thu', '孤傲', 1, 0, 1, '2026-10-29 14:53:36', '2026-10-29 14:48:58', '2026-10-29 14:53:36');

-- ----------------------------
-- Table structure for dr_archive
-- ----------------------------
DROP TABLE IF EXISTS `dr_archive`;
CREATE TABLE `dr_archive`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `code` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '卷宗编号，唯一（如 AR-P-001）',
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '卷宗标题',
  `summary` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '摘要（列表展示）',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '正文（详情展示）',
  `top_category_id` bigint NOT NULL COMMENT '一级分类 ID（冗余，便于按大类筛选）',
  `category_id` bigint NOT NULL COMMENT '二级分类 ID',
  `security_level` tinyint NOT NULL DEFAULT 1 COMMENT '密级：1=公开 2=内部 3=机密 4=绝密',
  `rank_label` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '评级标签：S / A / B / 龙王 / 禁咒 / 禁区…',
  `tags` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '标签，英文逗号分隔',
  `source` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '来源 / 归档部门',
  `view_count` int NOT NULL DEFAULT 0 COMMENT '浏览量',
  `heat` int NOT NULL DEFAULT 0 COMMENT '热度（首页排序用）',
  `spotlight` tinyint NOT NULL DEFAULT 0 COMMENT '是否首页精选：1=精选 0=普通',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '1=公开上架 0=封存',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=未删除 1=已删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '归档时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_code`(`code` ASC) USING BTREE,
  INDEX `idx_top_cat`(`top_category_id` ASC, `security_level` ASC) USING BTREE,
  INDEX `idx_cat`(`category_id` ASC, `security_level` ASC) USING BTREE,
  INDEX `idx_heat`(`heat` ASC) USING BTREE,
  INDEX `idx_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 36 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '档案主表（含密级分级）' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of dr_archive
-- ----------------------------
INSERT INTO `dr_archive` VALUES (1, 'AR-P-001', '路明非', '卡塞尔学院 2026 级新生，学院史上首位 S 级评估的混血种，血统来源至今不明。', '路明非，男，2026 级新生。入学血统评级为 S，是本校自建校以来第一位在入学鉴定中即取得 S 级评价的学生。其血统谱系无法追溯至任何已知混血种家族，基因组比对结果与现存所有龙王系谱均存在部分吻合，原因待查。目前已进入执行部重点关注名单。', 1001, 2001, 2, 'S', '混血种,S级,新生,执行部重点关注', '学院教务处', 3862, 98, 1, 1, 0, '2026-10-23 14:23:32', '2026-10-26 08:56:42');
INSERT INTO `dr_archive` VALUES (2, 'AR-P-002', '楚子航', '执行部王牌专员，代号「村雨」，持言灵·风王之瞳，剑术精湛。', '楚子航，执行部 A 级专员，行动代号「村雨」。持有瞳术系言灵·风王之瞳，可洞穿幻象与谎言。擅长近身剑术，参与过多次高危任务，是执行部现存战力评估最高的专员之一。性格冷静自持，任务完成率 100%。', 1001, 2003, 2, 'A', '执行部,言灵使,村雨,瞳术', '执行部人事科', 3240, 95, 1, 1, 0, '2026-10-23 14:23:32', '2026-10-23 14:23:32');
INSERT INTO `dr_archive` VALUES (3, 'AR-P-003', '芬格尔', '2026 级在校生，血统评级 B，以情报搜集与伪装渗透见长。', '芬格尔，2026 级在校生，血统评级 B。虽评级不高，但在情报搜集、伪装渗透与社交工程方面展现出极高天赋，多次为执行部提供关键线索。其持有的言灵·时零在实战中主要用于脱离与规避。', 1001, 2001, 1, 'B', '在校生,情报,伪装,时零', '学院教务处', 1580, 62, 0, 1, 0, '2026-10-23 14:23:32', '2026-10-23 14:23:32');
INSERT INTO `dr_archive` VALUES (4, 'AR-P-004', '凯撒·加图索', '执行部资深专员，血统评级 A，擅长正面攻坚与战术指挥。', '凯撒·加图索，执行部资深专员，血统评级 A。出身于意大利混血种世家加图索家族，家族与学院合作历史超过一百年。擅长正面攻坚与小组战术指挥，持有火系言灵。其家族背景使其在外部势力交涉中具有不可替代的作用。', 1001, 2003, 2, 'A', '执行部,加图索家族,攻坚,指挥', '执行部人事科', 2410, 80, 0, 1, 0, '2026-10-23 14:23:32', '2026-10-23 14:23:32');
INSERT INTO `dr_archive` VALUES (5, 'AR-P-005', '诺诺', '执行部专员，血统评级 A，擅长高速机动与侦察。', '诺诺，执行部专员，血统评级 A。以极高的机动性与侦察能力著称，多次独立完成深入里世界的侦察任务。与路明非、楚子航组成固定行动小组。', 1001, 2003, 2, 'A', '执行部,侦察,机动', '执行部人事科', 1960, 71, 0, 1, 0, '2026-10-23 14:23:32', '2026-10-23 14:23:32');
INSERT INTO `dr_archive` VALUES (6, 'AR-P-006', '苏恩曦', '学院医务室负责人，血统评级 B，精通混血种生理治理。', '苏恩曦，学院医务室负责人，血统评级 B。精通混血种生理结构与血统暴走救治，持有罕见的治疗类言灵。所有执行部专员在执行高危任务前必须经由她签署健康许可。', 1001, 2001, 2, 'B', '医务室,治疗,血统压制', '学院人事处', 1120, 54, 0, 1, 0, '2026-10-23 14:23:32', '2026-10-23 14:23:32');
INSERT INTO `dr_archive` VALUES (7, 'AR-P-007', '诺顿', '青铜与火之王的王座继承者，掌控青铜与火的双重权柄。', '诺顿，青铜与火之王的王座继承者。掌控青铜与火的双重权柄，是现存对混血种威胁等级最高的龙王之一。其活动轨迹与多次里世界异常波动高度相关。学院对其保持最高级别监控，任何相关线索需第一时间上报执行部。', 1001, 2002, 3, '龙王', '龙王,青铜与火,尼伯龙根,最高监控', '龙类研究院', 5211, 99, 1, 1, 0, '2026-10-23 14:23:32', '2026-10-29 14:53:02');
INSERT INTO `dr_archive` VALUES (8, 'AR-P-008', '康斯坦丁', '大地与山之王，与诺顿同为初代龙王直系。', '康斯坦丁，大地与山之王。与诺顿同属初代龙王直系，权柄覆盖大地与山脉。其苏醒征兆通常表现为大范围地壳微震，学院地质监测网已就此建立专项预警。', 1001, 2002, 3, '龙王', '龙王,大地与山,地质预警', '龙类研究院', 2870, 84, 0, 1, 0, '2026-10-23 14:23:32', '2026-10-23 14:23:32');
INSERT INTO `dr_archive` VALUES (9, 'AR-P-009', '白王', '龙族史上的叛变者，权柄为「精神」与「梦境」。', '白王，龙族史上唯一一位背叛龙王阵营并站在人类一侧的存在，权柄覆盖精神与梦境领域。其遗留的权柄碎片至今仍是多方势力争夺的焦点，任何一枚碎片的现世都可能引发连锁反应。', 1001, 2002, 4, '龙王', '龙王,精神,梦境,权柄碎片', '绝密档案室', 4330, 92, 0, 1, 0, '2026-10-23 14:23:32', '2026-10-23 14:23:32');
INSERT INTO `dr_archive` VALUES (10, 'AR-P-010', '昂热', '卡塞尔学院校长，血统强度与真实年龄均属绝密。', '昂热，卡塞尔学院校长。其血统强度、真实年龄与早年经历均为绝密信息，仅限校董会与执行部最高层查阅。已知信息：其在过去一个世纪中持续主导学院的屠龙战略，并与多位初代龙王有过正面交锋记录。', 1001, 2003, 4, '绝密', '校长,绝密,战略决策', '绝密档案室', 3720, 90, 1, 1, 0, '2026-10-23 14:23:32', '2026-10-23 14:23:32');
INSERT INTO `dr_archive` VALUES (11, 'AR-S-001', '言灵·君焰', '以龙文吟诵引动的高热领域，范围毁灭级言灵。', '言灵·君焰，火系最高阶言灵之一。以龙文吟诵引动，在指定范围内生成极高温度的领域，属于范围毁灭级。仅限 S 级及以上评估者接触原始卷宗，演练需在学院地下演练场进行。', 1002, 2004, 3, '禁咒', '言灵,禁咒级,火系,毁灭级', '言灵研究院', 4680, 97, 1, 1, 0, '2026-10-23 14:23:32', '2026-10-23 14:23:32');
INSERT INTO `dr_archive` VALUES (12, 'AR-S-002', '言灵·烛龙', '光与暗的双生言灵，可生成绝对黑暗领域。', '言灵·烛龙，传说中烛龙之眼的具现化。可在指定范围内生成绝对黑暗领域，同时持有者保留视觉。属于高阶言灵，实战中多用于突袭与撤退掩护。', 1002, 2004, 2, '高阶', '言灵,高阶,黑暗,突袭', '言灵研究院', 1720, 68, 0, 1, 0, '2026-10-23 14:23:32', '2026-10-23 14:23:32');
INSERT INTO `dr_archive` VALUES (13, 'AR-S-003', '言灵·时零', '短时间加速自身时间流速，高阶辅助型言灵。', '言灵·时零，高阶辅助型言灵。可在数秒内将持有者的主观时间流速提升至常人数倍，外界观察者看来近乎瞬移。副作用为神经负荷，连续使用不得超过三次。', 1002, 2005, 2, '高阶', '言灵,高阶,辅助,时间', '言灵研究院', 2210, 76, 0, 1, 0, '2026-10-23 14:23:32', '2026-10-23 14:23:32');
INSERT INTO `dr_archive` VALUES (14, 'AR-S-004', '言灵·镇魂', '安抚与压制类言灵，常用于抑制血统暴走。', '言灵·镇魂，中阶辅助型言灵。可对目标的精神活动产生安抚作用，是抑制混血种血统暴走的标准手段之一。执行部任务标配言灵，所有专员须通过基础使用考核。', 1002, 2005, 1, '中阶', '言灵,中阶,辅助,血统压制', '执行部训练科', 1450, 60, 0, 1, 0, '2026-10-23 14:23:32', '2026-10-23 14:23:32');
INSERT INTO `dr_archive` VALUES (15, 'AR-S-005', '言灵·风王之瞳', '瞳术系言灵，可洞穿谎言与幻象。', '言灵·风王之瞳，瞳术系高阶言灵。激活后可洞穿幻象与谎言，并在短时间内大幅提升视觉捕捉能力，是执行部侦查与审讯任务的核心手段。已知持有者：楚子航。', 1002, 2006, 2, '高阶', '言灵,瞳术,高阶,侦查', '言灵研究院', 2980, 88, 0, 1, 0, '2026-10-23 14:23:32', '2026-10-23 14:23:32');
INSERT INTO `dr_archive` VALUES (16, 'AR-S-006', '言灵·王权', '禁咒级言灵，可对所有低阶龙类施加绝对压制。', '言灵·王权，禁咒级言灵。可对血统位阶低于持有者的龙类施加绝对压制，短暂剥夺其行动能力。使用代价极高，每次激活后持有者需至少72小时恢复期。已知持有者信息为绝密。', 1002, 2007, 4, '禁咒', '言灵,禁咒级,压制,绝密', '绝密档案室', 3560, 91, 0, 1, 0, '2026-10-23 14:23:32', '2026-10-23 14:23:32');
INSERT INTO `dr_archive` VALUES (17, 'AR-S-007', '言灵·青铜御座', '禁咒级言灵，与青铜与火之王权柄直接相关。', '言灵·青铜御座，禁咒级言灵。其权柄来源与青铜与火之王直接相关，持有者可生成高强度金属防御领域。因权柄来源敏感，该言灵的所有研究记录均存放于绝密档案室。', 1002, 2007, 3, '禁咒', '言灵,禁咒级,防御,青铜与火', '绝密档案室', 2040, 79, 0, 1, 0, '2026-10-23 14:23:32', '2026-10-23 14:23:32');
INSERT INTO `dr_archive` VALUES (18, 'AR-S-008', '言灵·审判', '禁咒级言灵，以龙文宣告的方式直接裁定目标命运。', '言灵·审判，禁咒级言灵。以龙文宣告的方式对目标直接施加「裁定」，其效果与代价均未被完整记录。学院仅在一次历史任务中有过使用记载，此后列为禁止主动使用的言灵。', 1002, 2007, 4, '禁咒', '言灵,禁咒级,禁止使用,绝密', '绝密档案室', 3120, 87, 1, 1, 0, '2026-10-23 14:23:32', '2026-10-23 14:23:32');
INSERT INTO `dr_archive` VALUES (19, 'AR-L-001', '尼伯龙根', '龙族栖息的里世界入口，空间结构极不稳定。', '尼伯龙根，龙族栖息的里世界入口统称。其内部空间结构极不稳定，常规物理规律部分失效。未经校长或执行部最高层书面授权，任何人不得进入。已知入口分布于全球七处，其中三处处于学院监控之下。', 1003, 2008, 3, '禁区', '里世界,禁区,龙王领域,授权进入', '龙类研究院', 4120, 93, 1, 1, 0, '2026-10-23 14:23:32', '2026-10-23 14:23:32');
INSERT INTO `dr_archive` VALUES (20, 'AR-L-002', '冰海', '位于北冰洋下的巨大空腔，与白王权柄高度相关。', '冰海，位于北冰洋冰盖下的巨大地下空腔。内部存在大量龙文遗迹，其权柄特征与白王高度吻合。学院曾在此执行过一次大型勘探任务，任务卷宗已封存。', 1003, 2008, 3, '禁区', '里世界,冰海,白王,勘探', '龙类研究院', 1890, 72, 0, 1, 0, '2026-10-23 14:23:32', '2026-10-23 14:23:32');
INSERT INTO `dr_archive` VALUES (21, 'AR-L-003', '地下图书馆', '学院地下七层的龙文典籍藏馆，A 级以上权限方可入内。', '地下图书馆，位于学院主楼地下共七层，收藏龙文典籍与各类绝密卷宗。B1-B3 层对 A 级及以上开放，B4 及以下需校长授权。馆内恒温恒湿，龙文残卷的破译工作在此进行。', 1003, 2009, 2, '管控', '学院设施,图书馆,权限分级', '学院教务厅', 2670, 81, 0, 1, 0, '2026-10-23 14:23:32', '2026-10-23 14:23:32');
INSERT INTO `dr_archive` VALUES (22, 'AR-L-004', '炼金工坊', '执行部装备研发与维护中心，混血种专属武器的唯一产地。', '炼金工坊，执行部装备研发与维护中心。全部混血种专属武器（如村雨系列）均在此锻造与维护。工坊采用龙文炼金术与传统冶金结合工艺，核心技术仅掌握在少数匠人手中。', 1003, 2009, 2, '管控', '学院设施,炼金,装备', '执行部后勤科', 1340, 58, 0, 1, 0, '2026-10-23 14:23:32', '2026-10-23 14:23:32');
INSERT INTO `dr_archive` VALUES (23, 'AR-O-001', '卡塞尔学院', '表面为芝加哥私立贵族学府，实为混血种培养与屠龙战略核心机构。', '卡塞尔学院，对外为美国芝加哥的一所私立贵族学府。其真实职能为混血种的培养、血统研究以及屠龙战略的制定与执行。学院由校董会直接管理，执行部为其行动臂膀。', 1004, 2010, 1, '公开', '学院,总部,屠龙,混血种', '学院教务厅', 5200, 96, 1, 1, 0, '2026-10-23 14:23:32', '2026-10-23 14:23:32');
INSERT INTO `dr_archive` VALUES (24, 'AR-O-002', '执行部', '学院的行动部门，负责高危任务与龙类威胁处置。', '执行部，卡塞尔学院的行动部门。负责所有高危任务的策划与执行，以及对龙类威胁的直接处置。成员均为血统评级 B 以上的在校生或毕业生，行动代号制度为其特色。', 1004, 2010, 2, '内部', '执行部,行动部门,高危任务', '执行部人事科', 3410, 89, 0, 1, 0, '2026-10-23 14:23:32', '2026-10-23 14:23:32');
INSERT INTO `dr_archive` VALUES (25, 'AR-O-003', '校董会', '学院最高决策机构，成员身份与决议均为绝密。', '校董会，卡塞尔学院的最高决策机构。其成员身份、议事过程与决议内容均为绝密信息。已知其掌握学院全部战略资源调度权，并可绕过执行部直接下达指令。', 1004, 2010, 4, '绝密', '校董会,绝密,决策机构', '绝密档案室', 2260, 83, 0, 1, 0, '2026-10-23 14:23:32', '2026-10-23 14:23:32');
INSERT INTO `dr_archive` VALUES (26, 'AR-O-004', '蛇岐八家', '日本混血种家族联盟，与学院保持有限合作。', '蛇岐八家，日本境内八个混血种家族组成的联盟。与卡塞尔学院保持有限合作关系，在东亚地区拥有独立的行动体系。其持有的龙文典籍数量仅次于学院地下图书馆。', 1004, 2011, 3, '机密', '外部势力,日本,家族联盟', '外事联络处', 1630, 66, 0, 1, 0, '2026-10-23 14:23:32', '2026-10-23 14:23:32');
INSERT INTO `dr_archive` VALUES (27, 'AR-O-005', '秘党', '独立于学院之外的混血种秘密结社，立场不明。', '秘党，独立于卡塞尔学院之外的混血种秘密结社。其组织结构、成员规模与最终目标均不明。已知其多次在学院行动前一步取得关键龙文残卷，立场判定为「不友好但非敌对」。', 1004, 2011, 4, '绝密', '外部势力,秘党,立场不明', '绝密档案室', 2080, 77, 0, 1, 0, '2026-10-23 14:23:32', '2026-10-23 14:23:32');
INSERT INTO `dr_archive` VALUES (28, 'AR-D-001', '龙文', '龙族语言文字体系，一切言灵的载体。', '龙文，龙族的语言与文字体系，是一切言灵的载体与触发媒介。其语法结构与人类语言完全不同，破译工作已持续逾百年，目前完成度不足四成。学院地下图书馆藏有残卷，阅读需 A 级以上权限。', 1005, 2012, 3, '核心', '文献,龙族语言,权限受限', '龙文研究院', 4490, 94, 1, 1, 0, '2026-10-23 14:23:32', '2026-10-23 14:23:32');
INSERT INTO `dr_archive` VALUES (29, 'AR-D-002', '冰海残卷', '于冰海勘探中取得的龙文残卷，记载白王权柄碎片线索。', '冰海残卷，学院在冰海勘探任务中取得的七枚龙文残卷统称。其内容指向白王权柄碎片的下落，破译工作由龙文研究院与绝密档案室联合进行，进度列为机密。', 1005, 2012, 4, '绝密', '文献,残卷,白王,绝密', '绝密档案室', 2710, 85, 0, 1, 0, '2026-10-23 14:23:32', '2026-10-23 14:23:32');
INSERT INTO `dr_archive` VALUES (30, 'AR-D-003', '青铜计划', '学院历史上规模最大的单次屠龙作战计划。', '青铜计划，卡塞尔学院历史上投入规模最大的单次屠龙作战计划，目标为青铜与火之王诺顿。计划细节与最终战果均已封存，仅校董会与执行部最高层可申请查阅。', 1005, 2013, 4, '绝密', '任务卷宗,青铜计划,封存', '绝密档案室', 3980, 90, 0, 1, 0, '2026-10-23 14:23:32', '2026-10-23 14:23:32');
INSERT INTO `dr_archive` VALUES (31, 'AR-D-004', '屠龙行动记录', '执行部历年高危行动的归档记录汇编。', '屠龙行动记录，执行部历年来所有高危行动的归档汇编。包含任务目标、参与人员、战损情况与后续处置。记录按密级分层存放，B 级以上专员可申请查阅本人参与部分。', 1005, 2013, 3, '机密', '任务卷宗,行动记录,机密', '执行部档案科', 1830, 70, 0, 1, 0, '2026-10-23 14:23:32', '2026-10-23 14:23:32');
INSERT INTO `dr_archive` VALUES (32, 'AR-R-001', '血统鉴定', '对新生混血种血统浓度进行评估分级的制度。', '血统鉴定，对新生及在校混血种的血统浓度进行评估分级的制度。评级分为 C / B / A / S 四档，决定培养方案、任务准入资格以及可接触的档案密级。鉴定由学院医务室与龙类研究院联合执行。', 1006, 2014, 1, '公开', '制度,新生,分级,血统', '学院教务厅', 3620, 86, 1, 1, 0, '2026-10-23 14:23:32', '2026-10-23 14:23:32');
INSERT INTO `dr_archive` VALUES (33, 'AR-R-002', '分级评估标准', '血统评级的量化标准与复核流程。', '分级评估标准，血统 C/B/A/S 四档评级的量化依据。包含龙文亲和度、血统觉醒速度、言灵强度与稳定性四个维度。评级结果每学年复核一次，S 级需由校长亲自复核。', 1006, 2014, 2, '内部', '制度,评级,量化标准', '龙类研究院', 1270, 56, 0, 1, 0, '2026-10-23 14:23:32', '2026-10-23 14:23:32');
INSERT INTO `dr_archive` VALUES (34, 'AR-R-003', '学院守则总则', '在校混血种必须遵守的基本行为准则。', '学院守则总则，所有在校混血种必须遵守的基本行为准则。核心条款包括：不得在非授权场合使用言灵、不得向外界透露学院真实职能、不得私自进入里世界。违反者视情节给予警告至封印处理。', 1006, 2015, 1, '公开', '制度,守则,行为准则', '学院教务厅', 2410, 74, 0, 1, 0, '2026-10-23 14:23:32', '2026-10-23 14:23:32');
INSERT INTO `dr_archive` VALUES (35, 'AR-R-004', '档案借阅权限条例', '规定不同身份可接触的档案密级上限。', '档案借阅权限条例，规定不同在校身份可接触的档案密级上限：访客与未认证者 1 级（公开），新生与在校生 2 级（内部），执行部成员 3 级（机密），教员与研究员 4 级（绝密）。越级查阅将触发审计告警。', 1006, 2015, 2, '内部', '制度,权限,密级,审计', '学院档案委员会', 1590, 64, 0, 1, 0, '2026-10-23 14:23:32', '2026-10-23 14:23:32');

-- ----------------------------
-- Table structure for dr_archive_category
-- ----------------------------
DROP TABLE IF EXISTS `dr_archive_category`;
CREATE TABLE `dr_archive_category`  (
  `id` bigint NOT NULL COMMENT '主键（固定值，便于种子数据引用父子关系）',
  `parent_id` bigint NOT NULL DEFAULT 0 COMMENT '父级分类 ID，0 = 一级分类',
  `code` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分类 code，全局唯一（如 person / person.mixed）',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分类名称',
  `icon` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'archive' COMMENT '前端图标名（对应 AppIcon.vue）',
  `sort_no` int NOT NULL DEFAULT 0 COMMENT '排序号，小的在前',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '1=启用 0=停用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_code`(`code` ASC) USING BTREE,
  INDEX `idx_parent`(`parent_id` ASC, `sort_no` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '档案分类表（两级：parent_id=0 为一级）' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of dr_archive_category
-- ----------------------------
INSERT INTO `dr_archive_category` VALUES (1001, 0, 'person', '人物志', 'user', 1, 1, '2026-10-23 14:23:32', '2026-10-23 14:23:32');
INSERT INTO `dr_archive_category` VALUES (1002, 0, 'spirit', '言灵谱', 'sparkle', 2, 1, '2026-10-23 14:23:32', '2026-10-23 14:23:32');
INSERT INTO `dr_archive_category` VALUES (1003, 0, 'place', '地理志', 'flame', 3, 1, '2026-10-23 14:23:32', '2026-10-23 14:23:32');
INSERT INTO `dr_archive_category` VALUES (1004, 0, 'org', '机构志', 'shield', 4, 1, '2026-10-23 14:23:32', '2026-10-23 14:23:32');
INSERT INTO `dr_archive_category` VALUES (1005, 0, 'lore', '文献卷', 'archive', 5, 1, '2026-10-23 14:23:32', '2026-10-23 14:23:32');
INSERT INTO `dr_archive_category` VALUES (1006, 0, 'rule', '制度典', 'check', 6, 1, '2026-10-23 14:23:32', '2026-10-23 14:23:32');
INSERT INTO `dr_archive_category` VALUES (2001, 1001, 'person.mixed', '混血种名册', 'user', 1, 1, '2026-10-23 14:23:32', '2026-10-23 14:23:32');
INSERT INTO `dr_archive_category` VALUES (2002, 1001, 'person.dragonking', '龙王系谱', 'flame', 2, 1, '2026-10-23 14:23:32', '2026-10-23 14:23:32');
INSERT INTO `dr_archive_category` VALUES (2003, 1001, 'person.executor', '执行部专员', 'shield', 3, 1, '2026-10-23 14:23:32', '2026-10-23 14:23:32');
INSERT INTO `dr_archive_category` VALUES (2004, 1002, 'spirit.attack', '攻击型言灵', 'flame', 1, 1, '2026-10-23 14:23:32', '2026-10-23 14:23:32');
INSERT INTO `dr_archive_category` VALUES (2005, 1002, 'spirit.support', '辅助型言灵', 'sparkle', 2, 1, '2026-10-23 14:23:32', '2026-10-23 14:23:32');
INSERT INTO `dr_archive_category` VALUES (2006, 1002, 'spirit.eye', '瞳术系言灵', 'eye', 3, 1, '2026-10-23 14:23:32', '2026-10-23 14:23:32');
INSERT INTO `dr_archive_category` VALUES (2007, 1002, 'spirit.forbidden', '禁咒级言灵', 'alert', 4, 1, '2026-10-23 14:23:32', '2026-10-23 14:23:32');
INSERT INTO `dr_archive_category` VALUES (2008, 1003, 'place.inner', '里世界', 'flame', 1, 1, '2026-10-23 14:23:32', '2026-10-23 14:23:32');
INSERT INTO `dr_archive_category` VALUES (2009, 1003, 'place.facility', '学院设施', 'shield', 2, 1, '2026-10-23 14:23:32', '2026-10-23 14:23:32');
INSERT INTO `dr_archive_category` VALUES (2010, 1004, 'org.college', '学院机构', 'shield', 1, 1, '2026-10-23 14:23:32', '2026-10-23 14:23:32');
INSERT INTO `dr_archive_category` VALUES (2011, 1004, 'org.outside', '外部势力', 'alert', 2, 1, '2026-10-23 14:23:32', '2026-10-23 14:23:32');
INSERT INTO `dr_archive_category` VALUES (2012, 1005, 'lore.dragonlang', '龙文典籍', 'archive', 1, 1, '2026-10-23 14:23:32', '2026-10-23 14:23:32');
INSERT INTO `dr_archive_category` VALUES (2013, 1005, 'lore.mission', '任务卷宗', 'ticket', 2, 1, '2026-10-23 14:23:32', '2026-10-23 14:23:32');
INSERT INTO `dr_archive_category` VALUES (2014, 1006, 'rule.blood', '血统制度', 'sparkle', 1, 1, '2026-10-23 14:23:32', '2026-10-23 14:23:32');
INSERT INTO `dr_archive_category` VALUES (2015, 1006, 'rule.code', '学院守则', 'check', 2, 1, '2026-10-23 14:23:32', '2026-10-23 14:23:32');

-- ----------------------------
-- Table structure for dr_hot_keyword
-- ----------------------------
DROP TABLE IF EXISTS `dr_hot_keyword`;
CREATE TABLE `dr_hot_keyword`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `keyword` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '检索词',
  `heat` int NOT NULL DEFAULT 0 COMMENT '热度',
  `sort_no` int NOT NULL DEFAULT 0 COMMENT '排序号，小的在前',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '1=启用 0=停用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_keyword`(`keyword` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '热门检索词' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of dr_hot_keyword
-- ----------------------------
INSERT INTO `dr_hot_keyword` VALUES (1, '龙王', 980, 1, 1, '2026-10-23 14:23:32', '2026-10-23 14:23:32');
INSERT INTO `dr_hot_keyword` VALUES (2, '言灵', 940, 2, 1, '2026-10-23 14:23:32', '2026-10-23 14:23:32');
INSERT INTO `dr_hot_keyword` VALUES (3, '尼伯龙根', 860, 3, 1, '2026-10-23 14:23:32', '2026-10-23 14:23:32');
INSERT INTO `dr_hot_keyword` VALUES (4, '血统鉴定', 720, 4, 1, '2026-10-23 14:23:32', '2026-10-23 14:23:32');
INSERT INTO `dr_hot_keyword` VALUES (5, '青铜与火', 690, 5, 1, '2026-10-23 14:23:32', '2026-10-23 14:23:32');
INSERT INTO `dr_hot_keyword` VALUES (6, '村雨', 610, 6, 1, '2026-10-23 14:23:32', '2026-10-23 14:23:32');
INSERT INTO `dr_hot_keyword` VALUES (7, '龙文', 580, 7, 1, '2026-10-23 14:23:32', '2026-10-23 14:23:32');
INSERT INTO `dr_hot_keyword` VALUES (8, '白王', 520, 8, 1, '2026-10-23 14:23:32', '2026-10-23 14:23:32');

-- ----------------------------
-- Table structure for dr_notice
-- ----------------------------
DROP TABLE IF EXISTS `dr_notice`;
CREATE TABLE `dr_notice`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `code` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '公告编号，唯一',
  `title` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '公告标题',
  `content` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '公告正文',
  `level` tinyint NOT NULL DEFAULT 1 COMMENT '紧急度：1=普通 2=重要 3=紧急',
  `pinned` tinyint NOT NULL DEFAULT 0 COMMENT '是否置顶：1=置顶 0=普通',
  `publisher` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '学院教务厅' COMMENT '发布部门',
  `publish_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
  `expire_at` datetime NULL DEFAULT NULL COMMENT '失效时间，NULL=长期有效',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '1=已发布 0=下架',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_code`(`code` ASC) USING BTREE,
  INDEX `idx_publish`(`pinned` ASC, `publish_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '首页公告 / 学院通报' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of dr_notice
-- ----------------------------
INSERT INTO `dr_notice` VALUES (1, 'NT-2026-001', '龙王级血统波动预警：本区里世界入口活跃度异常上升', '监测网显示本区里世界入口活跃度较上周上升 47%。执行部已进入二级战备，所有专员取消休假。非授权人员严禁接近 B3 层及以下区域。', 3, 1, '执行部指挥中心', '2026-10-15 09:00:00', '2026-10-22 23:59:59', 1, 0, '2026-10-23 14:23:32', '2026-10-23 14:23:32');
INSERT INTO `dr_notice` VALUES (2, 'NT-2026-002', '地下图书馆 B3 层封闭检修，借阅服务暂停一周', '因龙文恒温系统例行检修，地下图书馆 B3 层于 10 月 16 日至 10 月 22 日封闭，期间暂停一切借阅与查阅服务。B1-B2 层正常开放。', 2, 1, '学院教务厅', '2026-10-14 15:30:00', '2026-10-23 00:00:00', 1, 0, '2026-10-23 14:23:32', '2026-10-23 14:23:32');
INSERT INTO `dr_notice` VALUES (3, 'NT-2026-003', '2026 级新生血统鉴定安排通知', '2026 级新生血统鉴定将于本月 20 日起分批进行，具体批次与时间见各班级通知。鉴定前 24 小时内请勿使用任何言灵。', 1, 0, '学院医务室', '2026-10-13 10:00:00', '2026-10-25 23:59:59', 1, 0, '2026-10-23 14:23:32', '2026-10-23 14:23:32');

-- ----------------------------
-- Table structure for dr_reset_code
-- ----------------------------
DROP TABLE IF EXISTS `dr_reset_code`;
CREATE TABLE `dr_reset_code`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `account` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '对应的学院编号（学号）',
  `code` varchar(6) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '6 位数字验证龙文',
  `expire_time` datetime NOT NULL COMMENT '过期时间',
  `used` tinyint NOT NULL DEFAULT 0 COMMENT '0=未使用 1=已使用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下发时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_account`(`account` ASC, `create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '密钥重置验证码表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of dr_reset_code
-- ----------------------------
INSERT INTO `dr_reset_code` VALUES (1, 'REGf4c00e74d4f942d0', '954588', '2026-10-23 14:53:49', 1, '2026-10-23 14:48:49');
INSERT INTO `dr_reset_code` VALUES (2, 'REG498a4529512f2acb', '185567', '2026-10-26 08:58:27', 0, '2026-10-26 08:53:27');
INSERT INTO `dr_reset_code` VALUES (3, 'CC20260001', '459723', '2026-10-26 09:01:13', 1, '2026-10-26 08:56:13');
INSERT INTO `dr_reset_code` VALUES (4, 'CC20260001', '359638', '2026-10-26 09:10:57', 1, '2026-10-26 09:05:57');
INSERT INTO `dr_reset_code` VALUES (5, 'CC202600004', '122889', '2026-10-26 09:11:16', 1, '2026-10-26 09:06:16');
INSERT INTO `dr_reset_code` VALUES (6, 'CC20260001', '349669', '2026-10-26 09:12:27', 1, '2026-10-26 09:07:27');
INSERT INTO `dr_reset_code` VALUES (7, 'CC20260001', '938303', '2026-10-26 09:17:20', 0, '2026-10-26 09:12:20');
INSERT INTO `dr_reset_code` VALUES (8, 'CC20260001', '365194', '2026-10-26 09:20:29', 0, '2026-10-26 09:15:28');
INSERT INTO `dr_reset_code` VALUES (9, 'CC20260001', '283040', '2026-10-26 09:29:04', 0, '2026-10-26 09:24:04');

-- ----------------------------
-- Table structure for dr_search_log
-- ----------------------------
DROP TABLE IF EXISTS `dr_search_log`;
CREATE TABLE `dr_search_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NULL DEFAULT NULL COMMENT '检索人 ID，未登录为 NULL',
  `keyword` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '检索关键词',
  `top_category` bigint NULL DEFAULT NULL COMMENT '一级分类筛选',
  `category_id` bigint NULL DEFAULT NULL COMMENT '二级分类筛选',
  `level` tinyint NULL DEFAULT NULL COMMENT '密级筛选',
  `result_count` int NOT NULL DEFAULT 0 COMMENT '命中条数',
  `client_ip` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '来源 IP',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '检索时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_keyword`(`keyword` ASC) USING BTREE,
  INDEX `idx_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 32 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '检索日志' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of dr_search_log
-- ----------------------------
INSERT INTO `dr_search_log` VALUES (1, 4, '', NULL, NULL, NULL, 18, '127.0.0.1', '2026-10-23 14:49:36');
INSERT INTO `dr_search_log` VALUES (2, 4, '', NULL, NULL, NULL, 18, '127.0.0.1', '2026-10-23 14:51:48');
INSERT INTO `dr_search_log` VALUES (3, 4, '', NULL, NULL, NULL, 18, '127.0.0.1', '2026-10-23 14:52:16');
INSERT INTO `dr_search_log` VALUES (4, 4, '', NULL, NULL, NULL, 18, '127.0.0.1', '2026-10-23 14:53:24');
INSERT INTO `dr_search_log` VALUES (5, 4, '', NULL, NULL, NULL, 18, '127.0.0.1', '2026-10-23 14:53:32');
INSERT INTO `dr_search_log` VALUES (6, 4, '', NULL, NULL, NULL, 18, '127.0.0.1', '2026-10-23 14:53:36');
INSERT INTO `dr_search_log` VALUES (7, 4, '', NULL, NULL, NULL, 18, '127.0.0.1', '2026-10-23 14:57:02');
INSERT INTO `dr_search_log` VALUES (8, 1, '龙', NULL, NULL, NULL, 4, '127.0.0.1', '2026-10-26 08:56:13');
INSERT INTO `dr_search_log` VALUES (9, 4, '', NULL, NULL, NULL, 18, '127.0.0.1', '2026-10-26 09:08:50');
INSERT INTO `dr_search_log` VALUES (10, NULL, '龙', NULL, NULL, NULL, 1, '127.0.0.1', '2026-10-26 09:24:05');
INSERT INTO `dr_search_log` VALUES (11, NULL, '龙', NULL, NULL, NULL, 1, '127.0.0.1', '2026-10-26 09:24:28');
INSERT INTO `dr_search_log` VALUES (12, 4, '', NULL, NULL, NULL, 18, '127.0.0.1', '2026-10-26 09:29:11');
INSERT INTO `dr_search_log` VALUES (13, 4, '', NULL, NULL, NULL, 18, '127.0.0.1', '2026-10-26 09:36:11');
INSERT INTO `dr_search_log` VALUES (14, 4, '', NULL, NULL, NULL, 18, '127.0.0.1', '2026-10-26 09:40:53');
INSERT INTO `dr_search_log` VALUES (15, 4, '', NULL, NULL, NULL, 18, '127.0.0.1', '2026-10-26 09:46:15');
INSERT INTO `dr_search_log` VALUES (16, 4, '', NULL, NULL, NULL, 18, '127.0.0.1', '2026-10-26 09:48:39');
INSERT INTO `dr_search_log` VALUES (17, 4, '', NULL, NULL, NULL, 18, '127.0.0.1', '2026-10-29 14:38:57');
INSERT INTO `dr_search_log` VALUES (18, 4, '', NULL, NULL, NULL, 18, '127.0.0.1', '2026-10-29 14:43:10');
INSERT INTO `dr_search_log` VALUES (19, 4, '', NULL, NULL, NULL, 35, '127.0.0.1', '2026-10-29 14:50:45');
INSERT INTO `dr_search_log` VALUES (20, 4, '', NULL, NULL, NULL, 35, '127.0.0.1', '2026-10-29 14:50:58');
INSERT INTO `dr_search_log` VALUES (21, 4, '', NULL, NULL, NULL, 35, '127.0.0.1', '2026-10-29 14:51:07');
INSERT INTO `dr_search_log` VALUES (22, 4, '', NULL, NULL, NULL, 35, '127.0.0.1', '2026-10-29 14:51:39');
INSERT INTO `dr_search_log` VALUES (23, 4, '', NULL, NULL, NULL, 35, '127.0.0.1', '2026-10-29 14:52:00');
INSERT INTO `dr_search_log` VALUES (24, 4, '', NULL, NULL, NULL, 35, '127.0.0.1', '2026-10-29 14:52:41');
INSERT INTO `dr_search_log` VALUES (25, 4, '', NULL, NULL, NULL, 35, '127.0.0.1', '2026-10-29 14:52:59');
INSERT INTO `dr_search_log` VALUES (26, 4, '', NULL, NULL, NULL, 35, '127.0.0.1', '2026-10-29 14:58:05');
INSERT INTO `dr_search_log` VALUES (27, 4, '', NULL, NULL, NULL, 35, '127.0.0.1', '2026-10-29 14:59:36');
INSERT INTO `dr_search_log` VALUES (28, 1, '', NULL, NULL, NULL, 18, '127.0.0.1', '2026-10-29 15:03:05');
INSERT INTO `dr_search_log` VALUES (29, 1, '', NULL, NULL, NULL, 18, '127.0.0.1', '2026-10-29 15:05:09');
INSERT INTO `dr_search_log` VALUES (30, 1, '', NULL, NULL, NULL, 18, '127.0.0.1', '2026-10-29 15:05:13');
INSERT INTO `dr_search_log` VALUES (31, 1, '', NULL, NULL, NULL, 18, '127.0.0.1', '2026-10-29 15:09:12');

-- ----------------------------
-- Table structure for dr_spirit
-- ----------------------------
DROP TABLE IF EXISTS `dr_spirit`;
CREATE TABLE `dr_spirit`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `spirit_key` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '路由 key，如 junyan（对应 /spirit/junyan 与插画 /img/spirit/junyan.webp）',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '言灵名',
  `element` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '元素系别：火/冰/风/重力/时间/精神/暗影/雷/威压…',
  `level` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '内部' COMMENT '密级：内部/机密/高危/绝密',
  `effect` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '言灵效果（一句话）',
  `intro` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '详细介绍',
  `holder` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '典型持有者',
  `note` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '使用禁忌 / 备注',
  `image_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '插画图片网址（http/https），非空时优先于本地 /img/spirit/<key>.webp',
  `sort` int NOT NULL DEFAULT 0 COMMENT '展示排序，越小越靠前',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '1=展示 0=隐藏',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=未删除 1=已删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_spirit_key`(`spirit_key` ASC) USING BTREE,
  INDEX `idx_sort`(`sort` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 17 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '言灵图鉴表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of dr_spirit
-- ----------------------------
INSERT INTO `dr_spirit` VALUES (1, 'junyan', '君焰', '火', '高危', '领域内可燃物瞬间爆燃', '以血统呼唤高阶火焰，在领域内形成绝对压制。稳定释放时可将空气加热至燃点，是正面攻坚中最具破坏力的言灵之一。相传释放者眼中会燃起金色竖瞳。', '攻坚型混血种 · 多见于战斗编制', '持续释放会灼伤自身血脉，超过三分钟即有失控风险。', '', 1, 1, 0, '2026-10-23 15:00:22', '2026-10-23 15:00:22');
INSERT INTO `dr_spirit` VALUES (2, 'shiling', '时零', '时间', '机密', '万物迟滞，唯我不动', '让领域内的时间流速放缓，使用者几乎获得一段「静止时间」的观察与行动窗口。对精神与体力消耗极大，是执行部部长的招牌言灵。', '执行部高级专员 · 需 S 级以上血统', '每次释放都在燃烧寿命，频繁使用者普遍早衰。', '', 2, 1, 0, '2026-10-23 15:00:22', '2026-10-23 15:00:22');
INSERT INTO `dr_spirit` VALUES (3, 'fengwang', '风王之瞳', '风', '高危', '风刃无形，触之即断', '凝聚风之王的竖瞳，领域内生成无色无形的风刃，可洞穿一切实体。因无法被肉眼捕捉，是最难防御的突袭类言灵。', '突袭型混血种 · 擅长单兵作战', '风刃不分敌我，混战中极易误伤同伴。', '', 3, 1, 0, '2026-10-23 15:00:22', '2026-10-23 15:00:22');
INSERT INTO `dr_spirit` VALUES (4, 'bronze', '青铜御座', '重力', '机密', '重力由我定义', '以自身为中心重构重力场，可抬升万钧之物，也可让敌人当场跪伏。传说中曾用于托起整艘倾覆的巨轮。', '重装型混血种 · 常任现场指挥', '重力反转的瞬间使用者自身骨骼同样承受数倍负荷。', '', 4, 1, 0, '2026-10-23 15:00:22', '2026-10-23 15:00:22');
INSERT INTO `dr_spirit` VALUES (5, 'zhulong', '烛龙', '冰', '绝密', '千里冰封', '极寒吐息所过之处一切冻结。传说中烛龙睁眼为昼、闭目为夜，此言灵一旦失控将引发区域性极寒灾害，故编列为绝密级。', '禁忌级持有者 · 全学院记录在册者不足三人', '释放后体温骤降，须在医疗部监护下复苏。', '', 5, 1, 0, '2026-10-23 15:00:22', '2026-10-23 15:00:22');
INSERT INTO `dr_spirit` VALUES (6, 'wangquan', '王权', '威压', '绝密', '血统即律法', '血统的绝对压制。领域内一切低阶混血种会因本能的臣服而跪倒，无法直视使用者。它不属于任何元素系，是「王」的证明。', '龙王血裔 · 非传承不可习得', '对同阶及以上血统无效，一旦失效将遭到反噬。', '', 6, 1, 0, '2026-10-23 15:00:22', '2026-10-23 15:00:22');
INSERT INTO `dr_spirit` VALUES (7, 'lianyou', '镰鼬', '风', '高危', '三道真空风刃，肉眼不可见', '以极细的真空刃切开空气，形成三道交错的弧形轨迹。被斩者往往在察觉疼痛之前就已负伤，是潜行与伏击中最难预警的言灵。', '侦察 / 追踪专员', '觉醒后听觉被永久强化，畏强噪音，须长期佩戴护具。', '', 7, 1, 0, '2026-10-23 15:00:22', '2026-10-23 15:00:22');
INSERT INTO `dr_spirit` VALUES (8, 'chiri', '炽日', '火', '绝密', '凝出微型恒星，领域内一切汽化', '把释放者的血统燃烧到极致，在掌心凝出一颗白炽的微型恒星。其光芒可穿透一切遮蔽，其热度足以让钢铁在瞬间蒸发。', '屠龙级战力 · 需学院最高会议授权方可记录', '释放后使用者会陷入长达数日的血统衰竭，期间形同废人。', '', 8, 1, 0, '2026-10-23 15:00:22', '2026-10-23 15:00:22');
INSERT INTO `dr_spirit` VALUES (9, 'jingmi', '静谧', '精神', '机密', '领域内一切声音湮灭', '以精神力抹平空气的振动，让领域内的声音彻底消失。可用于潜行，也可让敌人在无声中丧失方位感与协同能力。', '潜入 / 暗杀专员', '长期使用会侵蚀使用者的听觉与语言能力。', '', 9, 1, 0, '2026-10-23 15:00:22', '2026-10-23 15:00:22');
INSERT INTO `dr_spirit` VALUES (10, 'mingzhao', '冥照', '暗影', '绝密', '绝对黑暗领域，剥夺一切视觉', '吞噬领域内的全部光线，只留下释放者视野中的轮廓。在黑暗中，唯有血脉相近者能彼此辨认，其余人皆沦为盲者。', '情报 / 肃清专员', '黑暗中视物的代价，是使用者会逐渐失去对色彩的感知。', '', 10, 1, 0, '2026-10-23 15:00:22', '2026-10-23 15:00:22');
INSERT INTO `dr_spirit` VALUES (11, 'binghai', '冰海', '冰', '机密', '瞳孔所及，皆成冰原', '视线扫过之处海水凝滞、空气结晶，可将一整片海湾冻结成白原。与烛龙同源而威能远逊，胜在消耗可控，是极地任务中的战略级支援言灵。', '极地行动编制 · 冷区特训合格者', '冻结范围内使用者自身同样畏寒，须装备恒温护甲执行。', '', 11, 1, 0, '2026-10-23 15:00:22', '2026-10-23 15:00:22');
INSERT INTO `dr_spirit` VALUES (12, 'xinwang', '心网', '精神', '内部', '听见方圆百米的心跳', '以精神力织成一张无形之网，网内一切生物的心跳、情绪与杀意都会在释放者脑中成形。无法读取具体思维，但谎言在它面前毫无意义。', '审讯 / 监察编制 · 风纪委员会外聘', '人群密集处长时间开启会引发精神过载，须限时使用。', '', 12, 1, 0, '2026-10-23 15:00:22', '2026-10-23 15:00:22');
INSERT INTO `dr_spirit` VALUES (13, 'guzhong', '骨冢', '暗影', '绝密', '白骨为冢，亡者为兵', '让领域内残存的骨骼应声而起，听凭释放者驱使。传说最高纪录曾唤起一整支沉没战船的骸骨军团，故被列为本代封存级言灵。', '禁忌级持有者 · 记录在案者仅一人', '被驱使的亡骨会缓慢反噬操纵者的血肉，每次使用都在折损寿数。', '', 13, 1, 0, '2026-10-23 15:00:22', '2026-10-23 15:00:22');
INSERT INTO `dr_spirit` VALUES (14, 'yulei', '御雷', '雷', '高危', '雷霆为我执剑', '在高空电离层与释放者之间架起「引线」，将落雷引为己用。导引精度高时可将雷霆约束成一道笔直的惩罚之剑，是压制成建制冷兵器的言灵。', '攻坚型混血种 · 需雷暴环境或天气支援', '雷雨天气威力翻倍，无云晴天几乎不可释放。', '', 14, 1, 0, '2026-10-23 15:00:22', '2026-10-23 15:00:22');
INSERT INTO `dr_spirit` VALUES (15, 'test-spirit-2', '测试言灵改', '测试', '机密', 'x', 'x', '', '', '', 98, 0, 1, '2026-10-23 15:01:25', '2026-10-23 15:01:44');
INSERT INTO `dr_spirit` VALUES (16, 'key', '黑日', '测试', '高危', '领域生成‌：施放后形成旋转的黑色日轮领域，看起来像日全食中的太阳，刀锋划出的圆弧赤红发亮。 ‌引力拉扯‌：领域产生强引力场，吞噬空气掀起飓风，将周围物体、海水甚至尸守拉向中心。尸守需抠紧地面以免被吸走', '黑日，一种火系言灵能力。凡是接触到黑日领域的事物都会化为灰烬。当黑日的释放者死时，黑日就会坍塌，吞噬周围的一切掀起巨大的风暴，形成一个绝大的引力场。', '', '', '', 50, 1, 0, '2026-10-23 15:06:45', '2026-10-29 14:56:40');

-- ----------------------------
-- Table structure for dr_user
-- ----------------------------
DROP TABLE IF EXISTS `dr_user`;
CREATE TABLE `dr_user`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `account` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '学院编号（学号），如 CC20260001',
  `codename` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '姓名 / 行动代号',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'BCrypt 加密后的言灵密钥',
  `identity` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'freshman' COMMENT '在校身份 code：freshman/student/executor/teacher/guest',
  `spirit` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'none' COMMENT '言灵 code：none/junyan/bronze/shiling/fengwang/zhulong/wangquan',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1=在册 0=封印（禁用）',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=未删除 1=已删除',
  `login_count` int NOT NULL DEFAULT 0 COMMENT '累计登录次数',
  `last_login_at` datetime NULL DEFAULT NULL COMMENT '最近登录时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登记时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_account`(`account` ASC) USING BTREE,
  UNIQUE INDEX `uk_codename`(`codename` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '混血种（用户）表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of dr_user
-- ----------------------------
INSERT INTO `dr_user` VALUES (1, 'CC20260001', '路明非', '$2a$10$6Hzup9QGKCA9Hx8S4ouXKuGOVV27/2z4cb1.zpPYHgVP0hagU.SZS', 'freshman', 'junyan', 1, 0, 18, '2026-10-29 15:09:10', '2026-10-23 14:45:24', '2026-10-29 15:09:10');
INSERT INTO `dr_user` VALUES (2, 'CC20260002', '楚子航', '$2b$10$EhhQtz0J7vukNhH3zCoBiew.9/FOuG9I/pE3uKWRR.7JcZxq150ou', 'executor', 'shiling', 1, 0, 0, NULL, '2026-10-23 14:45:24', '2026-10-23 14:45:24');
INSERT INTO `dr_user` VALUES (3, 'CC20260003', '芬格尔', '$2b$10$aamVUE83rOCVisNGuH2TbOJHUrEfbXNcNzvwSOsmnASalROlxKlAC', 'student', 'fengwang', 1, 0, 0, NULL, '2026-10-23 14:45:24', '2026-10-23 14:45:24');
INSERT INTO `dr_user` VALUES (4, 'CC202600004', '孤傲', '$2a$10$q8Im82jt0rLviJcbujD1POsUJAqX5QrLTEqGN1ZdpR9QnUpcK2Thu', 'teacher', 'none', 1, 0, 4, '2026-10-29 14:51:37', '2026-10-23 14:49:24', '2026-10-29 14:51:37');

SET FOREIGN_KEY_CHECKS = 1;
