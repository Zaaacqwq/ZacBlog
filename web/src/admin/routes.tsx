import type { RouteObject } from 'react-router-dom'

// 后台路由（T5 负责），统一挂在 /admin 下。页面按需加载，前台不会打包 antd
const page = (load: () => Promise<{ default: React.ComponentType }>) => async () => ({
  Component: (await load()).default,
})

export const adminRoutes: RouteObject[] = [
  {
    path: '/admin',
    // 首次加载后台代码时什么都不显示
    HydrateFallback: () => null,
    lazy: page(() => import('./AdminRoot')),
    children: [
      { path: 'login', lazy: page(() => import('./pages/LoginPage')) },
      {
        lazy: page(() => import('./layout/GuardedLayout')),
        children: [
          { index: true, lazy: page(() => import('./pages/DashboardPage')) },
          { path: 'articles', lazy: page(() => import('./pages/ArticleListPage')) },
          { path: 'articles/new', lazy: page(() => import('./pages/ArticleEditPage')) },
          { path: 'articles/:id/edit', lazy: page(() => import('./pages/ArticleEditPage')) },
          { path: 'categories', lazy: page(() => import('./pages/CategoryPage')) },
          { path: 'tags', lazy: page(() => import('./pages/TagPage')) },
          { path: 'comments', lazy: page(() => import('./pages/CommentListPage')) },
          { path: 'messages', lazy: page(() => import('./pages/MessageListPage')) },
          { path: 'readers', lazy: page(() => import('./pages/ReaderPage')) },
          { path: 'notices', lazy: page(() => import('./pages/NoticePage')) },
          { path: 'files', lazy: page(() => import('./pages/FilePage')) },
          { path: 'profile', lazy: page(() => import('./pages/ProfilePage')) },
          { path: '*', lazy: page(() => import('./pages/NotFoundPage')) },
        ],
      },
    ],
  },
]
