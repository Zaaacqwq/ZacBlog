import { act, fireEvent, screen, waitFor } from '@testing-library/react'
import { clearToken, setToken } from '../../shared/token'
import { mockApi, renderAt, route, type Call } from './helpers'

const article = (id: number, title: string) => ({
  id,
  title,
  summary: '',
  coverUrl: null,
  contentType: 'markdown',
  contentMarkdown: `# ${title}`,
  contentHtml: '',
  categoryId: null,
  categoryName: null,
  tags: [],
  top: false,
  essay: false,
  status: 'published',
  views: 0,
  commentCount: 0,
  createdAt: '2026-10-01T00:00:00+08:00',
  updatedAt: '2026-10-01T00:00:00+08:00',
  files: [],
})

function deferred<T>() {
  let resolve!: (v: T) => void
  const promise = new Promise<T>((r) => (resolve = r))
  return { promise, resolve }
}

type Reply = { status?: number; data?: unknown; msg?: string }

let calls: Call[]
let pending: Record<string, ReturnType<typeof deferred<Reply>>>

beforeEach(() => {
  setToken('test-only-token')
  pending = { '1': deferred<Reply>(), '2': deferred<Reply>() }
  calls = mockApi((c) => {
    const r = route(c)
    if (r === 'GET /api/admin/me') return { data: { username: 'admin', nickname: 'Zac', avatarUrl: null } }
    if (r === 'GET /api/admin/categories' || r === 'GET /api/admin/tags') return { data: [] }
    const m = r.match(/^GET \/api\/admin\/articles\/(\d+)$/)
    if (m) return pending[m[1]].promise
    if (r.startsWith('PUT /api/admin/articles/')) return { data: article(Number(r.split('/').pop()), 'saved') }
  })
})

afterEach(() => {
  vi.unstubAllGlobals()
  clearToken()
})

const publishButton = () => screen.getByRole('button', { name: /发\s*布/ })
const titleInput = () => screen.getByPlaceholderText('文章标题') as HTMLInputElement

describe('article editor', () => {
  it('ignores a late response for the previous article and saves to the current one', async () => {
    const router = renderAt('/admin/articles/1/edit')
    await screen.findByPlaceholderText('文章标题')
    expect(publishButton()).toBeDisabled()

    // 通过标签页切到文章 2，文章 1 的响应还没回来
    await act(() => router.navigate('/admin/articles/2/edit'))
    expect(publishButton()).toBeDisabled()
    // 文章 2 先返回，文章 1 后返回（乱序）
    await act(async () => pending['2'].resolve({ data: article(2, '文章B') }))
    await act(async () => pending['1'].resolve({ data: article(1, '文章A') }))

    await waitFor(() => expect(titleInput().value).toBe('文章B'))
    await waitFor(() => expect(publishButton()).toBeEnabled())
    fireEvent.click(publishButton())
    await waitFor(() => expect(calls.some((c) => route(c).startsWith('PUT '))).toBe(true))
    const put = calls.filter((c) => route(c).startsWith('PUT '))
    expect(put).toHaveLength(1)
    expect(route(put[0])).toBe('PUT /api/admin/articles/2')
    expect(put[0].body).toMatchObject({ title: '文章B', contentMarkdown: '# 文章B' })
  })

  it('does not allow saving while loading or after the load failed', async () => {
    renderAt('/admin/articles/1/edit')
    await screen.findByPlaceholderText('文章标题')
    fireEvent.click(publishButton())
    await act(async () => pending['1'].resolve({ status: 404, msg: '资源不存在' }))
    expect(await screen.findByText('资源不存在')).toBeInTheDocument()
    expect(publishButton()).toBeDisabled()
    expect(screen.getByRole('button', { name: /暂\s*存/ })).toBeDisabled()
    expect(calls.some((c) => route(c).startsWith('PUT '))).toBe(false)
  })
})
