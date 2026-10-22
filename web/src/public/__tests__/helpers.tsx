import { render } from '@testing-library/react'
import { RouterProvider, createMemoryRouter } from 'react-router-dom'
import { routes } from '../../routes'

export function ok(data: unknown, status = 200) {
  return new Response(JSON.stringify({ code: status, msg: status === 200 ? '操作成功' : 'error', data }), { status })
}

export function fail(status: number, msg: string) {
  return new Response(JSON.stringify({ code: status, msg, data: null }), { status })
}

type Handler = (url: URL, init?: RequestInit) => Response | undefined

/** 按 “方法 路径” 返回假数据；没有匹配的请求一律返回 data: null */
export function mockApi(handlers: Record<string, Handler | unknown> = {}) {
  const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = new URL(String(input), 'http://localhost')
    const method = (init?.method ?? 'GET').toUpperCase()
    const handler = handlers[`${method} ${url.pathname}`]
    if (typeof handler === 'function') return (handler as Handler)(url, init) ?? ok(null)
    if (handler !== undefined) return ok(handler)
    return ok(null)
  })
  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}

export function renderAt(path: string) {
  const router = createMemoryRouter(routes, { initialEntries: [path] })
  render(<RouterProvider router={router} />)
  return router
}
