import type { RouteObject } from 'react-router-dom'
import { adminRoutes } from './admin/routes'
import { publicRoutes } from './public/routes'

// 只负责拼装；前台路由改 public/routes.tsx（T4），后台路由改 admin/routes.tsx（T5）
export const routes: RouteObject[] = [...publicRoutes, ...adminRoutes]
