import type { ReactNode } from 'react'
import avatar from '../assets/avatar.png'
import { publicApi } from '../api'
import { stripHtml } from '../lib/content'
import { useApi } from '../lib/useApi'
import Icon, { type IconName } from './Icon'

const SOCIAL: { href: string; label: string; icon: IconName }[] = [
  { href: 'https://github.com/Zaaacqwq', label: 'GitHub', icon: 'github' },
  { href: 'https://portfolio.zaaac.vip/', label: 'Portfolio', icon: 'user' },
  { href: 'https://www.instagram.com/zaaacqwq', label: 'Instagram', icon: 'instagram' },
  { href: 'https://www.linkedin.com/in/zaaac', label: 'LinkedIn', icon: 'linkedin' },
]

/** 右侧栏：博主卡片、站点说明、最新公告、站点数据；children 追加在后面（首页的分类、标签、推荐） */
export default function Sidebar({ children }: { children?: ReactNode }) {
  const site = useApi('site', publicApi.site).data
  const notice = useApi('notice', publicApi.latestNotice)
  const num = (n: number | undefined) => (n === undefined ? '–' : n)

  return (
    <aside className="sidebar">
      <section className="card side-card profile-card">
        <div className="eyebrow">Editor</div>
        <img className="profile-avatar" src={avatar} alt="Zac's Avatar" />
        <h3>Zac</h3>
        <p className="muted">Building software, writing notes, shipping experiments.</p>
        <div className="stats-grid">
          <div className="stat-box">
            <strong>{num(site?.articleCount)}</strong>
            <span>Posts</span>
          </div>
          <div className="stat-box">
            <strong>{num(site?.totalViews)}</strong>
            <span>Views</span>
          </div>
        </div>
        <div className="social-links">
          {SOCIAL.map((s) => (
            <a key={s.label} href={s.href} target="_blank" rel="noopener noreferrer" aria-label={s.label}>
              <Icon name={s.icon} />
            </a>
          ))}
        </div>
      </section>
      <section className="card side-card">
        <div className="eyebrow">About This Site</div>
        <p className="muted">
          A personal corner for engineering notes, longer essays, experiments, and the occasional public draft.
        </p>
      </section>
      <section className="card side-card">
        <div className="eyebrow">Latest Notice</div>
        <p className="muted">
          {notice.loading ? 'Loading latest announcement...' : notice.data ? stripHtml(notice.data.content) || notice.data.title : 'No notice yet.'}
        </p>
      </section>
      <section className="card side-card">
        <div className="eyebrow">Site Numbers</div>
        <dl className="stat-rows">
          <div>
            <dt>Articles</dt>
            <dd>{num(site?.articleCount)}</dd>
          </div>
          <div>
            <dt>Views</dt>
            <dd>{num(site?.totalViews)}</dd>
          </div>
          <div>
            <dt>Comments</dt>
            <dd>{num(site?.commentCount)}</dd>
          </div>
          <div>
            <dt>Messages</dt>
            <dd>{num(site?.messageCount)}</dd>
          </div>
        </dl>
      </section>
      {children}
    </aside>
  )
}
