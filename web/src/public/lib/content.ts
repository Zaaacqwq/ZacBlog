import DOMPurify from 'dompurify'
import { marked } from 'marked'
import { API_BASE } from '../../shared/api'

/**
 * 图片和附件地址：根路径 /profile/... 走 /api 代理；http(s) 外链（包括旧的
 * https://api.zaaac.vip/profile/...）原样使用，不改写。见 docs/api.md 第 5 节。
 */
export function resolveFileUrl(url: string | null | undefined): string {
  if (!url) return ''
  return url.startsWith('/profile/') ? API_BASE + url : url
}

/** 正文：优先 Markdown，旧文章只有 HTML 时用 HTML；统一经过 DOMPurify 过滤 */
export function renderContent(markdown?: string | null, html?: string | null): string {
  const raw = markdown?.trim() ? (marked.parse(markdown, { async: false, gfm: true }) as string) : (html ?? '')
  const clean = DOMPurify.sanitize(raw, { ADD_ATTR: ['target'] })
  // 只给根路径 /profile/ 补 /api 前缀，绝对地址不动
  return clean.replace(/(\s(?:src|href)=")\/profile\//g, `$1${API_BASE}/profile/`)
}

const dateTimeFormat = new Intl.DateTimeFormat('sv-SE', {
  timeZone: 'Asia/Shanghai',
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
  second: '2-digit',
  hour12: false,
})

/** 2026-07-07T06:02:03Z → 2026-07-07 14:02:03（按博客所在时区显示，和旧站一致） */
export function formatDateTime(iso: string | null | undefined): string {
  if (!iso) return ''
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) return iso
  return dateTimeFormat.format(date)
}

export function formatSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}

export function stripHtml(html: string): string {
  return DOMPurify.sanitize(html, { ALLOWED_TAGS: [] }).trim()
}
