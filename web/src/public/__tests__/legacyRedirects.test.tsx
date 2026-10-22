import { screen, waitFor } from '@testing-library/react'
import { mockApi, renderAt } from './helpers'
import { PRELOAD_TIMEOUT, preloadAdminRoutes } from '../../test/preloadAdmin'

beforeAll(preloadAdminRoutes, PRELOAD_TIMEOUT)

afterEach(() => vi.unstubAllGlobals())
beforeEach(() => {
  mockApi()
})

// 旧 URL 跳转表，和任务说明一一对应
const CASES: [from: string, to: string][] = [
  ['/cms', '/'],
  ['/cms/main', '/'],
  ['/cms/main/cmsIndex', '/'],
  ['/cms/main/blog?id=42', '/posts/42'],
  ['/cms/main/blog?id=42&commentId=7', '/posts/42?commentId=7'],
  ['/cms/main/essay', '/notes'],
  ['/cms/main/message', '/messages'],
  ['/cms/doucument', '/about'],
  ['/cmsLogin', '/'],
  ['/cmsRegister', '/'],
  ['/login', '/admin/login'],
  ['/index', '/admin'],
]

describe('legacy URL redirects', () => {
  it.each(CASES)('%s → %s', async (from, to) => {
    const router = renderAt(from)
    await waitFor(() => {
      const { pathname, search } = router.state.location
      expect(pathname + search).toBe(to)
    })
  })

  it('sends /cms/main/blog without a valid id to the home page', async () => {
    const router = renderAt('/cms/main/blog?id=abc')
    await waitFor(() => expect(router.state.location.pathname).toBe('/'))
  })

  it('shows a 404 page for unknown paths', async () => {
    const router = renderAt('/no/such/page')
    expect(await screen.findByRole('heading', { name: '页面不存在' })).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/no/such/page')
  })

  it('has no reader login or register page', async () => {
    renderAt('/register')
    expect(await screen.findByRole('heading', { name: '页面不存在' })).toBeInTheDocument()
  })
})
