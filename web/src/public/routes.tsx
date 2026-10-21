import type { RouteObject } from 'react-router-dom'
import HomePage from './pages/HomePage'

// 前台路由（T4 负责）
export const publicRoutes: RouteObject[] = [{ path: '/', element: <HomePage /> }]
