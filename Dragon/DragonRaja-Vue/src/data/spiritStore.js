import { ref } from 'vue'
import { SPIRIT_GALLERY } from './codex'
import { homeApi } from '@/api/home'

/**
 * 言灵图鉴共享数据源。
 *
 * 初始值为内置静态数据（保证后端不可用时页面照常渲染），
 * 挂载首页 / 言灵详情页时调用 ensureSpirits() 异步拉取
 * 后台管理系统维护的言灵列表（GET /api/home/spirits），
 * 拉取成功后整表替换，后台的增删改即可实时反映到前台。
 *
 * 每次调用都会重新拉取（后台改完数据返回首页即可看到最新），
 * 同一时刻并发调用共享同一个请求，不重复打接口。
 */
const spirits = ref([...SPIRIT_GALLERY])

/** 当前是否有一次进行中的请求 */
let inflight = null

/** 响应式言灵列表：组件里直接 `const list = useSpirits()` */
export function useSpirits() {
  return spirits
}

/** 拉取后台维护的言灵数据；失败时保持现状（静态兜底），永不抛错 */
export function ensureSpirits() {
  if (!inflight) {
    inflight = homeApi
      .spirits()
      .then((data) => {
        if (Array.isArray(data) && data.length) {
          spirits.value = data
        }
      })
      .catch(() => {})
      .finally(() => {
        inflight = null
      })
  }
  return inflight
}
