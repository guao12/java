// 形态主题与纹章（SVG）数据

export const FORMS = {
  rabbittank: {
    name: '兔子坦克',
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

export const CRESTS = {
  rabbittank: `<svg viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg">
    <defs>
      <linearGradient id="rtRed" x1="0" y1="0" x2="1" y2="1"><stop offset="0%" stop-color="#ff5a6a"/><stop offset="100%" stop-color="#c8102e"/></linearGradient>
      <linearGradient id="rtBlue" x1="0" y1="0" x2="1" y2="1"><stop offset="0%" stop-color="#36c2f5"/><stop offset="100%" stop-color="#0072bc"/></linearGradient>
      <linearGradient id="rtGold" x1="0" y1="0" x2="1" y2="1"><stop offset="0%" stop-color="#f5c518"/><stop offset="100%" stop-color="#c79a00"/></linearGradient>
      <clipPath id="rtHelm"><path d="M20,42 Q20,18 50,16 Q80,18 80,42 L80,60 Q80,76 50,78 Q20,76 20,60 Z"/></clipPath>
    </defs>
    <circle cx="50" cy="50" r="47" fill="none" stroke="url(#rtGold)" stroke-width="3"/>
    <circle cx="50" cy="50" r="41" fill="none" stroke="rgba(255,59,59,0.5)" stroke-width="1.5"/>
    <g clip-path="url(#rtHelm)">
      <rect x="0" y="0" width="50" height="100" fill="url(#rtRed)"/>
      <rect x="50" y="0" width="50" height="100" fill="url(#rtBlue)"/>
      <path d="M33,26 C27,12 31,5 37,4 C41,3 41,14 42,24 Z" fill="#a30c24"/>
      <path d="M44,26 C42,14 47,9 50,9 C51,9 50,18 50,26 Z" fill="#a30c24"/>
      <g stroke="#00609e" stroke-width="2.4" stroke-linecap="round">
        <line x1="60" y1="52" x2="74" y2="52"/><line x1="60" y1="58" x2="74" y2="58"/><line x1="60" y1="64" x2="74" y2="64"/>
      </g>
      <rect x="74" y="40" width="9" height="7" rx="2" fill="#00609e" transform="rotate(18 78 43)"/>
      <ellipse class="eye-l" cx="36" cy="46" rx="7" ry="9" fill="#fff"/><circle class="eye-l" cx="36" cy="47" r="3.4" fill="#ff3b3b"/>
      <ellipse class="eye-r" cx="64" cy="46" rx="7" ry="9" fill="#fff"/><circle class="eye-r" cx="64" cy="47" r="3.4" fill="#00a6e6"/>
    </g>
    <line x1="50" y1="16" x2="50" y2="78" stroke="url(#rtGold)" stroke-width="2"/>
    <path d="M50,42 L56,50 L50,58 L44,50 Z" fill="url(#rtGold)"/>
    <circle class="halo" cx="36" cy="47" r="6" fill="none" stroke="#ff3b3b" stroke-width="2"/>
    <circle class="halo" cx="64" cy="47" r="6" fill="none" stroke="#00a6e6" stroke-width="2"/>
  </svg>`,

  hazard: `<svg viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg">
    <defs>
      <linearGradient id="hzL" x1="0" y1="0" x2="1" y2="1"><stop offset="0%" stop-color="#2a0a0a"/><stop offset="100%" stop-color="#0c0606"/></linearGradient>
      <linearGradient id="hzR" x1="0" y1="0" x2="1" y2="1"><stop offset="0%" stop-color="#160404"/><stop offset="100%" stop-color="#050202"/></linearGradient>
      <linearGradient id="hzRed" x1="0" y1="0" x2="1" y2="1"><stop offset="0%" stop-color="#ff4d4d"/><stop offset="100%" stop-color="#b30000"/></linearGradient>
      <clipPath id="hzHelm"><path d="M20,42 Q20,18 50,16 Q80,18 80,42 L80,60 Q80,76 50,78 Q20,76 20,60 Z"/></clipPath>
    </defs>
    <circle cx="50" cy="50" r="47" fill="none" stroke="url(#hzRed)" stroke-width="3"/>
    <circle cx="50" cy="50" r="41" fill="none" stroke="rgba(255,40,40,0.5)" stroke-width="1.5"/>
    <g clip-path="url(#hzHelm)">
      <rect x="0" y="0" width="50" height="100" fill="url(#hzL)"/>
      <rect x="50" y="0" width="50" height="100" fill="url(#hzR)"/>
      <path d="M33,26 C27,12 31,5 37,4 C41,3 41,14 42,24 Z" fill="#3a0a0a"/>
      <path d="M44,26 C42,14 47,9 50,9 C51,9 50,18 50,26 Z" fill="#3a0a0a"/>
      <g stroke="#5a0a0a" stroke-width="2.4" stroke-linecap="round">
        <line x1="60" y1="52" x2="74" y2="52"/><line x1="60" y1="58" x2="74" y2="58"/><line x1="60" y1="64" x2="74" y2="64"/>
      </g>
      <rect x="74" y="40" width="9" height="7" rx="2" fill="#5a0a0a" transform="rotate(18 78 43)"/>
      <path d="M28,30 L40,44 L34,56 L46,66" fill="none" stroke="#ff2a2a" stroke-width="1.5" stroke-linecap="round" opacity=".85"/>
      <path d="M72,32 L60,46 L68,58 L56,68" fill="none" stroke="#ff2a2a" stroke-width="1.5" stroke-linecap="round" opacity=".85"/>
      <ellipse class="eye-l" cx="36" cy="46" rx="7" ry="9" fill="#fff"/><circle class="eye-l" cx="36" cy="47" r="3.6" fill="#ff1f1f"/>
      <ellipse class="eye-r" cx="64" cy="46" rx="7" ry="9" fill="#fff"/><circle class="eye-r" cx="64" cy="47" r="3.6" fill="#ff1f1f"/>
    </g>
    <line x1="50" y1="16" x2="50" y2="78" stroke="url(#hzRed)" stroke-width="2"/>
    <path d="M50,42 L56,50 L50,58 L44,50 Z" fill="url(#hzRed)"/>
    <circle class="halo" cx="36" cy="47" r="6" fill="none" stroke="#ff1f1f" stroke-width="2"/>
    <circle class="halo" cx="64" cy="47" r="6" fill="none" stroke="#ff1f1f" stroke-width="2"/>
  </svg>`,

  // 天才形态：彩色纹章（彩虹头盔 + 彩虹双眼 + 彩虹光环）
  genius: `<svg viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg">
    <defs>
      <linearGradient id="gnRain" x1="0" y1="0" x2="1" y2="0">
        <stop offset="0%" stop-color="#ff3b3b"/><stop offset="33%" stop-color="#f5c518"/>
        <stop offset="66%" stop-color="#00a6e6"/><stop offset="100%" stop-color="#7ad7ff"/>
      </linearGradient>
      <linearGradient id="gnRainV" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0%" stop-color="#ff3b3b"/><stop offset="25%" stop-color="#f5c518"/>
        <stop offset="50%" stop-color="#00a6e6"/><stop offset="75%" stop-color="#7ad7ff"/>
        <stop offset="100%" stop-color="#9b5de5"/>
      </linearGradient>
      <linearGradient id="gnGold" x1="0" y1="0" x2="1" y2="1"><stop offset="0%" stop-color="#ffe14d"/><stop offset="100%" stop-color="#e0a900"/></linearGradient>
      <clipPath id="gnHelm"><path d="M20,42 Q20,18 50,16 Q80,18 80,42 L80,60 Q80,76 50,78 Q20,76 20,60 Z"/></clipPath>
    </defs>
    <circle cx="50" cy="50" r="47" fill="none" stroke="url(#gnGold)" stroke-width="3"/>
    <circle cx="50" cy="50" r="41" fill="none" stroke="rgba(245,197,24,0.5)" stroke-width="1.5"/>
    <g clip-path="url(#gnHelm)">
      <rect x="0" y="0" width="50" height="100" fill="url(#gnRainV)"/>
      <rect x="50" y="0" width="50" height="100" fill="url(#gnRainV)" opacity="0.9"/>
      <path d="M33,26 C27,12 31,5 37,4 C41,3 41,14 42,24 Z" fill="#ffffff" opacity="0.85"/>
      <path d="M44,26 C42,14 47,9 50,9 C51,9 50,18 50,26 Z" fill="#ffffff" opacity="0.85"/>
      <g stroke="#ffffff" stroke-width="2.4" stroke-linecap="round" opacity="0.9">
        <line x1="60" y1="52" x2="74" y2="52"/><line x1="60" y1="58" x2="74" y2="58"/><line x1="60" y1="64" x2="74" y2="64"/>
      </g>
      <rect x="74" y="40" width="9" height="7" rx="2" fill="#ffffff" opacity="0.9" transform="rotate(18 78 43)"/>
      <path d="M28,30 Q50,7 72,30" fill="none" stroke="url(#gnRain)" stroke-width="2.6" stroke-linecap="round" opacity=".95"/>
      <ellipse class="eye-l" cx="36" cy="46" rx="7" ry="9" fill="#fff"/>
      <circle class="eye-l" cx="36" cy="47" r="3.6" fill="url(#gnRain)"/>
      <ellipse class="eye-r" cx="64" cy="46" rx="7" ry="9" fill="#fff"/>
      <circle class="eye-r" cx="64" cy="47" r="3.6" fill="url(#gnRain)"/>
    </g>
    <line x1="50" y1="16" x2="50" y2="78" stroke="url(#gnGold)" stroke-width="2"/>
    <path d="M50,42 L56,50 L50,58 L44,50 Z" fill="url(#gnGold)"/>
    <circle class="halo" cx="36" cy="47" r="6" fill="none" stroke="url(#gnRain)" stroke-width="2"/>
    <circle class="halo" cx="64" cy="47" r="6" fill="none" stroke="url(#gnRain)" stroke-width="2"/>
  </svg>`
}
