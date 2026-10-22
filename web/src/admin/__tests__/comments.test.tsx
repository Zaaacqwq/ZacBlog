import { act, screen, waitFor } from '@testing-library/react'
import { clearToken, setToken } from '../../shared/token'
import { mockApi, renderAt, route, type Call } from './helpers'
import { FIRST_RENDER_TIMEOUT, PRELOAD_TIMEOUT, preloadAdminRoutes } from '../../test/preloadAdmin'

beforeAll(preloadAdminRoutes, PRELOAD_TIMEOUT)

let calls: Call[]

beforeEach(() => {
  setToken('test-only-token')
  calls = mockApi((c) => {
    if (route(c) === 'GET /api/admin/me') return { data: { username: 'admin', nickname: 'Zac', avatarUrl: null } }
    if (route(c) === 'GET /api/admin/comments') return { data: { rows: [], total: 0 } }
  })
})

afterEach(() => {
  vi.unstubAllGlobals()
  clearToken()
})

const listCalls = () => calls.filter((c) => route(c) === 'GET /api/admin/comments')

describe('comment list reader filter', () => {
  it('follows the reader query parameter when it changes on the same route', async () => {
    const router = renderAt('/admin/comments?reader=a%40example.com')
    const input = (await screen.findByPlaceholderText('邮箱或 user:id')) as HTMLInputElement
    expect(input.value).toBe('a@example.com')
    await waitFor(() => expect(listCalls().at(-1)?.url).toContain('reader=a%40example.com'))

    // 点标签页回到不带参数的评论管理
    await act(() => router.navigate('/admin/comments'))
    await waitFor(() => expect(listCalls().length).toBeGreaterThanOrEqual(2))
    await waitFor(() => expect(listCalls().at(-1)?.url).not.toContain('reader='))
    expect((screen.getByPlaceholderText('邮箱或 user:id') as HTMLInputElement).value).toBe('')
  }, FIRST_RENDER_TIMEOUT)
})
