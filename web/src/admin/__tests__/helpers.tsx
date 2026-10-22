import { render } from '@testing-library/react'
import { RouterProvider, createMemoryRouter } from 'react-router-dom'
import { routes } from '../../routes'

export interface Call {
  method: string
  url: string
  body: unknown
}

type Reply = { status?: number; data?: unknown; msg?: string; headers?: Record<string, string> } | undefined
type Handler = (call: Call) => Reply | Promise<Reply>

/** 按 “METHOD /path” 匹配的假 fetch；未匹配的请求返回 404 */
export function mockApi(handler: Handler) {
  const calls: Call[] = []
  const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input)
    const body = typeof init?.body === 'string' ? JSON.parse(init.body) : init?.body
    const call = { method: init?.method ?? 'GET', url, body }
    calls.push(call)
    const res = await handler(call)
    const status = res?.status ?? (res ? 200 : 404)
    return new Response(JSON.stringify({ code: status, msg: res?.msg ?? (status === 200 ? '操作成功' : '出错了'), data: res?.data ?? null }), {
      status,
      headers: res?.headers,
    })
  })
  vi.stubGlobal('fetch', fetchMock)
  return calls
}

export function renderAt(path: string) {
  const router = createMemoryRouter(routes, { initialEntries: [path] })
  render(<RouterProvider router={router} />)
  return router
}

export const route = (call: Call) => `${call.method} ${call.url.replace(/\?.*$/, '')}`
