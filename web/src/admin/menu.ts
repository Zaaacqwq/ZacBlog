import { matchPath } from 'react-router-dom'

export interface MenuEntry {
  path: string
  title: string
}

// 侧栏菜单只有这些；个人中心放在右上角用户下拉里
export const MENU: MenuEntry[] = [
  { path: '/admin', title: '首页' },
  { path: '/admin/articles', title: '文章' },
  { path: '/admin/categories', title: '分类' },
  { path: '/admin/tags', title: '标签' },
  { path: '/admin/comments', title: '评论' },
  { path: '/admin/messages', title: '留言' },
  { path: '/admin/readers', title: '读者管理' },
  { path: '/admin/notices', title: '通知公告' },
  { path: '/admin/files', title: '文件' },
]

const EXTRA: MenuEntry[] = [
  { path: '/admin/articles/new', title: '新建文章' },
  { path: '/admin/articles/:id/edit', title: '编辑文章' },
  { path: '/admin/profile', title: '个人中心' },
]

/** 页面标题，用于面包屑和标签页；未知路径返回 null */
export function titleFor(pathname: string): string | null {
  const normalized = pathname.replace(/\/+$/, '') || '/'
  for (const entry of [...EXTRA, ...MENU]) {
    if (matchPath({ path: entry.path, end: true }, normalized)) return entry.title
  }
  return null
}

/** 当前路径属于哪个菜单项（新建/编辑文章归到“文章”） */
export function menuKeyFor(pathname: string): string {
  const hit = MENU.filter((m) => m.path !== '/admin' && pathname.startsWith(m.path))
  return hit[0]?.path ?? '/admin'
}

/** 登录页地址，登录成功后回到 redirect */
export function loginPath(redirect: string): string {
  return `/admin/login?redirect=${encodeURIComponent(redirect)}`
}
