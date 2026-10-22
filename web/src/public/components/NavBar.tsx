import { useState, type FormEvent } from 'react'
import { Link, NavLink, useLocation, useNavigate } from 'react-router-dom'
import logo from '../assets/logo.png'
import Icon, { type IconName } from './Icon'

const MENU: { to: string; label: string; icon: IconName }[] = [
  { to: '/', label: 'Home', icon: 'home' },
  { to: '/notes', label: 'Notes', icon: 'pen' },
  { to: '/messages', label: 'Comments', icon: 'chat' },
  { to: '/about', label: 'About', icon: 'user' },
]

export default function NavBar() {
  const navigate = useNavigate()
  const location = useLocation()
  const [keyword, setKeyword] = useState('')
  const [open, setOpen] = useState(false)
  // 首页顶部压在大图上时导航栏用半透明深色，其余页面是白色玻璃
  const onHero = location.pathname === '/'

  function onSearch(event: FormEvent) {
    event.preventDefault()
    const q = keyword.trim()
    setOpen(false)
    navigate(q ? `/?keyword=${encodeURIComponent(q)}` : '/')
  }

  const search = (
    <form className="nav-search" role="search" onSubmit={onSearch}>
      <Icon name="search" />
      <input
        type="search"
        aria-label="搜索文章"
        placeholder="Search articles"
        value={keyword}
        onChange={(e) => setKeyword(e.target.value)}
      />
    </form>
  )

  const links = MENU.map((item) => (
    <NavLink key={item.to} to={item.to} end={item.to === '/'} onClick={() => setOpen(false)}>
      <Icon name={item.icon} />
      {item.label}
    </NavLink>
  ))

  return (
    <header className={`site-nav${onHero ? ' on-hero' : ''}`}>
      <Link className="brand" to="/">
        <span className="brand-badge">
          <img src={logo} alt="Zac Blog logo" />
        </span>
        <span className="brand-copy">
          <strong>Zac Blog</strong>
          <small>notes, builds, and essays</small>
        </span>
      </Link>
      <nav className="nav-menu" aria-label="主导航">
        {links}
      </nav>
      <div className="nav-right">
        {search}
        {/* 只有博主登录后台；读者不需要注册或登录 */}
        <Link className="nav-login" to="/admin/login">
          Login
        </Link>
        <button
          className="nav-toggle"
          type="button"
          aria-label="菜单"
          aria-expanded={open}
          onClick={() => setOpen((v) => !v)}
        >
          <Icon name="menu" />
        </button>
      </div>
      {open && (
        <div className="nav-mobile">
          <nav aria-label="移动端导航">{links}</nav>
          {search}
        </div>
      )}
    </header>
  )
}
