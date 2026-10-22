import { api, type PageResult } from '../shared/api'

// 前台 /public/** 接口，字段见 docs/api.md 第 5、6.1 节

export interface Article {
  id: number
  title: string
  summary: string
  coverUrl: string | null
  contentMarkdown?: string | null
  contentHtml?: string | null
  categoryId: number | null
  categoryName: string | null
  tags: { id: number; name: string }[]
  top: boolean
  essay: boolean
  views: number
  commentCount: number
  createdAt: string
  updatedAt: string
  files?: FileInfo[]
}

export interface Category {
  id: number
  name: string
  articleCount: number
}

export type Tag = Category

export interface Comment {
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
  children: Comment[]
}

export interface Notice {
  id: number
  title: string
  content: string
  createdAt: string
}

export interface FileInfo {
  id: number
  name: string
  url: string
  size: number
  createdAt: string
}

export interface SiteStats {
  articleCount: number
  categoryCount: number
  tagCount: number
  commentCount: number
  messageCount: number
  totalViews: number
}

export interface ArticleQuery {
  pageNum?: number
  pageSize?: number
  categoryId?: number
  tagId?: number
  keyword?: string
  essay?: boolean
}

export interface CommentPayload {
  parentId: number | null
  nickname: string
  email: string
  website: string | null
  content: string
  /** 蜜罐：真人永远留空 */
  homepage: string
}

/** 评论区的两种来源：文章评论、留言板 */
export type CommentTarget = { kind: 'article'; articleId: number } | { kind: 'message' }

export const publicApi = {
  articles: (query: ArticleQuery = {}) => api.get<PageResult<Article>>('/public/articles', { ...query }),
  recommend: (limit = 5) => api.get<Article[]>('/public/articles/recommend', { limit }),
  article: (id: number) => api.get<Article>(`/public/articles/${id}`),
  addView: (id: number) => api.post<number>(`/public/articles/${id}/views`),
  categories: () => api.get<Category[]>('/public/categories'),
  tags: () => api.get<Tag[]>('/public/tags'),
  site: () => api.get<SiteStats>('/public/site'),
  latestNotice: () => api.get<Notice | null>('/public/notices/latest'),
  about: () => api.get<{ content: string }>('/public/about'),

  comments: (target: CommentTarget, pageNum: number, pageSize: number) =>
    api.get<PageResult<Comment>>(commentsPath(target), { pageNum, pageSize }),
  postComment: (target: CommentTarget, body: CommentPayload) =>
    api.post<Comment | null>(commentsPath(target), body),
  like: (target: CommentTarget, id: number, liked: boolean) => {
    const path = `/public/${target.kind === 'article' ? 'comments' : 'messages'}/${id}/like`
    return liked ? api.post<number>(path) : api.delete<number>(path)
  },
}

function commentsPath(target: CommentTarget): string {
  return target.kind === 'article' ? `/public/articles/${target.articleId}/comments` : '/public/messages'
}
