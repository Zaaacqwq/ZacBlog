import { useEffect, useState } from 'react'
import { publicApi, type Comment, type CommentTarget } from '../api'
import { formatDateTime, resolveFileUrl } from '../lib/content'
import { useApi } from '../lib/useApi'
import CommentForm from './CommentForm'
import Icon from './Icon'
import Pagination from './Pagination'

const PAGE_SIZE = 10
const LIKED_KEY = 'zacblog_liked'

function loadLiked(): Set<string> {
  try {
    return new Set(JSON.parse(localStorage.getItem(LIKED_KEY) ?? '[]') as string[])
  } catch {
    return new Set()
  }
}

interface Props {
  target: CommentTarget
  /** 旧链接 ?commentId= 带过来的评论，加载后滚动过去并高亮 */
  highlightId?: number | null
  /** 留言板把发表框放在列表上方的独立卡片里；文章页放在评论区内 */
  formPlacement?: 'separate' | 'inline'
}

export default function CommentSection({ target, highlightId, formPlacement = 'inline' }: Props) {
  const [pageNum, setPageNum] = useState(1)
  const [replying, setReplying] = useState<number | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  const [liked, setLiked] = useState(loadLiked)
  const [likeCounts, setLikeCounts] = useState<Record<number, number>>({})
  const targetKey = target.kind === 'article' ? `article-${target.articleId}` : 'messages'
  const list = useApi(`comments:${targetKey}:${pageNum}`, () => publicApi.comments(target, pageNum, PAGE_SIZE))
  const rows = list.data?.rows ?? []
  const total = list.data?.total ?? 0

  useEffect(() => {
    if (!highlightId || !list.data) return
    document.getElementById(`comment-${highlightId}`)?.scrollIntoView?.({ behavior: 'smooth', block: 'center' })
  }, [highlightId, list.data])

  function onPosted(created: Comment | null) {
    setReplying(null)
    setNotice(created ? '发表成功，谢谢！' : '已提交。')
    if (pageNum === 1) list.reload()
    else setPageNum(1)
  }

  async function toggleLike(c: Comment) {
    const key = `${target.kind}:${c.id}`
    const next = !liked.has(key)
    try {
      const count = await publicApi.like(target, c.id, next)
      setLikeCounts((m) => ({ ...m, [c.id]: count }))
      setLiked((prev) => {
        const s = new Set(prev)
        if (next) s.add(key)
        else s.delete(key)
        localStorage.setItem(LIKED_KEY, JSON.stringify([...s]))
        return s
      })
    } catch {
      // 点赞失败不打扰读者
    }
  }

  function likeButton(c: Comment) {
    const on = liked.has(`${target.kind}:${c.id}`)
    return (
      <button
        type="button"
        className={`like-btn${on ? ' liked' : ''}`}
        aria-pressed={on}
        aria-label={on ? '取消点赞' : '点赞'}
        onClick={() => toggleLike(c)}
      >
        <Icon name={on ? 'heartFill' : 'heart'} />
        {likeCounts[c.id] ?? c.likeCount}
      </button>
    )
  }

  function replyForm(c: Comment, floorId: number) {
    if (replying !== c.id) return null
    return (
      <CommentForm
        target={target}
        parentId={c.id}
        replyTo={c.nickname}
        submitLabel="Reply"
        onPosted={onPosted}
        onCancel={() => setReplying(null)}
        key={`reply-${floorId}-${c.id}`}
      />
    )
  }

  function author(c: Comment) {
    const name = c.website ? (
      <a href={c.website} target="_blank" rel="noopener noreferrer nofollow ugc">
        {c.nickname}
      </a>
    ) : (
      c.nickname
    )
    return (
      <>
        <strong className="comment-name">{name}</strong>
        {c.fromAdmin && <span className="badge">博主</span>}
      </>
    )
  }

  const form = (
    <CommentForm
      target={target}
      submitLabel={target.kind === 'message' ? 'Publish Message' : 'Publish Comment'}
      onPosted={onPosted}
    />
  )
  const offset = (pageNum - 1) * PAGE_SIZE

  return (
    <div className="comment-section">
      {formPlacement === 'separate' ? (
        <section className="card comment-composer">
          <ComposerHeader />
          {form}
        </section>
      ) : null}
      <section className="card comment-list-card">
        {formPlacement === 'inline' && (
          <div className="comment-composer inline">
            <h2 className="section-title">Comments</h2>
            <ComposerHeader />
            {form}
          </div>
        )}
        {notice && (
          <p className="form-success" role="status">
            {notice}
          </p>
        )}
        {list.error ? <p className="empty">评论加载失败，请稍后刷新。</p> : null}
        {!list.loading && !list.error && rows.length === 0 && <p className="empty">还没有人说话，来坐沙发吧。</p>}
        <ol className="comment-list">
          {rows.map((c, index) => (
            <li
              key={c.id}
              id={`comment-${c.id}`}
              className={`comment-floor${highlightId === c.id ? ' highlighted' : ''}`}
            >
              <div className="comment-head">
                <Avatar comment={c} />
                <div>
                  <div>{author(c)}</div>
                  <time className="comment-time">{formatDateTime(c.createdAt)}</time>
                </div>
                <span className="floor-no">#{total - offset - index}</span>
              </div>
              <p className="comment-content">{c.content}</p>
              <div className="comment-actions">
                {likeButton(c)}
                <button type="button" className="reply-btn" onClick={() => setReplying(replying === c.id ? null : c.id)}>
                  Reply
                </button>
              </div>
              {replyForm(c, c.id)}
              {c.children.length > 0 && (
                <ul className="comment-children">
                  {c.children.map((r) => (
                    <li key={r.id} id={`comment-${r.id}`} className={highlightId === r.id ? 'highlighted' : undefined}>
                      <div>
                        {author(r)}
                        {r.replyToNickname && r.parentId !== c.id && <span className="reply-to"> @{r.replyToNickname}</span>}
                        <span>: {r.content}</span>
                      </div>
                      <div className="comment-actions small">
                        <time className="comment-time">{formatDateTime(r.createdAt)}</time>
                        {likeButton(r)}
                        <button
                          type="button"
                          className="reply-btn"
                          onClick={() => setReplying(replying === r.id ? null : r.id)}
                        >
                          Reply
                        </button>
                      </div>
                      {replyForm(r, c.id)}
                    </li>
                  ))}
                </ul>
              )}
            </li>
          ))}
        </ol>
        <Pagination pageNum={pageNum} pageSize={PAGE_SIZE} total={total} onChange={setPageNum} />
      </section>
    </div>
  )
}

function ComposerHeader() {
  return (
    <div className="composer-head">
      <span className="avatar placeholder">
        <Icon name="user" />
      </span>
      <div>
        <div>Anonymous User</div>
        <small>Share a thought, quick note, or reply.</small>
      </div>
    </div>
  )
}

function Avatar({ comment }: { comment: Comment }) {
  if (comment.avatarUrl) return <img className="avatar" src={resolveFileUrl(comment.avatarUrl)} alt="" />
  return (
    <span className="avatar placeholder">
      <Icon name="user" />
    </span>
  )
}
