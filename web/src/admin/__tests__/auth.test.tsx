import { fireEvent, screen, waitFor } from '@testing-library/react'
import { clearToken, getToken, setToken } from '../../shared/token'
import { mockApi, renderAt, route } from './helpers'

const CAPTCHA = { captchaId: 'c1', image: 'data:image/png;base64,AAAA' }
const DASHBOARD = {
  articleCount: 3,
  essayCount: 1,
  draftCount: 0,
  commentCount: 5,
  messageCount: 2,
  totalViews: 99,
  last7Days: [{ date: '2026-10-21', articles: 1, comments: 2, messages: 0 }],
  articlesByCategory: [],
  articlesByTag: [],
}

function fillLogin(code = '7') {
  fireEvent.change(screen.getByLabelText('账号'), { target: { value: 'admin' } })
  fireEvent.change(screen.getByLabelText('密码'), { target: { value: 'test-only-password' } })
  fireEvent.change(screen.getByLabelText('验证码'), { target: { value: code } })
  fireEvent.click(screen.getByRole('button', { name: /login/i }))
}

afterEach(() => {
  vi.unstubAllGlobals()
  clearToken()
  localStorage.clear()
})

describe('route guard', () => {
  it('redirects to the login page with redirect when there is no token', async () => {
    mockApi((c) => (route(c) === 'GET /api/auth/captcha' ? { data: CAPTCHA } : undefined))
    const router = renderAt('/admin/articles')
    expect(await screen.findByText('Zac Admin System')).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/admin/login')
    expect(router.state.location.search).toBe(`?redirect=${encodeURIComponent('/admin/articles')}`)
  })

  it('renders the layout with only the allowed menu when logged in', async () => {
    setToken('test-only-token')
    mockApi((c) => {
      if (route(c) === 'GET /api/admin/me') return { data: { username: 'admin', nickname: 'Zac', avatarUrl: null } }
      if (route(c) === 'GET /api/admin/dashboard') return { data: DASHBOARD }
    })
    renderAt('/admin')
    expect(await screen.findByText('Zac Console')).toBeInTheDocument()
    const menu = screen.getByRole('menu')
    const labels = Array.from(menu.querySelectorAll('[role="menuitem"]')).map((el) => el.textContent?.trim())
    expect(labels).toEqual(['首页', '文章', '分类', '标签', '评论', '留言', '读者管理', '通知公告', '文件'])
    for (const banned of ['代码生成', '表单构建', '定时任务', '部门', '岗位', '字典', '菜单管理', '角色', '监控']) {
      expect(screen.queryByText(banned)).not.toBeInTheDocument()
    }
    expect(await screen.findByText('Zac')).toBeInTheDocument()
    expect(await screen.findByText('阅读量')).toBeInTheDocument()
  })

  it('sends the user back to login when the server answers 401', async () => {
    setToken('expired-test-token')
    mockApi((c) => {
      if (route(c) === 'GET /api/auth/captcha') return { data: CAPTCHA }
      if (c.url.startsWith('/api/admin/')) return { status: 401, msg: '未登录' }
    })
    const router = renderAt('/admin/files')
    expect(await screen.findByText('Zac Admin System')).toBeInTheDocument()
    expect(getToken()).toBeNull()
    expect(router.state.location.pathname).toBe('/admin/login')
  })
})

describe('login', () => {
  it('logs in, stores the token and goes to the redirect target', async () => {
    const calls = mockApi((c) => {
      switch (route(c)) {
        case 'GET /api/auth/captcha':
          return { data: CAPTCHA }
        case 'POST /api/auth/login':
          return { data: { token: 'test-only-jwt', expiresAt: '2026-10-28T00:00:00+08:00' } }
        case 'GET /api/admin/me':
          return { data: { username: 'admin', nickname: 'Zac', avatarUrl: null } }
        case 'GET /api/admin/files':
          return { data: { rows: [], total: 0 } }
      }
    })
    const router = renderAt(`/admin/login?redirect=${encodeURIComponent('/admin/files')}`)
    await screen.findByAltText('验证码')
    fillLogin()

    await waitFor(() => expect(router.state.location.pathname).toBe('/admin/files'))
    expect(getToken()).toBe('test-only-jwt')
    expect(calls.find((c) => route(c) === 'POST /api/auth/login')?.body).toEqual({
      username: 'admin',
      password: 'test-only-password',
      captchaId: 'c1',
      captchaCode: '7',
    })
  })

  it('validates required fields before calling the server', async () => {
    const calls = mockApi((c) => (route(c) === 'GET /api/auth/captcha' ? { data: CAPTCHA } : undefined))
    renderAt('/admin/login')
    await screen.findByAltText('验证码')
    fireEvent.click(screen.getByRole('button', { name: /login/i }))
    expect(await screen.findByText('请输入您的账号')).toBeInTheDocument()
    expect(await screen.findByText('请输入您的密码')).toBeInTheDocument()
    expect(await screen.findByText('请输入验证码')).toBeInTheDocument()
    expect(calls.some((c) => route(c) === 'POST /api/auth/login')).toBe(false)
  })

  it('shows the retry hint on 429 and fetches a new captcha', async () => {
    const calls = mockApi((c) => {
      if (route(c) === 'GET /api/auth/captcha') return { data: CAPTCHA }
      if (route(c) === 'POST /api/auth/login') return { status: 429, msg: '操作太频繁', headers: { 'Retry-After': '42' } }
    })
    renderAt('/admin/login')
    await screen.findByAltText('验证码')
    fillLogin()
    expect(await screen.findByText('操作太频繁，请 42 秒后再试')).toBeInTheDocument()
    expect(getToken()).toBeNull()
    await waitFor(() => expect(calls.filter((c) => route(c) === 'GET /api/auth/captcha')).toHaveLength(2))
  })

  it('shows the server message on wrong password', async () => {
    mockApi((c) => {
      if (route(c) === 'GET /api/auth/captcha') return { data: CAPTCHA }
      if (route(c) === 'POST /api/auth/login') return { status: 401, msg: '用户名或密码错误' }
    })
    const router = renderAt('/admin/login')
    await screen.findByAltText('验证码')
    fillLogin()
    expect(await screen.findByText('用户名或密码错误')).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/admin/login')
  })
})
