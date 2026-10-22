import { useMemo } from 'react'
import { publicApi } from '../api'
import { TwoColumn } from '../components/PublicLayout'
import Sidebar from '../components/Sidebar'
import { renderContent } from '../lib/content'
import { useApi } from '../lib/useApi'

/** About（旧 cmsDoucument.vue，内容是 server 的 about.md） */
export default function AboutPage() {
  const { data, error } = useApi('about', publicApi.about)
  const html = useMemo(() => (data ? renderContent(data.content) : ''), [data])
  return (
    <div className="page">
      <TwoColumn side={<Sidebar />}>
        <article className="card post-card">
          {error ? (
            <p className="empty">加载失败，请稍后刷新。</p>
          ) : (
            <div className="markdown-body" dangerouslySetInnerHTML={{ __html: html }} />
          )}
        </article>
      </TwoColumn>
    </div>
  )
}
