import { fireEvent, screen, waitFor, within } from '@testing-library/react'
import { clearToken, setToken } from '../../shared/token'
import { mockApi, renderAt, route, type Call } from './helpers'

const readers = [
  { email: 'anon@example.com', nickname: '匿名读者', commentCount: 2, messageCount: 1, lastActiveAt: '2026-10-20T10:00:00+08:00', blocked: false },
  { email: 'old@example.com', nickname: '旧注册读者', commentCount: 5, messageCount: 0, lastActiveAt: '2024-01-01T10:00:00+08:00', blocked: true },
  { email: 'user:42', nickname: '没邮箱', commentCount: 1, messageCount: 0, lastActiveAt: '2023-01-01T10:00:00+08:00', blocked: false },
]

let calls: Call[]

beforeEach(() => {
  setToken('test-only-token')
  calls = mockApi((c) => {
    switch (route(c)) {
      case 'GET /api/admin/me':
        return { data: { username: 'admin', nickname: 'Zac', avatarUrl: null } }
      case 'GET /api/admin/readers':
        return { data: { rows: readers, total: readers.length } }
      case 'POST /api/admin/readers/anon%40example.com/block':
      case 'DELETE /api/admin/readers/old%40example.com/block':
        return { data: null }
      case 'GET /api/admin/readers/ip-blocks':
        return { data: { rows: [{ id: 3, ip: '203.0.113.7', reason: 'spam', createdAt: '2026-10-01T00:00:00+08:00' }], total: 1 } }
      case 'POST /api/admin/readers/ip-blocks':
        return { data: { id: 4, ip: '198.51.100.1', reason: '', createdAt: '2026-10-21T00:00:00+08:00' } }
      case 'DELETE /api/admin/readers/ip-blocks/3':
        return { data: null }
    }
  })
})

afterEach(() => {
  vi.unstubAllGlobals()
  clearToken()
})

const rowOf = async (text: string) => (await screen.findByText(text)).closest('tr') as HTMLElement

describe('reader management', () => {
  it('lists readers with counts and status', async () => {
    renderAt('/admin/readers')
    const row = await rowOf('anon@example.com')
    expect(within(row).getByText('匿名读者')).toBeInTheDocument()
    expect(within(row).getByText('正常')).toBeInTheDocument()
    expect(within(await rowOf('old@example.com')).getByText('已拉黑')).toBeInTheDocument()
    // 没有邮箱的旧读者不能按邮箱拉黑
    expect(within(await rowOf('user:42')).getByRole('button', { name: '拉黑' })).toBeDisabled()
  })

  it('blocks a reader with a reason', async () => {
    renderAt('/admin/readers')
    fireEvent.click(within(await rowOf('anon@example.com')).getByRole('button', { name: '拉黑' }))
    fireEvent.change(await screen.findByLabelText('拉黑原因'), { target: { value: '广告' } })
    const dialog = screen.getByRole('dialog')
    fireEvent.click(within(dialog).getByRole('button', { name: /拉\s*黑/ }))

    await waitFor(() =>
      expect(calls.find((c) => route(c) === 'POST /api/admin/readers/anon%40example.com/block')?.body).toEqual({
        reason: '广告',
      }),
    )
    expect(await screen.findByText('已拉黑', { selector: '.ant-message *' })).toBeInTheDocument()
    await waitFor(() => expect(calls.filter((c) => route(c) === 'GET /api/admin/readers')).toHaveLength(2))
  })

  it('unblocks a blocked reader', async () => {
    renderAt('/admin/readers')
    fireEvent.click(within(await rowOf('old@example.com')).getByRole('button', { name: '解除' }))
    fireEvent.click(await screen.findByRole('button', { name: /确\s*定/ }))
    await waitFor(() => expect(calls.some((c) => route(c) === 'DELETE /api/admin/readers/old%40example.com/block')).toBe(true))
    expect(await screen.findByText('已解除')).toBeInTheDocument()
  })

  it('filters by keyword and blocked status', async () => {
    renderAt('/admin/readers')
    await rowOf('anon@example.com')
    const search = screen.getByPlaceholderText('搜索邮箱或昵称')
    fireEvent.change(search, { target: { value: 'anon' } })
    fireEvent.keyDown(search, { key: 'Enter', code: 'Enter', keyCode: 13 })
    await waitFor(() => expect(calls.some((c) => c.url.includes('/api/admin/readers?') && c.url.includes('keyword=anon'))).toBe(true))
  })

  it('manages the IP blacklist in its own tab', async () => {
    renderAt('/admin/readers')
    fireEvent.click(await screen.findByRole('tab', { name: 'IP 黑名单' }))
    const row = await rowOf('203.0.113.7')
    fireEvent.click(within(row).getByRole('button', { name: /删\s*除/ }))
    fireEvent.click(await screen.findByRole('button', { name: /确\s*定/ }))
    await waitFor(() => expect(calls.some((c) => route(c) === 'DELETE /api/admin/readers/ip-blocks/3')).toBe(true))

    fireEvent.click(screen.getByRole('button', { name: /新增IP/ }))
    fireEvent.change(await screen.findByPlaceholderText('例如 203.0.113.7'), { target: { value: ' 198.51.100.1 ' } })
    fireEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: /确\s*定/ }))
    await waitFor(() =>
      expect(calls.find((c) => route(c) === 'POST /api/admin/readers/ip-blocks')?.body).toEqual({ ip: '198.51.100.1', reason: '' }),
    )
  })
})
