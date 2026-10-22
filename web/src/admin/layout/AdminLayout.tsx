import {
  AppstoreOutlined,
  CloseOutlined,
  CommentOutlined,
  FileTextOutlined,
  FolderOutlined,
  HomeOutlined,
  LogoutOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  MessageOutlined,
  NotificationOutlined,
  TagsOutlined,
  TeamOutlined,
  UserOutlined,
} from '@ant-design/icons'
import { Avatar, Breadcrumb, Dropdown, Menu, Tooltip } from 'antd'
import { useEffect, useState, type ReactNode } from 'react'
import { Link, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { UNAUTHORIZED_EVENT } from '../../shared/api'
import { clearToken } from '../../shared/token'
import { adminApi, fileUrl, type Me } from '../api'
import logo from '../assets/logo.png'
import { MENU, loginPath, menuKeyFor, titleFor } from '../menu'

const ICONS: Record<string, ReactNode> = {
  '/admin': <HomeOutlined />,
  '/admin/articles': <FileTextOutlined />,
  '/admin/categories': <AppstoreOutlined />,
  '/admin/tags': <TagsOutlined />,
  '/admin/comments': <CommentOutlined />,
  '/admin/messages': <MessageOutlined />,
  '/admin/readers': <TeamOutlined />,
  '/admin/notices': <NotificationOutlined />,
  '/admin/files': <FolderOutlined />,
}

interface TagView {
  path: string
  title: string
}

const HOME_TAG: TagView = { path: '/admin', title: '首页' }

/** 若依风格布局：左侧深色可折叠菜单、顶部导航栏（面包屑 + 用户下拉）、标签页栏 */
export default function AdminLayout() {
  const location = useLocation()
  const navigate = useNavigate()
  const [collapsed, setCollapsed] = useState(() => localStorage.getItem('zacblog_sidebar') === 'closed')
  const [tags, setTags] = useState<TagView[]>([HOME_TAG])
  const [me, setMe] = useState<Me | null>(null)

  const path = location.pathname.replace(/\/+$/, '') || '/admin'
  const title = titleFor(path)

  useEffect(() => {
    const onUnauthorized = () =>
      navigate(loginPath(window.location.pathname + window.location.search), { replace: true })
    window.addEventListener(UNAUTHORIZED_EVENT, onUnauthorized)
    return () => window.removeEventListener(UNAUTHORIZED_EVENT, onUnauthorized)
  }, [navigate])

  useEffect(() => {
    adminApi.me().then(setMe, () => {})
  }, [])

  useEffect(() => {
    if (!title) return
    // 标签页跟随路由变化追加，属于“同步外部状态”
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setTags((prev) => (prev.some((t) => t.path === path) ? prev : [...prev, { path, title }]))
  }, [path, title])

  const toggle = () => {
    setCollapsed((c) => {
      localStorage.setItem('zacblog_sidebar', c ? 'opened' : 'closed')
      return !c
    })
  }

  const closeTag = (target: string) => {
    const index = tags.findIndex((t) => t.path === target)
    const next = tags.filter((t) => t.path !== target)
    setTags(next)
    if (target === path) navigate(next[Math.min(index, next.length) - 1]?.path ?? '/admin')
  }

  const closeOthers = (keep: string) => {
    setTags((prev) => prev.filter((t) => t.path === HOME_TAG.path || t.path === keep))
    if (keep !== path) navigate(keep)
  }

  const closeAll = () => {
    setTags([HOME_TAG])
    navigate('/admin')
  }

  const logout = () => {
    clearToken()
    navigate('/admin/login', { replace: true })
  }

  const menuKey = menuKeyFor(path)
  const menuTitle = MENU.find((m) => m.path === menuKey)?.title

  const crumbs = [{ title: <Link to="/admin">首页</Link> }]
  if (path !== '/admin') {
    if (menuKey !== '/admin' && menuTitle && menuTitle !== title) {
      crumbs.push({ title: <Link to={menuKey}>{menuTitle}</Link> })
    }
    crumbs.push({ title: <span className="no-redirect">{title ?? '页面不存在'}</span> })
  }

  return (
    <div className={`app-wrapper${collapsed ? ' hideSidebar' : ''}`}>
      <aside className="sidebar-container">
        <Link to="/admin" className="sidebar-logo-link">
          <img src={logo} alt="" className="sidebar-logo" />
          {!collapsed && <h1 className="sidebar-title">Zac Console</h1>}
        </Link>
        <div className="sidebar-menu">
          <Menu
            mode="inline"
            theme="dark"
            inlineCollapsed={collapsed}
            selectedKeys={[menuKey]}
            items={MENU.map((m) => ({
              key: m.path,
              icon: ICONS[m.path],
              label: <Link to={m.path}>{m.title}</Link>,
            }))}
          />
        </div>
      </aside>

      <div className="main-container">
        <div className="fixed-header">
          <header className="navbar">
            <button
              type="button"
              className="hamburger-container"
              aria-label={collapsed ? '展开菜单' : '收起菜单'}
              onClick={toggle}
            >
              {collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
            </button>
            <Breadcrumb className="breadcrumb-container" items={crumbs} />
            <div className="right-menu">
              <Dropdown
                trigger={['click']}
                menu={{
                  items: [
                    { key: 'profile', icon: <UserOutlined />, label: <Link to="/admin/profile">个人中心</Link> },
                    { type: 'divider' },
                    { key: 'logout', icon: <LogoutOutlined />, label: '退出登录', onClick: logout },
                  ],
                }}
              >
                <button type="button" className="avatar-wrapper" aria-label="用户菜单">
                  <Avatar
                    size={36}
                    shape="square"
                    src={me?.avatarUrl ? fileUrl(me.avatarUrl) : undefined}
                    icon={<UserOutlined />}
                  />
                  <span className="user-name">{me?.nickname || me?.username || '管理员'}</span>
                </button>
              </Dropdown>
            </div>
          </header>

          <nav className="tags-view-container" aria-label="标签页">
            <div className="tags-view-wrapper">
              {tags.map((tag) => (
                <Dropdown
                  key={tag.path}
                  trigger={['contextMenu']}
                  menu={{
                    items: [
                      { key: 'close', label: '关闭', disabled: tag.path === HOME_TAG.path },
                      { key: 'others', label: '关闭其他' },
                      { key: 'all', label: '关闭所有' },
                    ],
                    onClick: ({ key }) => {
                      if (key === 'close') closeTag(tag.path)
                      if (key === 'others') closeOthers(tag.path)
                      if (key === 'all') closeAll()
                    },
                  }}
                >
                  <Link to={tag.path} className={`tags-view-item${tag.path === path ? ' active' : ''}`}>
                    {tag.title}
                    {tag.path !== HOME_TAG.path && (
                      <Tooltip title="关闭" mouseEnterDelay={0.6}>
                        <CloseOutlined
                          className="tag-close"
                          aria-label={`关闭 ${tag.title}`}
                          onClick={(e) => {
                            e.preventDefault()
                            e.stopPropagation()
                            closeTag(tag.path)
                          }}
                        />
                      </Tooltip>
                    )}
                  </Link>
                </Dropdown>
              ))}
            </div>
          </nav>
        </div>

        <main className="app-main">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
