import { ApiError, buildUrl, request } from './api'
import { clearToken, setToken } from './token'

describe('api client', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
    clearToken()
  })

  it('prefixes every path with /api and drops empty query values', () => {
    expect(buildUrl('/public/articles', { pageNum: 1, pageSize: 10, keyword: '' })).toBe(
      '/api/public/articles?pageNum=1&pageSize=10',
    )
  })

  it('unwraps data and sends the bearer token', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ code: 200, msg: 'ok', data: { id: 1 } }), { status: 200 }),
    )
    vi.stubGlobal('fetch', fetchMock)
    setToken('t0k')

    await expect(request('/admin/articles/1')).resolves.toEqual({ id: 1 })
    const [url, init] = fetchMock.mock.calls[0]
    expect(url).toBe('/api/admin/articles/1')
    expect(new Headers(init.headers).get('Authorization')).toBe('Bearer t0k')
  })

  it('throws ApiError with the server code on failure', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(JSON.stringify({ code: 429, msg: '操作太频繁', data: null }), { status: 429 }),
      ),
    )
    await expect(request('/public/comments')).rejects.toMatchObject({
      name: 'ApiError',
      status: 429,
      code: 429,
    } satisfies Partial<ApiError>)
  })
})
