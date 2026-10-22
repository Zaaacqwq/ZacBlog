import { useState } from 'react'
import { Link } from 'react-router-dom'
import { publicApi } from '../api'
import Pagination from '../components/Pagination'
import { TwoColumn } from '../components/PublicLayout'
import Sidebar from '../components/Sidebar'
import { formatDateTime } from '../lib/content'
import { useApi } from '../lib/useApi'

const PAGE_SIZE = 10

/** 随笔：时间线（旧 cmsEssay.vue） */
export default function NotesPage() {
  const [pageNum, setPageNum] = useState(1)
  const list = useApi(`notes:${pageNum}`, () => publicApi.articles({ essay: true, pageNum, pageSize: PAGE_SIZE }))
  const rows = list.data?.rows ?? []

  return (
    <div className="page">
      <TwoColumn side={<Sidebar />}>
        {list.error ? <p className="card empty">随笔加载失败，请稍后刷新。</p> : null}
        {!list.loading && !list.error && rows.length === 0 && <p className="card empty">还没有随笔。</p>}
        <ol className="timeline">
          {rows.map((a) => (
            <li key={a.id} className="timeline-item">
              <time className="timeline-stamp">{formatDateTime(a.createdAt)}</time>
              <Link to={`/posts/${a.id}`} className="card note-card">
                <div className="note-date">{formatDateTime(a.createdAt)}</div>
                <h3>{a.title}</h3>
                {a.summary && <p>{a.summary}</p>}
              </Link>
            </li>
          ))}
        </ol>
        <Pagination pageNum={pageNum} pageSize={PAGE_SIZE} total={list.data?.total ?? 0} onChange={setPageNum} />
      </TwoColumn>
    </div>
  )
}
