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

 Date: 19/10/2026 09:42:47
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for dr_archive
-- ----------------------------
DROP TABLE IF EXISTS `dr_archive`;
CREATE TABLE `dr_archive`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `code` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '鍗峰畻缂栧彿锛屽敮涓?紙濡?AR-P-001锛',
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '鍗峰畻鏍囬?',
  `summary` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '鎽樿?锛堝垪琛ㄥ睍绀猴級',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '姝ｆ枃锛堣?鎯呭睍绀猴級',
  `top_category_id` bigint NOT NULL COMMENT '涓?骇鍒嗙被 ID锛堝啑浣欙紝渚夸簬鎸夊ぇ绫荤瓫閫夛級',
  `category_id` bigint NOT NULL COMMENT '浜岀骇鍒嗙被 ID',
  `security_level` tinyint NOT NULL DEFAULT 1 COMMENT '瀵嗙骇锛?=鍏?紑 2=鍐呴儴 3=鏈哄瘑 4=缁濆瘑',
  `rank_label` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '璇勭骇鏍囩?锛歋 / A / B / 榫欑帇 / 绂佸拻 / 绂佸尯鈥',
  `tags` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '鏍囩?锛岃嫳鏂囬?鍙峰垎闅',
  `source` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '鏉ユ簮 / 褰掓。閮ㄩ棬',
  `view_count` int NOT NULL DEFAULT 0 COMMENT '娴忚?閲',
  `heat` int NOT NULL DEFAULT 0 COMMENT '鐑?害锛堥?椤垫帓搴忕敤锛',
  `spotlight` tinyint NOT NULL DEFAULT 0 COMMENT '鏄?惁棣栭〉绮鹃?锛?=绮鹃? 0=鏅??',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '1=鍏?紑涓婃灦 0=灏佸瓨',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '閫昏緫鍒犻櫎锛?=鏈?垹闄?1=宸插垹闄',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '褰掓。鏃堕棿',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_code`(`code` ASC) USING BTREE,
  INDEX `idx_top_cat`(`top_category_id` ASC, `security_level` ASC) USING BTREE,
  INDEX `idx_cat`(`category_id` ASC, `security_level` ASC) USING BTREE,
  INDEX `idx_heat`(`heat` ASC) USING BTREE,
  INDEX `idx_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 36 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '妗ｆ?涓昏〃锛堝惈瀵嗙骇鍒嗙骇锛' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of dr_archive
-- ----------------------------
INSERT INTO `dr_archive` VALUES (1, 'AR-P-001', '路明非', '卡塞尔学院 2026 级新生，学院史上首位 S 级评估的混血种，血统来源至今不明。', '路明非，男，2026 级新生。入学血统评级为 S，是本校自建校以来第一位在入学鉴定中即取得 S 级评价的学生。其血统谱系无法追溯至任何已知混血种家族，基因组比对结果与现存所有龙王系谱均存在部分吻合，原因待查。目前已进入执行部重点关注名单。', 1001, 2001, 2, 'S', '混血种,S级,新生,执行部重点关注', '学院教务处', 3860, 98, 1, 1, 0, '2026-10-19 08:46:20', '2026-10-19 08:46:20');
INSERT INTO `dr_archive` VALUES (2, 'AR-P-002', '楚子航', '执行部王牌专员，代号「村雨」，持言灵·风王之瞳，剑术精湛。', '楚子航，执行部 A 级专员，行动代号「村雨」。持有瞳术系言灵·风王之瞳，可洞穿幻象与谎言。擅长近身剑术，参与过多次高危任务，是执行部现存战力评估最高的专员之一。性格冷静自持，任务完成率 100%。', 1001, 2003, 2, 'A', '执行部,言灵使,村雨,瞳术', '执行部人事科', 3240, 95, 1, 1, 0, '2026-10-19 08:46:20', '2026-10-19 08:46:20');
INSERT INTO `dr_archive` VALUES (3, 'AR-P-003', '芬格尔', '2026 级在校生，血统评级 B，以情报搜集与伪装渗透见长。', '芬格尔，2026 级在校生，血统评级 B。虽评级不高，但在情报搜集、伪装渗透与社交工程方面展现出极高天赋，多次为执行部提供关键线索。其持有的言灵·时零在实战中主要用于脱离与规避。', 1001, 2001, 1, 'B', '在校生,情报,伪装,时零', '学院教务处', 1580, 62, 0, 1, 0, '2026-10-19 08:46:20', '2026-10-19 08:46:20');
INSERT INTO `dr_archive` VALUES (4, 'AR-P-004', '凯撒·加图索', '执行部资深专员，血统评级 A，擅长正面攻坚与战术指挥。', '凯撒·加图索，执行部资深专员，血统评级 A。出身于意大利混血种世家加图索家族，家族与学院合作历史超过一百年。擅长正面攻坚与小组战术指挥，持有火系言灵。其家族背景使其在外部势力交涉中具有不可替代的作用。', 1001, 2003, 2, 'A', '执行部,加图索家族,攻坚,指挥', '执行部人事科', 2410, 80, 0, 1, 0, '2026-10-19 08:46:20', '2026-10-19 08:46:20');
INSERT INTO `dr_archive` VALUES (5, 'AR-P-005', '诺诺', '执行部专员，血统评级 A，擅长高速机动与侦察。', '诺诺，执行部专员，血统评级 A。以极高的机动性与侦察能力著称，多次独立完成深入里世界的侦察任务。与路明非、楚子航组成固定行动小组。', 1001, 2003, 2, 'A', '执行部,侦察,机动', '执行部人事科', 1960, 71, 0, 1, 0, '2026-10-19 08:46:20', '2026-10-19 08:46:20');
INSERT INTO `dr_archive` VALUES (6, 'AR-P-006', '苏恩曦', '学院医务室负责人，血统评级 B，精通混血种生理治理。', '苏恩曦，学院医务室负责人，血统评级 B。精通混血种生理结构与血统暴走救治，持有罕见的治疗类言灵。所有执行部专员在执行高危任务前必须经由她签署健康许可。', 1001, 2001, 2, 'B', '医务室,治疗,血统压制', '学院人事处', 1120, 54, 0, 1, 0, '2026-10-19 08:46:20', '2026-10-19 08:46:20');
INSERT INTO `dr_archive` VALUES (7, 'AR-P-007', '诺顿', '青铜与火之王的王座继承者，掌控青铜与火的双重权柄。', '诺顿，青铜与火之王的王座继承者。掌控青铜与火的双重权柄，是现存对混血种威胁等级最高的龙王之一。其活动轨迹与多次里世界异常波动高度相关。学院对其保持最高级别监控，任何相关线索需第一时间上报执行部。', 1001, 2002, 3, '龙王', '龙王,青铜与火,尼伯龙根,最高监控', '龙类研究院', 5210, 99, 1, 1, 0, '2026-10-19 08:46:20', '2026-10-19 08:46:20');
INSERT INTO `dr_archive` VALUES (8, 'AR-P-008', '康斯坦丁', '大地与山之王，与诺顿同为初代龙王直系。', '康斯坦丁，大地与山之王。与诺顿同属初代龙王直系，权柄覆盖大地与山脉。其苏醒征兆通常表现为大范围地壳微震，学院地质监测网已就此建立专项预警。', 1001, 2002, 3, '龙王', '龙王,大地与山,地质预警', '龙类研究院', 2870, 84, 0, 1, 0, '2026-10-19 08:46:20', '2026-10-19 08:46:20');
INSERT INTO `dr_archive` VALUES (9, 'AR-P-009', '白王', '龙族史上的叛变者，权柄为「精神」与「梦境」。', '白王，龙族史上唯一一位背叛龙王阵营并站在人类一侧的存在，权柄覆盖精神与梦境领域。其遗留的权柄碎片至今仍是多方势力争夺的焦点，任何一枚碎片的现世都可能引发连锁反应。', 1001, 2002, 4, '龙王', '龙王,精神,梦境,权柄碎片', '绝密档案室', 4330, 92, 0, 1, 0, '2026-10-19 08:46:20', '2026-10-19 08:46:20');
INSERT INTO `dr_archive` VALUES (10, 'AR-P-010', '昂热', '卡塞尔学院校长，血统强度与真实年龄均属绝密。', '昂热，卡塞尔学院校长。其血统强度、真实年龄与早年经历均为绝密信息，仅限校董会与执行部最高层查阅。已知信息：其在过去一个世纪中持续主导学院的屠龙战略，并与多位初代龙王有过正面交锋记录。', 1001, 2003, 4, '绝密', '校长,绝密,战略决策', '绝密档案室', 3720, 90, 1, 1, 0, '2026-10-19 08:46:20', '2026-10-19 08:46:20');
INSERT INTO `dr_archive` VALUES (11, 'AR-S-001', '言灵·君焰', '以龙文吟诵引动的高热领域，范围毁灭级言灵。', '言灵·君焰，火系最高阶言灵之一。以龙文吟诵引动，在指定范围内生成极高温度的领域，属于范围毁灭级。仅限 S 级及以上评估者接触原始卷宗，演练需在学院地下演练场进行。', 1002, 2004, 3, '禁咒', '言灵,禁咒级,火系,毁灭级', '言灵研究院', 4680, 97, 1, 1, 0, '2026-10-19 08:46:20', '2026-10-19 08:46:20');
INSERT INTO `dr_archive` VALUES (12, 'AR-S-002', '言灵·烛龙', '光与暗的双生言灵，可生成绝对黑暗领域。', '言灵·烛龙，传说中烛龙之眼的具现化。可在指定范围内生成绝对黑暗领域，同时持有者保留视觉。属于高阶言灵，实战中多用于突袭与撤退掩护。', 1002, 2004, 2, '高阶', '言灵,高阶,黑暗,突袭', '言灵研究院', 1720, 68, 0, 1, 0, '2026-10-19 08:46:20', '2026-10-19 08:46:20');
INSERT INTO `dr_archive` VALUES (13, 'AR-S-003', '言灵·时零', '短时间加速自身时间流速，高阶辅助型言灵。', '言灵·时零，高阶辅助型言灵。可在数秒内将持有者的主观时间流速提升至常人数倍，外界观察者看来近乎瞬移。副作用为神经负荷，连续使用不得超过三次。', 1002, 2005, 2, '高阶', '言灵,高阶,辅助,时间', '言灵研究院', 2210, 76, 0, 1, 0, '2026-10-19 08:46:20', '2026-10-19 08:46:20');
INSERT INTO `dr_archive` VALUES (14, 'AR-S-004', '言灵·镇魂', '安抚与压制类言灵，常用于抑制血统暴走。', '言灵·镇魂，中阶辅助型言灵。可对目标的精神活动产生安抚作用，是抑制混血种血统暴走的标准手段之一。执行部任务标配言灵，所有专员须通过基础使用考核。', 1002, 2005, 1, '中阶', '言灵,中阶,辅助,血统压制', '执行部训练科', 1450, 60, 0, 1, 0, '2026-10-19 08:46:20', '2026-10-19 08:46:20');
INSERT INTO `dr_archive` VALUES (15, 'AR-S-005', '言灵·风王之瞳', '瞳术系言灵，可洞穿谎言与幻象。', '言灵·风王之瞳，瞳术系高阶言灵。激活后可洞穿幻象与谎言，并在短时间内大幅提升视觉捕捉能力，是执行部侦查与审讯任务的核心手段。已知持有者：楚子航。', 1002, 2006, 2, '高阶', '言灵,瞳术,高阶,侦查', '言灵研究院', 2980, 88, 0, 1, 0, '2026-10-19 08:46:20', '2026-10-19 08:46:20');
INSERT INTO `dr_archive` VALUES (16, 'AR-S-006', '言灵·王权', '禁咒级言灵，可对所有低阶龙类施加绝对压制。', '言灵·王权，禁咒级言灵。可对血统位阶低于持有者的龙类施加绝对压制，短暂剥夺其行动能力。使用代价极高，每次激活后持有者需至少72小时恢复期。已知持有者信息为绝密。', 1002, 2007, 4, '禁咒', '言灵,禁咒级,压制,绝密', '绝密档案室', 3560, 91, 0, 1, 0, '2026-10-19 08:46:20', '2026-10-19 08:46:20');
INSERT INTO `dr_archive` VALUES (17, 'AR-S-007', '言灵·青铜御座', '禁咒级言灵，与青铜与火之王权柄直接相关。', '言灵·青铜御座，禁咒级言灵。其权柄来源与青铜与火之王直接相关，持有者可生成高强度金属防御领域。因权柄来源敏感，该言灵的所有研究记录均存放于绝密档案室。', 1002, 2007, 3, '禁咒', '言灵,禁咒级,防御,青铜与火', '绝密档案室', 2040, 79, 0, 1, 0, '2026-10-19 08:46:20', '2026-10-19 08:46:20');
INSERT INTO `dr_archive` VALUES (18, 'AR-S-008', '言灵·审判', '禁咒级言灵，以龙文宣告的方式直接裁定目标命运。', '言灵·审判，禁咒级言灵。以龙文宣告的方式对目标直接施加「裁定」，其效果与代价均未被完整记录。学院仅在一次历史任务中有过使用记载，此后列为禁止主动使用的言灵。', 1002, 2007, 4, '禁咒', '言灵,禁咒级,禁止使用,绝密', '绝密档案室', 3120, 87, 1, 1, 0, '2026-10-19 08:46:20', '2026-10-19 08:46:20');
INSERT INTO `dr_archive` VALUES (19, 'AR-L-001', '尼伯龙根', '龙族栖息的里世界入口，空间结构极不稳定。', '尼伯龙根，龙族栖息的里世界入口统称。其内部空间结构极不稳定，常规物理规律部分失效。未经校长或执行部最高层书面授权，任何人不得进入。已知入口分布于全球七处，其中三处处于学院监控之下。', 1003, 2008, 3, '禁区', '里世界,禁区,龙王领域,授权进入', '龙类研究院', 4120, 93, 1, 1, 0, '2026-10-19 08:46:20', '2026-10-19 08:46:20');
INSERT INTO `dr_archive` VALUES (20, 'AR-L-002', '冰海', '位于北冰洋下的巨大空腔，与白王权柄高度相关。', '冰海，位于北冰洋冰盖下的巨大地下空腔。内部存在大量龙文遗迹，其权柄特征与白王高度吻合。学院曾在此执行过一次大型勘探任务，任务卷宗已封存。', 1003, 2008, 3, '禁区', '里世界,冰海,白王,勘探', '龙类研究院', 1890, 72, 0, 1, 0, '2026-10-19 08:46:20', '2026-10-19 08:46:20');
INSERT INTO `dr_archive` VALUES (21, 'AR-L-003', '地下图书馆', '学院地下七层的龙文典籍藏馆，A 级以上权限方可入内。', '地下图书馆，位于学院主楼地下共七层，收藏龙文典籍与各类绝密卷宗。B1-B3 层对 A 级及以上开放，B4 及以下需校长授权。馆内恒温恒湿，龙文残卷的破译工作在此进行。', 1003, 2009, 2, '管控', '学院设施,图书馆,权限分级', '学院教务厅', 2670, 81, 0, 1, 0, '2026-10-19 08:46:20', '2026-10-19 08:46:20');
INSERT INTO `dr_archive` VALUES (22, 'AR-L-004', '炼金工坊', '执行部装备研发与维护中心，混血种专属武器的唯一产地。', '炼金工坊，执行部装备研发与维护中心。全部混血种专属武器（如村雨系列）均在此锻造与维护。工坊采用龙文炼金术与传统冶金结合工艺，核心技术仅掌握在少数匠人手中。', 1003, 2009, 2, '管控', '学院设施,炼金,装备', '执行部后勤科', 1340, 58, 0, 1, 0, '2026-10-19 08:46:20', '2026-10-19 08:46:20');
INSERT INTO `dr_archive` VALUES (23, 'AR-O-001', '卡塞尔学院', '表面为芝加哥私立贵族学府，实为混血种培养与屠龙战略核心机构。', '卡塞尔学院，对外为美国芝加哥的一所私立贵族学府。其真实职能为混血种的培养、血统研究以及屠龙战略的制定与执行。学院由校董会直接管理，执行部为其行动臂膀。', 1004, 2010, 1, '公开', '学院,总部,屠龙,混血种', '学院教务厅', 5200, 96, 1, 1, 0, '2026-10-19 08:46:20', '2026-10-19 08:46:20');
INSERT INTO `dr_archive` VALUES (24, 'AR-O-002', '执行部', '学院的行动部门，负责高危任务与龙类威胁处置。', '执行部，卡塞尔学院的行动部门。负责所有高危任务的策划与执行，以及对龙类威胁的直接处置。成员均为血统评级 B 以上的在校生或毕业生，行动代号制度为其特色。', 1004, 2010, 2, '内部', '执行部,行动部门,高危任务', '执行部人事科', 3410, 89, 0, 1, 0, '2026-10-19 08:46:20', '2026-10-19 08:46:20');
INSERT INTO `dr_archive` VALUES (25, 'AR-O-003', '校董会', '学院最高决策机构，成员身份与决议均为绝密。', '校董会，卡塞尔学院的最高决策机构。其成员身份、议事过程与决议内容均为绝密信息。已知其掌握学院全部战略资源调度权，并可绕过执行部直接下达指令。', 1004, 2010, 4, '绝密', '校董会,绝密,决策机构', '绝密档案室', 2260, 83, 0, 1, 0, '2026-10-19 08:46:20', '2026-10-19 08:46:20');
INSERT INTO `dr_archive` VALUES (26, 'AR-O-004', '蛇岐八家', '日本混血种家族联盟，与学院保持有限合作。', '蛇岐八家，日本境内八个混血种家族组成的联盟。与卡塞尔学院保持有限合作关系，在东亚地区拥有独立的行动体系。其持有的龙文典籍数量仅次于学院地下图书馆。', 1004, 2011, 3, '机密', '外部势力,日本,家族联盟', '外事联络处', 1630, 66, 0, 1, 0, '2026-10-19 08:46:20', '2026-10-19 08:46:20');
INSERT INTO `dr_archive` VALUES (27, 'AR-O-005', '秘党', '独立于学院之外的混血种秘密结社，立场不明。', '秘党，独立于卡塞尔学院之外的混血种秘密结社。其组织结构、成员规模与最终目标均不明。已知其多次在学院行动前一步取得关键龙文残卷，立场判定为「不友好但非敌对」。', 1004, 2011, 4, '绝密', '外部势力,秘党,立场不明', '绝密档案室', 2080, 77, 0, 1, 0, '2026-10-19 08:46:20', '2026-10-19 08:46:20');
INSERT INTO `dr_archive` VALUES (28, 'AR-D-001', '龙文', '龙族语言文字体系，一切言灵的载体。', '龙文，龙族的语言与文字体系，是一切言灵的载体与触发媒介。其语法结构与人类语言完全不同，破译工作已持续逾百年，目前完成度不足四成。学院地下图书馆藏有残卷，阅读需 A 级以上权限。', 1005, 2012, 3, '核心', '文献,龙族语言,权限受限', '龙文研究院', 4490, 94, 1, 1, 0, '2026-10-19 08:46:20', '2026-10-19 08:46:20');
INSERT INTO `dr_archive` VALUES (29, 'AR-D-002', '冰海残卷', '于冰海勘探中取得的龙文残卷，记载白王权柄碎片线索。', '冰海残卷，学院在冰海勘探任务中取得的七枚龙文残卷统称。其内容指向白王权柄碎片的下落，破译工作由龙文研究院与绝密档案室联合进行，进度列为机密。', 1005, 2012, 4, '绝密', '文献,残卷,白王,绝密', '绝密档案室', 2710, 85, 0, 1, 0, '2026-10-19 08:46:20', '2026-10-19 08:46:20');
INSERT INTO `dr_archive` VALUES (30, 'AR-D-003', '青铜计划', '学院历史上规模最大的单次屠龙作战计划。', '青铜计划，卡塞尔学院历史上投入规模最大的单次屠龙作战计划，目标为青铜与火之王诺顿。计划细节与最终战果均已封存，仅校董会与执行部最高层可申请查阅。', 1005, 2013, 4, '绝密', '任务卷宗,青铜计划,封存', '绝密档案室', 3980, 90, 0, 1, 0, '2026-10-19 08:46:20', '2026-10-19 08:46:20');
INSERT INTO `dr_archive` VALUES (31, 'AR-D-004', '屠龙行动记录', '执行部历年高危行动的归档记录汇编。', '屠龙行动记录，执行部历年来所有高危行动的归档汇编。包含任务目标、参与人员、战损情况与后续处置。记录按密级分层存放，B 级以上专员可申请查阅本人参与部分。', 1005, 2013, 3, '机密', '任务卷宗,行动记录,机密', '执行部档案科', 1830, 70, 0, 1, 0, '2026-10-19 08:46:20', '2026-10-19 08:46:20');
INSERT INTO `dr_archive` VALUES (32, 'AR-R-001', '血统鉴定', '对新生混血种血统浓度进行评估分级的制度。', '血统鉴定，对新生及在校混血种的血统浓度进行评估分级的制度。评级分为 C / B / A / S 四档，决定培养方案、任务准入资格以及可接触的档案密级。鉴定由学院医务室与龙类研究院联合执行。', 1006, 2014, 1, '公开', '制度,新生,分级,血统', '学院教务厅', 3620, 86, 1, 1, 0, '2026-10-19 08:46:20', '2026-10-19 08:46:20');
INSERT INTO `dr_archive` VALUES (33, 'AR-R-002', '分级评估标准', '血统评级的量化标准与复核流程。', '分级评估标准，血统 C/B/A/S 四档评级的量化依据。包含龙文亲和度、血统觉醒速度、言灵强度与稳定性四个维度。评级结果每学年复核一次，S 级需由校长亲自复核。', 1006, 2014, 2, '内部', '制度,评级,量化标准', '龙类研究院', 1270, 56, 0, 1, 0, '2026-10-19 08:46:20', '2026-10-19 08:46:20');
INSERT INTO `dr_archive` VALUES (34, 'AR-R-003', '学院守则总则', '在校混血种必须遵守的基本行为准则。', '学院守则总则，所有在校混血种必须遵守的基本行为准则。核心条款包括：不得在非授权场合使用言灵、不得向外界透露学院真实职能、不得私自进入里世界。违反者视情节给予警告至封印处理。', 1006, 2015, 1, '公开', '制度,守则,行为准则', '学院教务厅', 2411, 74, 0, 1, 0, '2026-10-19 08:46:20', '2026-10-19 09:00:30');
INSERT INTO `dr_archive` VALUES (35, 'AR-R-004', '档案借阅权限条例', '规定不同身份可接触的档案密级上限。', '档案借阅权限条例，规定不同在校身份可接触的档案密级上限：访客与未认证者 1 级（公开），新生与在校生 2 级（内部），执行部成员 3 级（机密），教员与研究员 4 级（绝密）。越级查阅将触发审计告警。', 1006, 2015, 2, '内部', '制度,权限,密级,审计', '学院档案委员会', 1590, 64, 0, 1, 0, '2026-10-19 08:46:20', '2026-10-19 08:46:20');

-- ----------------------------
-- Table structure for dr_archive_category
-- ----------------------------
DROP TABLE IF EXISTS `dr_archive_category`;
CREATE TABLE `dr_archive_category`  (
  `id` bigint NOT NULL COMMENT '涓婚敭锛堝浐瀹氬?锛屼究浜庣?瀛愭暟鎹?紩鐢ㄧ埗瀛愬叧绯伙級',
  `parent_id` bigint NOT NULL DEFAULT 0 COMMENT '鐖剁骇鍒嗙被 ID锛? = 涓?骇鍒嗙被',
  `code` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '鍒嗙被 code锛屽叏灞?敮涓?紙濡?person / person.mixed锛',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '鍒嗙被鍚嶇О',
  `icon` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'archive' COMMENT '鍓嶇?鍥炬爣鍚嶏紙瀵瑰簲 AppIcon.vue锛',
  `sort_no` int NOT NULL DEFAULT 0 COMMENT '鎺掑簭鍙凤紝灏忕殑鍦ㄥ墠',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '1=鍚?敤 0=鍋滅敤',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_code`(`code` ASC) USING BTREE,
  INDEX `idx_parent`(`parent_id` ASC, `sort_no` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '妗ｆ?鍒嗙被琛?紙涓ょ骇锛歱arent_id=0 涓轰竴绾э級' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of dr_archive_category
-- ----------------------------
INSERT INTO `dr_archive_category` VALUES (1001, 0, 'person', '人物志', 'user', 1, 1, '2026-10-19 08:46:20', '2026-10-19 08:46:20');
INSERT INTO `dr_archive_category` VALUES (1002, 0, 'spirit', '言灵谱', 'sparkle', 2, 1, '2026-10-19 08:46:20', '2026-10-19 08:46:20');
INSERT INTO `dr_archive_category` VALUES (1003, 0, 'place', '地理志', 'flame', 3, 1, '2026-10-19 08:46:20', '2026-10-19 08:46:20');
INSERT INTO `dr_archive_category` VALUES (1004, 0, 'org', '机构志', 'shield', 4, 1, '2026-10-19 08:46:20', '2026-10-19 08:46:20');
INSERT INTO `dr_archive_category` VALUES (1005, 0, 'lore', '文献卷', 'archive', 5, 1, '2026-10-19 08:46:20', '2026-10-19 08:46:20');
INSERT INTO `dr_archive_category` VALUES (1006, 0, 'rule', '制度典', 'check', 6, 1, '2026-10-19 08:46:20', '2026-10-19 08:46:20');
INSERT INTO `dr_archive_category` VALUES (2001, 1001, 'person.mixed', '混血种名册', 'user', 1, 1, '2026-10-19 08:46:20', '2026-10-19 08:46:20');
INSERT INTO `dr_archive_category` VALUES (2002, 1001, 'person.dragonking', '龙王系谱', 'flame', 2, 1, '2026-10-19 08:46:20', '2026-10-19 08:46:20');
INSERT INTO `dr_archive_category` VALUES (2003, 1001, 'person.executor', '执行部专员', 'shield', 3, 1, '2026-10-19 08:46:20', '2026-10-19 08:46:20');
INSERT INTO `dr_archive_category` VALUES (2004, 1002, 'spirit.attack', '攻击型言灵', 'flame', 1, 1, '2026-10-19 08:46:20', '2026-10-19 08:46:20');
INSERT INTO `dr_archive_category` VALUES (2005, 1002, 'spirit.support', '辅助型言灵', 'sparkle', 2, 1, '2026-10-19 08:46:20', '2026-10-19 08:46:20');
INSERT INTO `dr_archive_category` VALUES (2006, 1002, 'spirit.eye', '瞳术系言灵', 'eye', 3, 1, '2026-10-19 08:46:20', '2026-10-19 08:46:20');
INSERT INTO `dr_archive_category` VALUES (2007, 1002, 'spirit.forbidden', '禁咒级言灵', 'alert', 4, 1, '2026-10-19 08:46:20', '2026-10-19 08:46:20');
INSERT INTO `dr_archive_category` VALUES (2008, 1003, 'place.inner', '里世界', 'flame', 1, 1, '2026-10-19 08:46:20', '2026-10-19 08:46:20');
INSERT INTO `dr_archive_category` VALUES (2009, 1003, 'place.facility', '学院设施', 'shield', 2, 1, '2026-10-19 08:46:20', '2026-10-19 08:46:20');
INSERT INTO `dr_archive_category` VALUES (2010, 1004, 'org.college', '学院机构', 'shield', 1, 1, '2026-10-19 08:46:20', '2026-10-19 08:46:20');
INSERT INTO `dr_archive_category` VALUES (2011, 1004, 'org.outside', '外部势力', 'alert', 2, 1, '2026-10-19 08:46:20', '2026-10-19 08:46:20');
INSERT INTO `dr_archive_category` VALUES (2012, 1005, 'lore.dragonlang', '龙文典籍', 'archive', 1, 1, '2026-10-19 08:46:20', '2026-10-19 08:46:20');
INSERT INTO `dr_archive_category` VALUES (2013, 1005, 'lore.mission', '任务卷宗', 'ticket', 2, 1, '2026-10-19 08:46:20', '2026-10-19 08:46:20');
INSERT INTO `dr_archive_category` VALUES (2014, 1006, 'rule.blood', '血统制度', 'sparkle', 1, 1, '2026-10-19 08:46:20', '2026-10-19 08:46:20');
INSERT INTO `dr_archive_category` VALUES (2015, 1006, 'rule.code', '学院守则', 'check', 2, 1, '2026-10-19 08:46:20', '2026-10-19 08:46:20');

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
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '热门检索词' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of dr_hot_keyword
-- ----------------------------
INSERT INTO `dr_hot_keyword` VALUES (1, '龙王', 980, 1, 1, '2026-10-19 08:46:20', '2026-10-19 08:46:20');
INSERT INTO `dr_hot_keyword` VALUES (2, '言灵', 940, 2, 1, '2026-10-19 08:46:20', '2026-10-19 08:46:20');
INSERT INTO `dr_hot_keyword` VALUES (3, '尼伯龙根', 860, 3, 1, '2026-10-19 08:46:20', '2026-10-19 08:46:20');
INSERT INTO `dr_hot_keyword` VALUES (4, '血统鉴定', 720, 4, 1, '2026-10-19 08:46:20', '2026-10-19 08:46:20');
INSERT INTO `dr_hot_keyword` VALUES (5, '青铜与火', 690, 5, 1, '2026-10-19 08:46:20', '2026-10-19 08:46:20');
INSERT INTO `dr_hot_keyword` VALUES (6, '村雨', 610, 6, 1, '2026-10-19 08:46:20', '2026-10-19 08:46:20');
INSERT INTO `dr_hot_keyword` VALUES (7, '龙文', 580, 7, 1, '2026-10-19 08:46:20', '2026-10-19 08:46:20');
INSERT INTO `dr_hot_keyword` VALUES (8, '白王', 520, 8, 1, '2026-10-19 08:46:20', '2026-10-19 08:46:20');

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
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '首页公告 / 学院通报' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of dr_notice
-- ----------------------------
INSERT INTO `dr_notice` VALUES (1, 'NT-2026-001', '龙王级血统波动预警：本区里世界入口活跃度异常上升', '监测网显示本区里世界入口活跃度较上周上升 47%。执行部已进入二级战备，所有专员取消休假。非授权人员严禁接近 B3 层及以下区域。', 3, 1, '执行部指挥中心', '2026-10-15 09:00:00', '2026-10-22 23:59:59', 1, 0, '2026-10-19 08:46:20', '2026-10-19 08:46:20');
INSERT INTO `dr_notice` VALUES (2, 'NT-2026-002', '地下图书馆 B3 层封闭检修，借阅服务暂停一周', '因龙文恒温系统例行检修，地下图书馆 B3 层于 10 月 16 日至 10 月 22 日封闭，期间暂停一切借阅与查阅服务。B1-B2 层正常开放。', 2, 1, '学院教务厅', '2026-10-14 15:30:00', '2026-10-23 00:00:00', 1, 0, '2026-10-19 08:46:20', '2026-10-19 08:46:20');
INSERT INTO `dr_notice` VALUES (3, 'NT-2026-003', '2026 级新生血统鉴定安排通知', '2026 级新生血统鉴定将于本月 20 日起分批进行，具体批次与时间见各班级通知。鉴定前 24 小时内请勿使用任何言灵。', 1, 0, '学院医务室', '2026-10-13 10:00:00', '2026-10-25 23:59:59', 1, 0, '2026-10-19 08:46:20', '2026-10-19 08:46:20');

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
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '密钥重置验证码表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of dr_reset_code
-- ----------------------------
INSERT INTO `dr_reset_code` VALUES (1, 'REG4d874874c839153c', '173538', '2026-10-19 08:59:05', 1, '2026-10-19 08:54:05');

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
) ENGINE = InnoDB AUTO_INCREMENT = 14 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '检索日志' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of dr_search_log
-- ----------------------------
INSERT INTO `dr_search_log` VALUES (1, 4, '', NULL, NULL, NULL, 18, '127.0.0.1', '2026-10-19 08:54:53');
INSERT INTO `dr_search_log` VALUES (2, 4, '', NULL, NULL, NULL, 18, '127.0.0.1', '2026-10-19 08:55:12');
INSERT INTO `dr_search_log` VALUES (3, 4, '', NULL, NULL, NULL, 18, '127.0.0.1', '2026-10-19 08:55:19');
INSERT INTO `dr_search_log` VALUES (4, 4, '', NULL, NULL, NULL, 18, '127.0.0.1', '2026-10-19 08:55:44');
INSERT INTO `dr_search_log` VALUES (5, 4, '', NULL, NULL, NULL, 18, '127.0.0.1', '2026-10-19 08:55:55');
INSERT INTO `dr_search_log` VALUES (6, 4, '', NULL, NULL, NULL, 18, '127.0.0.1', '2026-10-19 08:56:58');
INSERT INTO `dr_search_log` VALUES (7, 4, '', NULL, NULL, NULL, 18, '127.0.0.1', '2026-10-19 08:57:36');
INSERT INTO `dr_search_log` VALUES (8, 4, '', NULL, NULL, NULL, 18, '127.0.0.1', '2026-10-19 09:00:59');
INSERT INTO `dr_search_log` VALUES (9, 4, '', NULL, NULL, NULL, 18, '127.0.0.1', '2026-10-19 09:01:11');
INSERT INTO `dr_search_log` VALUES (10, 4, '', NULL, NULL, NULL, 18, '127.0.0.1', '2026-10-19 09:01:37');
INSERT INTO `dr_search_log` VALUES (11, 4, '', NULL, NULL, NULL, 18, '127.0.0.1', '2026-10-19 09:01:47');
INSERT INTO `dr_search_log` VALUES (12, 4, '', NULL, NULL, NULL, 18, '127.0.0.1', '2026-10-19 09:01:51');
INSERT INTO `dr_search_log` VALUES (13, 4, '', NULL, NULL, NULL, 18, '127.0.0.1', '2026-10-19 09:02:08');

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
  INDEX `idx_codename`(`codename` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '混血种（用户）表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of dr_user
-- ----------------------------
INSERT INTO `dr_user` VALUES (1, 'CC20260001', '路明非', '$2b$10$zo3PuzhEXRCmi/CgCZzvnOhCTVo8X9Bb7SDjnSfJkO6VZOuksD5W2', 'freshman', 'junyan', 1, 0, 2, '2026-10-19 09:12:44', '2026-10-19 08:46:20', '2026-10-19 09:12:43');
INSERT INTO `dr_user` VALUES (2, 'CC20260002', '楚子航', '$2b$10$EhhQtz0J7vukNhH3zCoBiew.9/FOuG9I/pE3uKWRR.7JcZxq150ou', 'executor', 'shiling', 1, 0, 0, NULL, '2026-10-19 08:46:20', '2026-10-19 08:46:20');
INSERT INTO `dr_user` VALUES (3, 'CC20260003', '芬格尔', '$2b$10$aamVUE83rOCVisNGuH2TbOJHUrEfbXNcNzvwSOsmnASalROlxKlAC', 'student', 'fengwang', 1, 0, 0, NULL, '2026-10-19 08:46:20', '2026-10-19 08:46:20');
INSERT INTO `dr_user` VALUES (4, 'CC202600004', 'guao', '$2a$10$QKtoUklOgL6ZoxGybXNnGePdnUWt3WR.C8.TshBdhfQ6zndqq9JPm', 'freshman', 'none', 1, 0, 1, '2026-10-19 08:54:51', '2026-10-19 08:54:39', '2026-10-19 08:54:51');

SET FOREIGN_KEY_CHECKS = 1;
