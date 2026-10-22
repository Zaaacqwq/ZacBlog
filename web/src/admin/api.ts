import { ApiError, api, request, type PageResult } from '../shared/api'

// 后台接口，字段与 docs/api.md 6.2、6.3 一致

export interface Captcha {
  captchaId: string
  image: string
}

export interface LoginBody {
  username: string
  password: string
  captchaId: string
  captchaCode: string
}

export interface Me {
  username: string
  nickname: string
  avatarUrl: string | null
}

export interface FileInfo {
  id: number
  name: string
  url: string
  size: number
  createdAt: string
}

export interface NamedItem {
  id: number
  name: string
  articleCount: number
}

export interface Article {
  id: number
  title: string
  summary: string
  coverUrl: string | null
  contentMarkdown?: string
  contentHtml?: string
  contentType?: 'markdown' | 'html'
  categoryId: number | null
  categoryName: string | null
  tags: { id: number; name: string }[]
  top: boolean
  essay: boolean
  status: 'published' | 'draft'
  views: number
  commentCount: number
  createdAt: string
  updatedAt: string
  files?: FileInfo[]
}

export interface ArticleBody {
  title: string
  summary: string
  coverUrl: string | null
  contentType: 'markdown' | 'html'
  contentMarkdown: string
  contentHtml: string
  categoryId: number | null
  tagIds: number[]
  top: boolean
  essay: boolean
  status: 'published' | 'draft'
  fileIds: number[]
}

export interface Notice {
  id: number
  title: string
  content: string
  status: 'normal' | 'closed'
  files: FileInfo[] | null
  createdAt: string
  updatedAt: string | null
}

export interface NoticeBody {
  title: string
  content: string
  status: 'normal' | 'closed'
  fileIds: number[]
}

export interface AdminComment {
  id: number
  parentId: number | null
  nickname: string
  website: string | null
  avatarUrl: string | null
  content: string
  likeCount: number
  fromAdmin: boolean
  replyToNickname: string | null
  createdAt: string
  email: string | null
  reader: string | null
  ip: string | null
  articleId: number | null
  articleTitle: string | null
}

export interface Reader {
  email: string
  nickname: string
  commentCount: number
  messageCount: number
  lastActiveAt: string
  blocked: boolean
}

export interface IpBlock {
  id: number
  ip: string
  reason: string | null
  createdAt: string
}

export interface KeywordBlock {
  id: number
  keyword: string
  reason: string | null
  createdAt: string
}

export interface Dashboard {
  articleCount: number
  essayCount: number
  draftCount: number
  commentCount: number
  messageCount: number
  totalViews: number
  last7Days: { date: string; articles: number; comments: number; messages: number }[]
  articlesByCategory: { name: string; count: number }[]
  articlesByTag: { name: string; count: number }[]
}

export type Query = Record<string, string | number | boolean | undefined | null>

/** 评论和留言接口结构相同，只有路径不同 */
export type CommentKind = 'comments' | 'messages'

export const adminApi = {
  captcha: () => api.get<Captcha>('/auth/captcha'),
  login: (body: LoginBody) => api.post<{ token: string; expiresAt: string }>('/auth/login', body),
  me: () => api.get<Me>('/admin/me'),
  changePassword: (oldPassword: string, newPassword: string) =>
    api.put<null>('/admin/profile/password', { oldPassword, newPassword }),

  dashboard: () => api.get<Dashboard>('/admin/dashboard'),

  articles: (q: Query) => api.get<PageResult<Article>>('/admin/articles', q),
  article: (id: number) => api.get<Article>(`/admin/articles/${id}`),
  createArticle: (body: ArticleBody) => api.post<Article>('/admin/articles', body),
  updateArticle: (id: number, body: ArticleBody) => api.put<Article>(`/admin/articles/${id}`, body),
  deleteArticle: (id: number) => api.delete<null>(`/admin/articles/${id}`),

  /** kind 为 categories 或 tags */
  named: (kind: 'categories' | 'tags') => api.get<NamedItem[]>(`/admin/${kind}`),
  createNamed: (kind: 'categories' | 'tags', name: string) => api.post<NamedItem>(`/admin/${kind}`, { name }),
  updateNamed: (kind: 'categories' | 'tags', id: number, name: string) =>
    api.put<NamedItem>(`/admin/${kind}/${id}`, { name }),
  deleteNamed: (kind: 'categories' | 'tags', id: number) => api.delete<null>(`/admin/${kind}/${id}`),

  notices: (q: Query) => api.get<PageResult<Notice>>('/admin/notices', q),
  notice: (id: number) => api.get<Notice>(`/admin/notices/${id}`),
  createNotice: (body: NoticeBody) => api.post<Notice>('/admin/notices', body),
  updateNotice: (id: number, body: NoticeBody) => api.put<Notice>(`/admin/notices/${id}`, body),
  deleteNotice: (id: number) => api.delete<null>(`/admin/notices/${id}`),

  files: (q: Query) => api.get<PageResult<FileInfo>>('/admin/files', q),
  uploadFile: (file: File) => {
    const form = new FormData()
    form.append('file', file)
    return request<FileInfo>('/admin/files', { method: 'POST', body: form })
  },
  deleteFile: (id: number) => api.delete<null>(`/admin/files/${id}`),

  comments: (kind: CommentKind, q: Query) => api.get<PageResult<AdminComment>>(`/admin/${kind}`, q),
  replyComment: (kind: CommentKind, id: number, content: string) =>
    api.post<AdminComment>(`/admin/${kind}/${id}/reply`, { content }),
  deleteComment: (kind: CommentKind, id: number) => api.delete<number>(`/admin/${kind}/${id}`),

  readers: (q: Query) => api.get<PageResult<Reader>>('/admin/readers', q),
  blockReader: (email: string, reason?: string) =>
    api.post<null>(`/admin/readers/${encodeURIComponent(email)}/block`, reason ? { reason } : undefined),
  unblockReader: (email: string) => api.delete<null>(`/admin/readers/${encodeURIComponent(email)}/block`),
  ipBlocks: (q: Query) => api.get<PageResult<IpBlock>>('/admin/readers/ip-blocks', q),
  addIpBlock: (ip: string, reason: string) => api.post<IpBlock>('/admin/readers/ip-blocks', { ip, reason }),
  deleteIpBlock: (id: number) => api.delete<null>(`/admin/readers/ip-blocks/${id}`),
  keywordBlocks: (q: Query) => api.get<PageResult<KeywordBlock>>('/admin/readers/keyword-blocks', q),
  addKeywordBlock: (keyword: string, reason: string) =>
    api.post<KeywordBlock>('/admin/readers/keyword-blocks', { keyword, reason }),
  deleteKeywordBlock: (id: number) => api.delete<null>(`/admin/readers/keyword-blocks/${id}`),
}

/** 把接口错误转成给人看的提示；429 带上重试秒数 */
export function errorText(err: unknown): string {
  if (err instanceof ApiError) {
    if (err.status === 429) {
      return err.retryAfter ? `操作太频繁，请 ${err.retryAfter} 秒后再试` : '操作太频繁，请稍后再试'
    }
    return err.message || `请求失败（${err.status}）`
  }
  return '网络错误，请稍后再试'
}

/** 存库的 /profile/... 在浏览器里要加 /api 前缀；外链原样使用 */
export function fileUrl(url: string | null | undefined): string {
  if (!url) return ''
  return url.startsWith('/profile/') ? `/api${url}` : url
}

export function formatTime(iso: string | null | undefined): string {
  if (!iso) return ''
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return iso
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

export function formatSize(size: number): string {
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}
