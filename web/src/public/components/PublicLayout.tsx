import { useEffect, type ReactNode } from 'react'
import { Outlet, useLocation } from 'react-router-dom'
import Footer from './Footer'
import NavBar from './NavBar'
import '../public.css'

export default function PublicLayout() {
  const { pathname } = useLocation()
  // 切换页面回到顶部（带 commentId 的文章页由评论区自己定位）
  useEffect(() => {
    window.scrollTo?.(0, 0)
  }, [pathname])

  return (
    <div className="public-shell">
      <NavBar />
      <main className="public-main">
        <Outlet />
      </main>
      <Footer />
    </div>
  )
}

/** 主栏 + 右侧栏的两列布局 */
export function TwoColumn({ children, side }: { children: ReactNode; side: ReactNode }) {
  return (
    <div className="page-grid">
      <div className="page-main">{children}</div>
      {side}
    </div>
  )
}
