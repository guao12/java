// 形态主题与纹章数据
// 纹章已从内联 SVG 改为 AI 生成的形态立绘（public/crest/*.webp，已预裁为正方形并对准头盔）

export const FORMS = {
  rabbittank: {
    name: '兔子坦克',
    finish: 'VORTEX FINISH',
    theme: {
      red: '#ff3b3b', blue: '#00a6e6', accent: '#f5c518',
      left: '#ff3b3b', right: '#00a6e6',
      gl: '#ff3b3b', gr: '#00a6e6',
      grid: 'rgba(255,120,120,0.06)',
      spot: 'radial-gradient(circle, rgba(255,59,59,0.14), rgba(0,166,230,0.11) 42%, transparent 66%)',
      ray1: '#ff3b3b', ray2: '#00a6e6',
      glowR: 'rgba(255,59,59,0.5)', glowB: 'rgba(0,166,230,0.5)',
      btn: 'linear-gradient(100deg,#ff3b3b 0%,#ff6a3d 50%,#00a6e6 100%)',
      flash: 'radial-gradient(circle at 50% 45%, rgba(255,59,59,.9), rgba(0,166,230,.5) 55%, transparent 75%)'
    }
  },
  hazard: {
    name: '危险形态',
    finish: 'HAZARD FINISH',
    theme: {
      red: '#ff2a2a', blue: '#3a0a0a', accent: '#ff3b3b',
      left: '#ff2a2a', right: '#1a1a1a',
      gl: '#ff1f1f', gr: '#ff1f1f',
      grid: 'rgba(255,70,70,0.08)',
      spot: 'radial-gradient(circle, rgba(255,30,30,0.18), rgba(120,0,0,0.12) 42%, transparent 66%)',
      ray1: '#ff2a2a', ray2: '#ff6a6a',
      glowR: 'rgba(255,30,30,0.6)', glowB: 'rgba(120,0,0,0.3)',
      btn: 'linear-gradient(100deg,#2a0808 0%,#ff2a2a 55%,#7a0000 100%)',
      flash: 'radial-gradient(circle at 50% 45%, rgba(255,30,30,.95), transparent 72%)'
    }
  },
  // 天才形态：彩色光效（背景聚光/光晕/点击波/纹章/点击光均为彩色）
  genius: {
    name: '天才形态',
    finish: 'GENIUS FINISH',
    colorful: true,
    theme: {
      red: '#f5c518', blue: '#ffffff', accent: '#f5c518',
      left: '#f5c518', right: '#ffffff',
      gl: '#f5c518', gr: '#f5c518',
      grid: 'rgba(245,197,24,0.06)',
      spot: 'conic-gradient(from 0deg, #ff3b3b, #f5c518, #00a6e6, #7ad7ff, #9b5de5, #ff5fa2, #ff3b3b)',
      ray1: '#ff3b3b', ray2: '#f5c518',
      glowR: 'radial-gradient(circle, rgba(255,59,59,.55), rgba(245,197,24,.4) 35%, rgba(0,166,230,.35) 65%, transparent 72%)',
      glowB: 'radial-gradient(circle, rgba(122,215,255,.5), rgba(155,93,229,.4) 40%, rgba(255,59,59,.3) 70%, transparent 75%)',
      btn: 'linear-gradient(100deg,#ff3b3b 0%,#f5c518 30%,#00a6e6 62%,#7ad7ff 100%)',
      flash: 'radial-gradient(circle at 50% 45%, rgba(255,59,59,.8), rgba(245,197,24,.6) 40%, rgba(0,166,230,.5) 70%, transparent 78%)'
    }
  }
}

export const GENIUS_RAINBOW = ['#ff3b3b', '#f5c518', '#00a6e6', '#7ad7ff', '#9b5de5', '#ff5fa2']

// ============ 骑士纹章 ============
// img 位于 public/crest/，已经预裁成对准头盔的正方形，圆形纹章直接 object-fit: cover 即可
export const CRESTS = {
  rabbittank: {
    img: '/crest/rabbittank.webp',
    alt: '假面骑士创骑 · 兔子坦克形态纹章'
  },
  hazard: {
    img: '/crest/hazard.webp',
    alt: '假面骑士创骑 · 危险形态纹章'
  },
  genius: {
    img: '/crest/genius.webp',
    alt: '假面骑士创骑 · 天才形态纹章'
  }
}

// 必杀飞踢横版图：变身成功 / 入队成功时的全屏特写
export const CREST_FINISH = '/crest/finish.webp'
