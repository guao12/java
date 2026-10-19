<script setup>
/**
 * 卡塞尔学院徽记 —— 龙文法阵 / 鎏金封印
 */
</script>

<template>
  <svg viewBox="0 0 120 120" width="100%" height="100%" role="img" aria-label="卡塞尔学院徽记">
    <defs>
      <linearGradient id="emblemGold" x1="0" y1="0" x2="0.4" y2="1">
        <stop offset="0%" stop-color="#f6e3ab" />
        <stop offset="45%" stop-color="#d8b25a" />
        <stop offset="100%" stop-color="#8f6a25" />
      </linearGradient>
      <radialGradient id="emblemGlow" cx="50%" cy="50%" r="50%">
        <stop offset="55%" stop-color="rgba(216,178,90,0.18)" />
        <stop offset="100%" stop-color="rgba(216,178,90,0)" />
      </radialGradient>
    </defs>

    <circle cx="60" cy="60" r="58" fill="url(#emblemGlow)" />

    <!-- 外环 -->
    <circle cx="60" cy="60" r="52" fill="none" stroke="url(#emblemGold)" stroke-width="1.6" />
    <circle cx="60" cy="60" r="46" fill="none" stroke="rgba(216,178,90,0.42)" stroke-width="0.8" />
    <circle
      cx="60"
      cy="60"
      r="38"
      fill="none"
      stroke="rgba(216,178,90,0.3)"
      stroke-width="0.8"
      stroke-dasharray="3 5"
    />

    <!-- 刻度 -->
    <g stroke="url(#emblemGold)" stroke-width="1.4" stroke-linecap="round">
      <path d="M60 6v9M60 105v9M6 60h9M105 60h9" />
      <g stroke-width="0.9" opacity="0.7">
        <path d="M60 12.5v5M60 102.5v5M12.5 60h5M102.5 60h5" transform="rotate(45 60 60)" />
        <path d="M60 16v4M60 100v4M16 60h4M100 60h4" transform="rotate(-45 60 60)" />
      </g>
    </g>

    <!-- 中心龍 -->
    <text
      x="60"
      y="76"
      text-anchor="middle"
      font-family="'Cinzel','Noto Serif SC','Songti SC',serif"
      font-size="44"
      fill="url(#emblemGold)"
      opacity="0.95"
    >
      龍
    </text>

    <!-- 侧翼龙纹 -->
    <g fill="none" stroke="rgba(216,178,90,0.55)" stroke-width="1.1" stroke-linecap="round">
      <path d="M22 60c6-9 12-13 19-13" />
      <path d="M98 60c-6-9-12-13-19-13" />
      <path d="M22 60c6 9 12 13 19 13" opacity="0.5" />
      <path d="M98 60c-6 9-12 13-19 13" opacity="0.5" />
    </g>
  </svg>
</template>
