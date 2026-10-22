import type { RouteObject } from 'react-router-dom'
import { adminRoutes } from '../admin/routes'

/** 模块加载的单独超时：只用于 beforeAll 里的 import，不放宽任何用例本身的超时 */
export const PRELOAD_TIMEOUT = 30_000

function collect(routes: RouteObject[]): (() => unknown)[] {
  return routes.flatMap((r) => [...(typeof r.lazy === 'function' ? [r.lazy as () => unknown] : []), ...collect(r.children ?? [])])
}

/**
 * 后台页面都是懒加载的，首次 import antd 在 CI 上要好几秒，原本全算在每个文件的第一个用例里。
 * 在 beforeAll 里提前加载一遍，同一测试文件内的模块缓存会让路由的 lazy() 立即返回，
 * 用例只需要等渲染本身。
 */
export async function preloadAdminRoutes() {
  await Promise.all(collect(adminRoutes).map((load) => load()))
}

/**
 * 每个文件第一个渲染整套后台布局的用例要承担 antd 首次渲染（样式生成、JIT 预热），
 * 本机约 0.5 秒，CI runner 并行时会放大好几倍。只给这些用例单独放宽，不改全局 testTimeout。
 */
export const FIRST_RENDER_TIMEOUT = 15_000
