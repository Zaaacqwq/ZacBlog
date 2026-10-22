import { clearToken, getToken } from './token'

/** 所有请求都发到 /api/...；dev 由 vite 代理、线上由 nginx 去掉 /api 前缀后转给 server。约定见 docs/api.md */
export const API_BASE = '/api'

/** 统一响应格式，见 docs/api.md */
export interface ApiResponse<T> {
  code: number
  msg: string
  data: T
}

export interface PageResult<T> {
  rows: T[]
  total: number
}

export class ApiError extends Error {
  readonly status: number
  readonly code: number
  /** 429 时响应头 Retry-After 的秒数 */
  readonly retryAfter: number | null

  constructor(status: number, code: number, msg: string, retryAfter: number | null = null) {
    super(msg)
    this.name = 'ApiError'
    this.status = status
    this.code = code
    this.retryAfter = retryAfter
  }
}

/** 401 时在 window 上派发此事件，后台布局监听后跳回登录页 */
export const UNAUTHORIZED_EVENT = 'zacblog:unauthorized'

export interface RequestOptions extends Omit<RequestInit, 'body'> {
  query?: Record<string, string | number | boolean | undefined | null>
  body?: unknown
}

export function buildUrl(path: string, query?: RequestOptions['query']): string {
  const url = API_BASE + (path.startsWith('/') ? path : `/${path}`)
  if (!query) return url
  const params = new URLSearchParams()
  for (const [key, value] of Object.entries(query)) {
    if (value !== undefined && value !== null && value !== '') params.append(key, String(value))
  }
  const qs = params.toString()
  return qs ? `${url}?${qs}` : url
}

export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { query, body, headers, ...init } = options
  const finalHeaders = new Headers(headers)
  const token = getToken()
  if (token) finalHeaders.set('Authorization', `Bearer ${token}`)

  let payload: BodyInit | undefined
  if (body instanceof FormData) {
    payload = body
  } else if (body !== undefined) {
    finalHeaders.set('Content-Type', 'application/json')
    payload = JSON.stringify(body)
  }

  const res = await fetch(buildUrl(path, query), { ...init, headers: finalHeaders, body: payload })
  const json = (await res.json().catch(() => null)) as ApiResponse<T> | null

  if (!res.ok || !json || json.code !== 200) {
    if (res.status === 401) {
      clearToken()
      window.dispatchEvent(new Event(UNAUTHORIZED_EVENT))
    }
    const retryAfter = Number(res.headers.get('Retry-After'))
    throw new ApiError(
      res.status,
      json?.code ?? res.status,
      json?.msg ?? res.statusText,
      Number.isFinite(retryAfter) && retryAfter > 0 ? retryAfter : null,
    )
  }
  return json.data
}

export const api = {
  get: <T>(path: string, query?: RequestOptions['query']) => request<T>(path, { method: 'GET', query }),
  post: <T>(path: string, body?: unknown) => request<T>(path, { method: 'POST', body }),
  put: <T>(path: string, body?: unknown) => request<T>(path, { method: 'PUT', body }),
  delete: <T>(path: string) => request<T>(path, { method: 'DELETE' }),
}
