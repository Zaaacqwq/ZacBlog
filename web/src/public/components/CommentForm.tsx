import { useState, type FormEvent } from 'react'
import { publicApi, type Comment, type CommentTarget } from '../api'
import { describeError } from '../lib/errors'

const PROFILE_KEY = 'zacblog_commenter'
const MAX_CONTENT = 255

interface Profile {
  nickname: string
  email: string
  website: string
}

function loadProfile(): Profile {
  try {
    const saved = JSON.parse(localStorage.getItem(PROFILE_KEY) ?? 'null') as Partial<Profile> | null
    return { nickname: saved?.nickname ?? '', email: saved?.email ?? '', website: saved?.website ?? '' }
  } catch {
    return { nickname: '', email: '', website: '' }
  }
}

interface Props {
  target: CommentTarget
  parentId?: number | null
  replyTo?: string | null
  submitLabel?: string
  placeholder?: string
  onPosted: (comment: Comment | null) => void
  onCancel?: () => void
}

export default function CommentForm({
  target,
  parentId = null,
  replyTo,
  submitLabel = 'Publish',
  placeholder = 'Enter your message',
  onPosted,
  onCancel,
}: Props) {
  const [profile, setProfile] = useState(loadProfile)
  const [content, setContent] = useState('')
  const [honeypot, setHoneypot] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const update = (key: keyof Profile) => (value: string) => setProfile((p) => ({ ...p, [key]: value }))

  async function onSubmit(event: FormEvent) {
    event.preventDefault()
    if (submitting) return
    setError(null)
    const nickname = profile.nickname.trim()
    const email = profile.email.trim()
    const website = profile.website.trim()
    if (!nickname || !email || !content.trim()) {
      setError('昵称、邮箱和内容都要填写。')
      return
    }
    setSubmitting(true)
    try {
      const created = await publicApi.postComment(target, {
        parentId,
        nickname,
        email,
        website: website || null,
        content: content.trim(),
        homepage: honeypot,
      })
      localStorage.setItem(PROFILE_KEY, JSON.stringify({ nickname, email, website }))
      setContent('')
      onPosted(created)
    } catch (e) {
      setError(describeError(e))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <form className="comment-form" onSubmit={onSubmit} noValidate>
      <div className="comment-form-fields">
        <input
          aria-label="昵称"
          placeholder="Nickname *"
          maxLength={20}
          required
          value={profile.nickname}
          onChange={(e) => update('nickname')(e.target.value)}
        />
        <input
          aria-label="邮箱"
          type="email"
          placeholder="Email *（不会公开）"
          required
          value={profile.email}
          onChange={(e) => update('email')(e.target.value)}
        />
        <input
          aria-label="网站"
          type="url"
          placeholder="Website (optional)"
          value={profile.website}
          onChange={(e) => update('website')(e.target.value)}
        />
      </div>
      {/* 蜜罐：普通文本框，用 CSS 移出可视区域；真人看不到也不会填，机器人会填。不能用 type=hidden */}
      <div className="hp-field" aria-hidden="true" style={HONEYPOT_STYLE}>
        <label>
          Homepage
          <input
            type="text"
            name="homepage"
            tabIndex={-1}
            autoComplete="off"
            value={honeypot}
            onChange={(e) => setHoneypot(e.target.value)}
          />
        </label>
      </div>
      <div className="comment-textarea">
        <textarea
          aria-label="内容"
          placeholder={replyTo ? `回复 @${replyTo}` : placeholder}
          maxLength={MAX_CONTENT}
          required
          value={content}
          onChange={(e) => setContent(e.target.value)}
        />
        <span className="counter">
          {content.length}/{MAX_CONTENT}
        </span>
      </div>
      {error && (
        <p className="form-error" role="alert">
          {error}
        </p>
      )}
      <div className="comment-form-actions">
        <span className="hint">Keep it concise and readable.</span>
        <div>
          {onCancel && (
            <button type="button" className="btn ghost" onClick={onCancel}>
              Cancel
            </button>
          )}
          <button type="submit" className="btn" disabled={submitting}>
            {submitting ? 'Publishing…' : submitLabel}
          </button>
        </div>
      </div>
    </form>
  )
}

// 内联样式，保证即使样式表没加载，蜜罐也不可见
const HONEYPOT_STYLE = {
  position: 'absolute',
  left: '-10000px',
  top: 'auto',
  width: '1px',
  height: '1px',
  overflow: 'hidden',
  opacity: 0,
} as const
