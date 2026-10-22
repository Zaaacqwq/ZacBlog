/** 旧 URL → 新 URL（只在 SPA 路由里做；nginx 已经让所有路径回落到 index.html） */
export const LEGACY_REDIRECTS: Record<string, string> = {
  '/cms': '/',
  '/cms/main': '/',
  '/cms/main/cmsIndex': '/',
  '/cms/main/essay': '/notes',
  '/cms/main/message': '/messages',
  '/cms/doucument': '/about',
  '/cmsLogin': '/',
  '/cmsRegister': '/',
  '/login': '/admin/login',
  '/index': '/admin',
}
