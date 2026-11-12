/**
 * 落地层静态数据（序章文案 / 阵营 / 门户公告）。
 *
 * 为什么放静态文件而不是接口：
 * 序章文案与阵营描述是「一次性世界观设定」，几乎不会运营变更；
 * 为它单独建表 + 写接口，收益远小于维护成本。
 * 门户公告后续若要动态化，再替换成 /api/portal/overview 即可，
 * 组件只依赖这里的数据形状，替换时不用改模板。
 */

const BASE = import.meta.env.BASE_URL

/** 落地层图片路径（public/img/landing/ 下按宽度分档） */
export function imgSrc(name, width) {
  return `${BASE}img/landing/${name}-${width}.webp`
}

/** 生成 srcset，浏览器按视口与 DPR 自行挑选，避免一律下最大图 */
export function imgSrcset(name, widths) {
  return widths.map((w) => `${imgSrc(name, w)} ${w}w`).join(', ')
}

/** 序章五段（原文照搬 longzu 原型，仅调整标点一致性） */
export const PROLOGUE_LINES = [
  '在人类文明出现之前，龙族已经建立高度发达的文明。黑王尼德霍格位于整个龙族血统和权力结构的顶点，既是创造者，也是绝对皇帝。龙族以血统维持秩序，低阶龙类必须服从高阶龙类；黑王对所有龙类拥有近乎绝对的统治权。',
  '当青铜与火之王的权柄触及大地，龙族的辉煌时代降临。它们建造了超越想象的城市，掌握了元素与炼金的极致之力。',
  '然而辉煌之下暗流涌动。白王的背叛点燃了龙族内战的烈焰，曾经辉煌的文明在战火中崩塌，龙族的时代走向终结。',
  '在废墟之中，人类文明悄然崛起。但龙族从未真正消亡——它们以另一种方式潜伏在历史的暗处，等待着再次觉醒的时刻。',
  '卡塞尔学院，便是人类对抗龙族的最前线。这里培养着拥有「言灵」之力的精英战士，他们是守夜人，是黑暗中的最后防线。'
]

/**
 * 双阵营。`tone` 决定背景遮罩的冷暖倾向：
 * cold = 卡塞尔哥特城堡（冷蓝）；warm = 蛇岐八家高天原（绯红）。
 */
export const FACTIONS = [
  {
    key: 'cassell',
    name: '卡塞尔学院',
    tone: 'cold',
    eyebrow: 'NIGHT WATCHERS COMMUNITY',
    title: ['CASSELL', 'COLLEGE'],
    subtitle: 'NIGHT WATCHERS COMMUNITY',
    desc: '芝加哥郊外的山间学院，人类对抗龙族的最后防线。',
    bg: 'bg-cassell',
    bulletin: {
      tag: 'Organization Bulletin',
      title: '组织部活动公示',
      text: '组织部正在摸鱼'
    }
  },
  {
    key: 'takamagahara',
    name: '蛇岐八家',
    tone: 'warm',
    eyebrow: 'THE EIGHT CLANS',
    title: ['TAKAMAGAHARA', 'SHRINE'],
    subtitle: 'THE EIGHT CLANS OF JAPAN',
    desc: '日本分部的八大家族，血脉与言灵的另一种秩序。',
    bg: 'bg-takamagahara',
    bulletin: {
      tag: 'Family Notice',
      title: '家老会通告',
      text: '高天原之门暂闭，诸家静候钟声'
    }
  }
]

/** 座右铭：落地页与门户页共用一处，改文案只改这里 */
export const MOTTO = '凡王之血 · 必以剑终'
