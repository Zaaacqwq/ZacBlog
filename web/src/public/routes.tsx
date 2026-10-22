import { Navigate, type RouteObject } from 'react-router-dom'
import PublicLayout from './components/PublicLayout'
import { LegacyBlogRedirect } from './legacy'
import { LEGACY_REDIRECTS } from './legacyRedirects'
import AboutPage from './pages/AboutPage'
import HomePage from './pages/HomePage'
import MessagesPage from './pages/MessagesPage'
import NotFoundPage from './pages/NotFoundPage'
import NotesPage from './pages/NotesPage'
import PostPage from './pages/PostPage'

// 前台路由（T4 负责）
export const publicRoutes: RouteObject[] = [
  {
    element: <PublicLayout />,
    children: [
      { path: '/', element: <HomePage /> },
      { path: '/posts/:id', element: <PostPage /> },
      { path: '/notes', element: <NotesPage /> },
      { path: '/messages', element: <MessagesPage /> },
      { path: '/about', element: <AboutPage /> },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
  { path: '/cms/main/blog', element: <LegacyBlogRedirect /> },
  ...Object.entries(LEGACY_REDIRECTS).map(([from, to]) => ({
    path: from,
    element: <Navigate to={to} replace />,
  })),
]
