import type { RouteObject } from 'react-router-dom'
import AdminHomePage from './pages/AdminHomePage'

// 后台路由（T5 负责），统一挂在 /admin 下
export const adminRoutes: RouteObject[] = [{ path: '/admin/*', element: <AdminHomePage /> }]
