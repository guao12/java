/**
 * 人物志 · 角色档案数据（首页人物志 + 档案详情页共用）
 *
 * category: student 学生 / exec 执行部 / seal 封存档案
 * blood:     血统评级 S / A / B …… 必须与 bloodPct 落在同一区间，
 *            口径与血统鉴定页一致（见 utils/bloodline.js）：
 *            S 龙血 >50 / A 30~50 / B 15~30 / C 5~15 / D 1~5 / E <1
 * bloodPct:  龙血占比（%）
 * tone:      头像框主色调 gold / ice / fire / rose
 *
 * 头像当前以「首字 + 纹章框」呈现；接入真实立绘后，
 * 将卡片与详情页的 avatar 区块替换为 <img :src="`/img/character/${key}.webp`"> 即可。
 */
import { gradeOf } from '@/utils/bloodline'

export const CHARACTER_CATEGORIES = [
  { id: 'student', label: '学生' },
  { id: 'exec', label: '执行部' },
  { id: 'seal', label: '封存档案' }
]

const RAW_CHARACTERS = [
  {
    key: 'luomingfei',
    name: '路明非',
    alias: '衰仔 · 青铜与火的候补',
    category: 'student',
    categoryLabel: '学生',
    role: '卡塞尔学院三年级 · 学生会「永久」挂名干事',
    level: '绝密',
    blood: 'S',
    bloodPct: 87.5,
    bloodNote: '对外档案标 C 级；三次复检样本均被上层直接调走，实测评级封存。',
    spirits: ['君焰', '王权（未激活）', '梦境回廊（观测中）'],
    tone: 'gold',
    desc: '入学考核 S 级评价的传奇衰仔。白天在学生会打杂，夜里被卷进一次又一次「顺便」的任务。',
    deeds: [
      '入学考核拿下 S 级评价，同一天被学生会挂上「永久干事」的名头。',
      '东京任务中独自留下殿后，用四分之一条命换回整支队伍。',
      '编号「〇」的卷宗调阅记录里，他的名字出现了不止一次。'
    ],
    quote: '「衰仔也有想守护的东西。」'
  },
  {
    key: 'chuzihang',
    name: '楚子航',
    alias: '村雨之主 · 独行者',
    category: 'exec',
    categoryLabel: '执行部',
    role: '执行部专员 · 兼修四年级课程',
    level: '机密',
    blood: 'A',
    bloodPct: 41.7,
    bloodNote: 'A 级 · 暴血倾向显著，历次高强度任务后须接受医疗部强制监护。',
    spirits: ['镰鼬', '村雨（炼金刀 · 人刀合一）'],
    tone: 'ice',
    desc: '沉默的独行刀客。刀术考核连续四年全院第一，至今无人接近纪录。',
    deeds: [
      '以一人之力清剿地铁隧道内的失控混血种巢穴，平民零误伤。',
      '刀术考核连续四年全院第一，纪录保持者本人拒绝领奖。',
      '为追查「三峡号」沉船真相，把卷宗堆满了半间宿舍。'
    ],
    quote: '「路，是一个人走的。」'
  },
  {
    key: 'kaisa',
    name: '恺撒·加图索',
    alias: '狮心会主席 · 加图索家嗣',
    category: 'student',
    categoryLabel: '学生',
    role: '四年级 · 狮心会主席',
    level: '机密',
    blood: 'A',
    bloodPct: 45.8,
    bloodNote: 'A 级 · 显赫血系直系，「王权」压制域宽度为同级之最。',
    spirits: ['王权（残响）', '狮心斩'],
    tone: 'gold',
    desc: '天生的指挥官与领袖。骄傲得理直气壮，也可靠得毋庸置疑。',
    deeds: [
      '率狮心会全员通过「冰海围猎」实战演习，评价全优。',
      '在学院公开赛上以一敌三，为狮心会夺回失落七年的锦标。',
      '家族年会上公开顶撞十二席之一，随后转身回校上课。'
    ],
    quote: '「加图索家的人，从不背对战场。」'
  },
  {
    key: 'nuonuo',
    name: '陈墨瞳',
    alias: '诺诺 · 火之妮可',
    category: 'student',
    categoryLabel: '学生',
    role: '四年级 · 学生会主席',
    level: '机密',
    blood: 'A',
    bloodPct: 37.5,
    bloodNote: 'A 级 · 血脉亲和火焰，情绪波动会引燃周边可燃物。',
    spirits: ['炽阳（短促释放）', '血怒'],
    tone: 'fire',
    desc: '风风火火的学生会主席，敢在全院大会上点名校董会的不是。',
    deeds: [
      '主持学生会改选，把「食堂宵夜供应」写进了年度预算。',
      '训练场事故中零点三秒内拉出三名新人，自己烧了半边袖子。',
      '全院大会上公开质疑预算案，第二天被请去钟楼「喝茶」。'
    ],
    quote: '「怕什么，烧起来就赢了。」'
  },
  {
    key: 'xiami',
    name: '夏弥',
    alias: '白王之嗣 · 平静的暴君',
    category: 'seal',
    categoryLabel: '封存档案',
    role: '身份封存 · 直属校长监管',
    level: '绝密',
    blood: 'S',
    bloodPct: 91.7,
    bloodNote: 'S 级 · 龙王血裔。鉴定当日仪器过载烧毁，数据以估算补录。',
    spirits: ['冰锥王座', '王权'],
    tone: 'ice',
    desc: '档案袋比人厚的转学生。不生气的时候，才是最危险的时候。',
    deeds: [
      '入学鉴定烧毁两台评级仪，第三台由校长亲自到场才敢开机。',
      '「冰锥王座」首次失控，冻结了半个训练场，无人敢上报细节。',
      '档案调阅记录显示：半年内被同一账号查阅十一次。'
    ],
    quote: '「我不生气的时候，才是最危险的。」'
  },
  {
    key: 'fingel',
    name: '芬格尔·冯·弗林斯',
    alias: '万年留级生 · 情报贩子',
    category: 'student',
    categoryLabel: '学生',
    role: '八年级（留级）· 学院内网幽灵',
    level: '内部',
    blood: 'B',
    bloodPct: 25,
    bloodNote: 'B 级 · 血统平平，但情报网覆盖学院每一个角落。',
    spirits: ['拟态', '静谧（入门）'],
    tone: 'gold',
    desc: '留级理由成谜的传奇学长。想要什么情报，先请他吃一顿芝士汉堡。',
    deeds: [
      '连续七年当选「最想请他吃饭的学长」，得票率逐年上升。',
      '在内网架设「芬格尔快讯」，订阅者包括三名不知名的教授。',
      '声称知道每一扇通风管道的走向，没人敢验证真假。'
    ],
    quote: '「兄弟，情报有价，兄弟价也贵。」'
  },
  {
    key: 'huilixi',
    name: '上杉绘梨衣',
    alias: 'Sakura · 龙王之女',
    category: 'seal',
    categoryLabel: '封存档案',
    role: '特殊监护对象 · 医疗部长期病区',
    level: '绝密',
    blood: 'S',
    bloodPct: 95.8,
    bloodNote: 'S 级 · 龙王血裔。血样接触空气即结晶，评级由档案科代签。',
    spirits: ['常暗', '时间回廊（疑似）'],
    tone: 'rose',
    desc: '病区窗边数乌鸦的女孩。她的病历本最后一页，写着一个不是病的名字。',
    deeds: [
      '病区逃脱事件累计十七次，每次都在天台被找到，无一例外。',
      '「常暗」无意识展开时，整层病区的监护仪同时黑屏。',
      '把折好的纸星星塞满了储物柜，柜门上贴着「不许偷看」。'
    ],
    quote: '「SAKURA 想和路明非，一直一直在一起。」'
  },
  {
    key: 'jiudemayi',
    name: '酒德麻衣',
    alias: '夜行鬼 · 执行部之刃',
    category: 'exec',
    categoryLabel: '执行部',
    role: '执行部高级专员 · 东京驻外',
    level: '机密',
    blood: 'B',
    bloodPct: 29.2,
    bloodNote: 'B 级 · 血统并非顶尖，任务完成率却是执行部现役第一。',
    spirits: ['冥照', '镰鼬（改式）'],
    tone: 'ice',
    desc: '夜色里来去的驻外专员。话少，刀快，报告永远准时提交。',
    deeds: [
      '东京新宿区连续三年「零失控事件」，与她驻外的时间完全重合。',
      '单独回收高危卷宗九份，回收过程从无目击者。',
      '年度考核评语只有一句：「希望她按时吃饭。」'
    ],
    quote: '「工作的时候，请叫我专员。」'
  }
]

/**
 * 对外导出前统一按龙血占比推导评级（utils/bloodline.js），
 * 保证人物志 / 档案详情展示的血统等级与血统鉴定页的检测口径完全一致。
 */
export const CHARACTER_GALLERY = RAW_CHARACTERS.map((c) => ({
  ...c,
  blood: gradeOf(c.bloodPct).grade
}))
