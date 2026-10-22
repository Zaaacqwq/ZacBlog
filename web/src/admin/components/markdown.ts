import DOMPurify from 'dompurify'
import { marked } from 'marked'

/** 后台预览用：Markdown 转 HTML 并过滤脚本；/profile/ 图片补上 /api 前缀 */
export function renderMarkdown(md: string): string {
  return sanitize(marked.parse(md, { async: false, gfm: true }))
}

export function sanitize(html: string): string {
  return DOMPurify.sanitize(html).replace(/(src|href)="\/profile\//g, '$1="/api/profile/')
}
