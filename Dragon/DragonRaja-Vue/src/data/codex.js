import { CHARACTER_GALLERY } from '@/data/characters'

/**
 * 言灵谱 / 机构志 数据 —— 首页图鉴卡片与独立详情页共用同一份数据。
 * 插画：public/img/spirit/<key>.webp、public/img/org/<key>.webp
 */

export const SPIRIT_GALLERY = [
  {
    key: 'junyan',
    name: '君焰',
    element: '火',
    level: '高危',
    effect: '领域内可燃物瞬间爆燃',
    desc: '以血统呼唤高阶火焰，在领域内形成绝对压制。稳定释放时可将空气加热至燃点，是正面攻坚中最具破坏力的言灵之一。相传释放者眼中会燃起金色竖瞳。',
    holder: '攻坚型混血种 · 多见于战斗编制',
    note: '持续释放会灼伤自身血脉，超过三分钟即有失控风险。'
  },
  {
    key: 'shiling',
    name: '时零',
    element: '时间',
    level: '机密',
    effect: '万物迟滞，唯我不动',
    desc: '让领域内的时间流速放缓，使用者几乎获得一段「静止时间」的观察与行动窗口。对精神与体力消耗极大，是执行部部长的招牌言灵。',
    holder: '执行部高级专员 · 需 S 级以上血统',
    note: '每次释放都在燃烧寿命，频繁使用者普遍早衰。'
  },
  {
    key: 'fengwang',
    name: '风王之瞳',
    element: '风',
    level: '高危',
    effect: '风刃无形，触之即断',
    desc: '凝聚风之王的竖瞳，领域内生成无色无形的风刃，可洞穿一切实体。因无法被肉眼捕捉，是最难防御的突袭类言灵。',
    holder: '突袭型混血种 · 擅长单兵作战',
    note: '风刃不分敌我，混战中极易误伤同伴。'
  },
  {
    key: 'bronze',
    name: '青铜御座',
    element: '重力',
    level: '机密',
    effect: '重力由我定义',
    desc: '以自身为中心重构重力场，可抬升万钧之物，也可让敌人当场跪伏。传说中曾用于托起整艘倾覆的巨轮。',
    holder: '重装型混血种 · 常任现场指挥',
    note: '重力反转的瞬间使用者自身骨骼同样承受数倍负荷。'
  },
  {
    key: 'zhulong',
    name: '烛龙',
    element: '冰',
    level: '绝密',
    effect: '千里冰封',
    desc: '极寒吐息所过之处一切冻结。传说中烛龙睁眼为昼、闭目为夜，此言灵一旦失控将引发区域性极寒灾害，故编列为绝密级。',
    holder: '禁忌级持有者 · 全学院记录在册者不足三人',
    note: '释放后体温骤降，须在医疗部监护下复苏。'
  },
  {
    key: 'wangquan',
    name: '王权',
    element: '威压',
    level: '绝密',
    effect: '血统即律法',
    desc: '血统的绝对压制。领域内一切低阶混血种会因本能的臣服而跪倒，无法直视使用者。它不属于任何元素系，是「王」的证明。',
    holder: '龙王血裔 · 非传承不可习得',
    note: '对同阶及以上血统无效，一旦失效将遭到反噬。'
  },
  {
    key: 'lianyou',
    name: '镰鼬',
    element: '风',
    level: '高危',
    effect: '三道真空风刃，肉眼不可见',
    desc: '以极细的真空刃切开空气，形成三道交错的弧形轨迹。被斩者往往在察觉疼痛之前就已负伤，是潜行与伏击中最难预警的言灵。',
    holder: '侦察 / 追踪专员',
    note: '觉醒后听觉被永久强化，畏强噪音，须长期佩戴护具。'
  },
  {
    key: 'chiri',
    name: '炽日',
    element: '火',
    level: '绝密',
    effect: '凝出微型恒星，领域内一切汽化',
    desc: '把释放者的血统燃烧到极致，在掌心凝出一颗白炽的微型恒星。其光芒可穿透一切遮蔽，其热度足以让钢铁在瞬间蒸发。',
    holder: '屠龙级战力 · 需学院最高会议授权方可记录',
    note: '释放后使用者会陷入长达数日的血统衰竭，期间形同废人。'
  },
  {
    key: 'jingmi',
    name: '静谧',
    element: '精神',
    level: '机密',
    effect: '领域内一切声音湮灭',
    desc: '以精神力抹平空气的振动，让领域内的声音彻底消失。可用于潜行，也可让敌人在无声中丧失方位感与协同能力。',
    holder: '潜入 / 暗杀专员',
    note: '长期使用会侵蚀使用者的听觉与语言能力。'
  },
  {
    key: 'mingzhao',
    name: '冥照',
    element: '暗影',
    level: '绝密',
    effect: '绝对黑暗领域，剥夺一切视觉',
    desc: '吞噬领域内的全部光线，只留下释放者视野中的轮廓。在黑暗中，唯有血脉相近者能彼此辨认，其余人皆沦为盲者。',
    holder: '情报 / 肃清专员',
    note: '黑暗中视物的代价，是使用者会逐渐失去对色彩的感知。'
  },
  {
    key: 'binghai',
    name: '冰海',
    element: '冰',
    level: '机密',
    effect: '瞳孔所及，皆成冰原',
    desc: '视线扫过之处海水凝滞、空气结晶，可将一整片海湾冻结成白原。与烛龙同源而威能远逊，胜在消耗可控，是极地任务中的战略级支援言灵。',
    holder: '极地行动编制 · 冷区特训合格者',
    note: '冻结范围内使用者自身同样畏寒，须装备恒温护甲执行。'
  },
  {
    key: 'xinwang',
    name: '心网',
    element: '精神',
    level: '内部',
    effect: '听见方圆百米的心跳',
    desc: '以精神力织成一张无形之网，网内一切生物的心跳、情绪与杀意都会在释放者脑中成形。无法读取具体思维，但谎言在它面前毫无意义。',
    holder: '审讯 / 监察编制 · 风纪委员会外聘',
    note: '人群密集处长时间开启会引发精神过载，须限时使用。'
  },
  {
    key: 'guzhong',
    name: '骨冢',
    element: '暗影',
    level: '绝密',
    effect: '白骨为冢，亡者为兵',
    desc: '让领域内残存的骨骼应声而起，听凭释放者驱使。传说最高纪录曾唤起一整支沉没战船的骸骨军团，故被列为本代封存级言灵。',
    holder: '禁忌级持有者 · 记录在案者仅一人',
    note: '被驱使的亡骨会缓慢反噬操纵者的血肉，每次使用都在折损寿数。'
  },
  {
    key: 'yulei',
    name: '御雷',
    element: '雷',
    level: '高危',
    effect: '雷霆为我执剑',
    desc: '在高空电离层与释放者之间架起「引线」，将落雷引为己用。导引精度高时可将雷霆约束成一道笔直的惩罚之剑，是压制成建制冷兵器的言灵。',
    holder: '攻坚型混血种 · 需雷暴环境或天气支援',
    note: '雷雨天气威力翻倍，无云晴天几乎不可释放。'
  },
  {
    key: 'heiri',
    name: '黑日',
    element: '火',
    level: '高危',
    effect: '黑色日轮领域，引力吞噬一切',
    desc: '施放后形成旋转的黑色日轮领域，如日全食中的太阳，刀锋划出的圆弧赤红发亮。领域内产生强引力场，吞噬空气掀起飓风，将周围物体、海水乃至尸守一并拉向中心，尸守须抠紧地面才不会被吸走。',
    holder: '上杉越 · 蛇岐八家前任大家主，白王血统，实力接近龙王',
    note: '若施放者身死，黑日会瞬间收缩爆炸，吞噬周围一切，威力极大。'
  }
]

export const ORG_GALLERY = [
  {
    key: 'executive',
    name: '执行部',
    code: 'EXE-01',
    level: '机密',
    duty: '清理失控混血种，回收高危卷宗',
    desc: '学院最强、也是唯一常备的战斗编制。成员从在校生中秘密选拔，直接受校董会与校长节制，可在未经地方许可的情况下执行跨境任务。',
    tags: ['作战', '外勤', '肃清'],
    head: '直属：执行部部长',
    seat: '驻地：学院地下三层 · 作战指挥厅'
  },
  {
    key: 'alchemy',
    name: '炼金术与科学部',
    code: 'ALC-02',
    level: '机密',
    duty: '言灵器械化、药剂与装备研发',
    desc: '把不可控的血统力量封装进可复制的金属与药液之中。学院现役的炼金刀剑、抗龙血药剂与定位符文，绝大多数出自这里。',
    tags: ['研发', '炼金', '装备'],
    head: '直属：首席炼金术士',
    seat: '驻地：学院西区 · 铸造工坊'
  },
  {
    key: 'discipline',
    name: '风纪委员会',
    code: 'DIS-03',
    level: '机密',
    duty: '学员纪律审判与院规执行',
    desc: '学院内部的最高裁判机构。凡涉及违反《卡塞尔学院守则》的案件，无论身份，均由风纪委员会闭门审理，其裁定不可上诉。',
    tags: ['审判', '纪律', '监察'],
    head: '直属：风纪委员长',
    seat: '驻地：钟楼 · 审判厅'
  },
  {
    key: 'archive',
    name: '中央档案馆',
    code: 'ARC-04',
    level: '绝密',
    duty: '卷宗编修、密级审定与借阅登记',
    desc: '收藏了自建校以来全部混血种与龙族的记录。馆藏按密级分级存放，每一份卷宗的每一次调阅都会留下不可磨灭的痕迹。',
    tags: ['编修', '归档', '密级'],
    head: '直属：档案馆馆长',
    seat: '驻地：学院地下七层 · 环形书库'
  },
  {
    key: 'medical',
    name: '医疗部',
    code: 'MED-05',
    level: '机密',
    duty: '血统伤病救治与言灵后遗症监护',
    desc: '专治普通医学无法解释的伤——被龙血灼伤的皮肤、被言灵透支的神经、以及在暴血后濒临失控的混血种。',
    tags: ['救治', '监护', '血统'],
    head: '直属：医疗部主任',
    seat: '驻地：学院东翼 · 净化病区'
  },
  {
    key: 'board',
    name: '校董会',
    code: 'BRD-00',
    level: '绝密',
    duty: '学院最高决策与预算审批',
    desc: '由十二个古老混血种家族各占一席。他们不参与日常教学，却决定学院的存续、战争的开启，以及某些档案的永久封存。',
    tags: ['决策', '家族', '封存'],
    head: '席位：十二席 · 轮值主席',
    seat: '驻地：钟楼顶层 · 圆桌会议厅'
  }
]

/* ------------------------------------------------------------------ *
 * 图鉴索引：供首页「精选 / 热门 / 最新」卷宗卡片判断能否跳到对应详情页
 * ------------------------------------------------------------------ */

/**
 * 卷宗标题与图鉴条目的别名对照（卷宗用常用称呼，图鉴用全名）。
 * 只在名称直接比对失败时兜底，避免「诺诺」「凯撒」这类称呼跳不到对应人物。
 */
const SYNONYMS = {
  诺诺: ['person', 'nuonuo'],
  '凯撒·加图索': ['person', 'kaisa'],
  '恺撒·加图索': ['person', 'kaisa']
}

/** 三类图鉴的详情路由前缀 */
export const CODEX_PATH = {
  person: '/codex',
  spirit: '/spirit',
  org: '/org'
}

/** key → 详情路径 */
export function codexPath(kind, key) {
  return `${CODEX_PATH[kind] || '/codex'}/${key}`
}

function findCodex(kind, key) {
  if (kind === 'person') return CHARACTER_GALLERY.find((i) => i.key === key) || null
  if (kind === 'spirit') return SPIRIT_GALLERY.find((i) => i.key === key) || null
  if (kind === 'org') return ORG_GALLERY.find((i) => i.key === key) || null
  return null
}

/** 卷宗标题里的常见前缀（「言灵·君焰」→「君焰」） */
function normalizeTitle(title) {
  return String(title || '')
    .replace(/^言灵[·・:：\s]*/, '')
    .replace(/^机构[·・:：\s]*/, '')
    .trim()
}

/**
 * 把一条卷宗（dr_archive）映射到图鉴详情页。
 * 命中规则：先看一级分类决定查哪本图鉴，再用标题与图鉴条目名比对
 * （支持「卷宗名包含条目名」或「条目名包含卷宗名」，避免个别字差异漏配）。
 * 命中返回 { kind, key, name, path }，否则 null —— 未命中时仍走原来的卷宗详情弹窗。
 */
export function matchCodexByArchive(item) {
  if (!item) return null
  const top = String(item.topCategoryName || '')
  const title = normalizeTitle(item.title)
  if (!title) return null

  const pools = []
  if (top === '人物志') pools.push(['person', CHARACTER_GALLERY])
  else if (top === '言灵谱') pools.push(['spirit', SPIRIT_GALLERY])
  else if (top === '机构志') pools.push(['org', ORG_GALLERY])
  else pools.push(['person', CHARACTER_GALLERY], ['spirit', SPIRIT_GALLERY], ['org', ORG_GALLERY])

  for (const [kind, list] of pools) {
    const hit =
      list.find((i) => i.name === title) ||
      list.find((i) => title.includes(i.name) || i.name.includes(title))
    if (hit) return { kind, key: hit.key, name: hit.name, path: codexPath(kind, hit.key) }
  }

  // 兜底：别名 / 异体字对照
  const syn = SYNONYMS[title]
  if (syn) {
    const [kind, key] = syn
    const item = findCodex(kind, key)
    if (item) return { kind, key, name: item.name, path: codexPath(kind, key) }
  }
  return null
}
