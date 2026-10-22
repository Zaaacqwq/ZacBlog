import { useEffect, useMemo } from 'react'
import { Link, useParams, useSearchParams } from 'react-router-dom'
import { ApiError } from '../../shared/api'
import { publicApi } from '../api'
import CommentSection from '../components/CommentSection'
import Icon from '../components/Icon'
import { TwoColumn } from '../components/PublicLayout'
import Sidebar from '../components/Sidebar'
import { formatDateTime, formatSize, renderContent, resolveFileUrl } from '../lib/content'
import { useApi } from '../lib/useApi'
import NotFoundPage from './NotFoundPage'

export default function PostPage() {
  const { id: rawId } = useParams()
  const [params] = useSearchParams()
  const id = Number(rawId)
  const valid = Number.isInteger(id) && id > 0
  const commentId = Number(params.get('commentId')) || null

  const { data, error, loading } = useApi(valid ? `article:${id}` : null, () => publicApi.article(id))
  const article = data?.id === id ? data : undefined
  const html = useMemo(
    () => (article ? renderContent(article.contentMarkdown, article.contentHtml) : ''),
    [article],
  )

  useEffect(() => {
    if (valid) publicApi.addView(id).catch(() => undefined)
  }, [id, valid])

  useEffect(() => {
    if (article) document.title = `${article.title} · ZacBlog`
    return () => {
      document.title = 'ZacBlog'
    }
  }, [article])

  if (!valid || (error instanceof ApiError && error.status === 404)) return <NotFoundPage />

  return (
    <div className="page">
      <TwoColumn side={<Sidebar />}>
        <article className="card post-card">
          {error ? (
            <p className="empty">文章加载失败，请稍后刷新。</p>
          ) : loading || !article ? (
            <p className="empty">Loading…</p>
          ) : (
            <>
              <header className="post-header">
                <div className="eyebrow">{article.categoryName ?? (article.essay ? 'Note' : 'Post')}</div>
                <h1 className="post-title">{article.title}</h1>
                <div className="meta">
                  <span>
                    <Icon name="calendar" /> {formatDateTime(article.createdAt)}
                  </span>
                  <span>
                    <Icon name="eye" /> {article.views} views
                  </span>
                </div>
                {article.tags.length > 0 && (
                  <div className="tag-row">
                    {article.tags.map((t) => (
                      <Link key={t.id} className="pill" to={`/?tagId=${t.id}`}>
                        {t.name}
                      </Link>
                    ))}
                  </div>
                )}
              </header>
              <div className="markdown-body" data-testid="post-content" dangerouslySetInnerHTML={{ __html: html }} />
              {article.files && article.files.length > 0 && (
                <section className="attachments">
                  <h2 className="section-title">Attachments</h2>
                  <ul>
                    {article.files.map((f) => (
                      <li key={f.id}>
                        <a href={resolveFileUrl(f.url)} target="_blank" rel="noopener noreferrer" download={f.name}>
                          <Icon name="file" /> {f.name}
                        </a>
                        <small>{formatSize(f.size)}</small>
                      </li>
                    ))}
                  </ul>
                </section>
              )}
            </>
          )}
        </article>
        {article && <CommentSection target={{ kind: 'article', articleId: article.id }} highlightId={commentId} />}
      </TwoColumn>
    </div>
  )
}
