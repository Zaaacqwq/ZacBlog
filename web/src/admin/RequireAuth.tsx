import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { getToken } from '../shared/token'
import { loginPath } from './menu'

/** 路由守卫：没有 token 时跳到登录页，登录后回到原地址 */
export default function RequireAuth({ children }: { children: ReactNode }) {
  const location = useLocation()
  if (!getToken()) {
    return <Navigate to={loginPath(location.pathname + location.search)} replace />
  }
  return <>{children}</>
}
