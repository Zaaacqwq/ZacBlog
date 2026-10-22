import {
  CommentOutlined,
  EditOutlined,
  EyeOutlined,
  FileTextOutlined,
  MessageOutlined,
  ReadOutlined,
} from '@ant-design/icons'
import { App, Col, Empty, Row, Spin } from 'antd'
import { useEffect, useState, type ReactNode } from 'react'
import { adminApi, errorText, type Dashboard } from '../api'

const SERIES = [
  { key: 'articles', name: '文章', color: '#40c9c6' },
  { key: 'comments', name: '评论', color: '#36a3f7' },
  { key: 'messages', name: '留言', color: '#f4516c' },
] as const

/** 最近 7 天新增的折线图（对应旧后台 lineChart） */
function LineChart({ data }: { data: Dashboard['last7Days'] }) {
  const w = 640
  const h = 260
  const pad = { l: 36, r: 16, t: 12, b: 28 }
  const max = Math.max(1, ...data.flatMap((d) => [d.articles, d.comments, d.messages]))
  const x = (i: number) => pad.l + (data.length <= 1 ? 0 : (i * (w - pad.l - pad.r)) / (data.length - 1))
  const y = (v: number) => h - pad.b - (v / max) * (h - pad.t - pad.b)
  const ticks = Array.from(new Set([0, Math.round(max / 2), max]))
  return (
    <>
      <div className="chart-legend">
        {SERIES.map((s) => (
          <span key={s.key}>
            <i style={{ background: s.color }} />
            {s.name}
          </span>
        ))}
      </div>
      <svg viewBox={`0 0 ${w} ${h}`} width="100%" role="img" aria-label="最近 7 天新增">
        {ticks.map((t) => (
          <g key={t}>
            <line x1={pad.l} x2={w - pad.r} y1={y(t)} y2={y(t)} stroke="#e8ebf0" />
            <text x={pad.l - 8} y={y(t) + 4} fontSize="11" textAnchor="end" fill="#8a94a6">
              {t}
            </text>
          </g>
        ))}
        {data.map((d, i) => (
          <text key={d.date} x={x(i)} y={h - 8} fontSize="11" textAnchor="middle" fill="#8a94a6">
            {d.date.slice(5)}
          </text>
        ))}
        {SERIES.map((s) => (
          <g key={s.key}>
            <polyline
              fill="none"
              stroke={s.color}
              strokeWidth="2"
              points={data.map((d, i) => `${x(i)},${y(d[s.key])}`).join(' ')}
            />
            {data.map((d, i) => (
              <circle key={d.date} cx={x(i)} cy={y(d[s.key])} r="3" fill={s.color}>
                <title>{`${d.date} ${s.name} ${d[s.key]}`}</title>
              </circle>
            ))}
          </g>
        ))}
      </svg>
    </>
  )
}

/** 横向条形图（对应旧后台 pieChart / tagChart） */
function BarList({ data, color }: { data: { name: string; count: number }[]; color: string }) {
  if (data.length === 0) return <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} />
  const max = Math.max(1, ...data.map((d) => d.count))
  return (
    <div>
      {data.slice(0, 10).map((d) => (
        <div key={d.name} style={{ display: 'flex', alignItems: 'center', gap: 8, margin: '6px 0', fontSize: 13 }}>
          <span style={{ width: 80, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
            {d.name}
          </span>
          <div style={{ flex: 1, background: '#eef1f5', borderRadius: 4, height: 14 }}>
            <div style={{ width: `${(d.count / max) * 100}%`, background: color, height: '100%', borderRadius: 4 }} />
          </div>
          <span style={{ width: 28, textAlign: 'right' }}>{d.count}</span>
        </div>
      ))}
    </div>
  )
}

function Card({ icon, color, label, value }: { icon: ReactNode; color: string; label: string; value: number }) {
  return (
    <div className="card-panel">
      <span className="card-panel-icon" style={{ color }}>
        {icon}
      </span>
      <div className="card-panel-text">
        {label}
        <span className="card-panel-num">{value.toLocaleString()}</span>
      </div>
    </div>
  )
}

export default function DashboardPage() {
  const { message } = App.useApp()
  const [data, setData] = useState<Dashboard | null>(null)

  useEffect(() => {
    adminApi.dashboard().then(setData, (err) => message.error(errorText(err)))
  }, [message])

  if (!data) return <Spin style={{ display: 'block', margin: '80px auto' }} />

  const cards = [
    { icon: <FileTextOutlined />, color: '#40c9c6', label: '文章', value: data.articleCount },
    { icon: <ReadOutlined />, color: '#b37feb', label: '随笔', value: data.essayCount },
    { icon: <EditOutlined />, color: '#fec171', label: '草稿', value: data.draftCount },
    { icon: <CommentOutlined />, color: '#36a3f7', label: '评论', value: data.commentCount },
    { icon: <MessageOutlined />, color: '#f4516c', label: '留言', value: data.messageCount },
    { icon: <EyeOutlined />, color: '#34bfa3', label: '阅读量', value: data.totalViews },
  ]

  return (
    <>
      <Row gutter={[16, 16]} className="panel-group">
        {cards.map((c) => (
          <Col key={c.label} xs={12} md={8} xl={4}>
            <Card {...c} />
          </Col>
        ))}
      </Row>
      <div className="app-container chart-wrapper" style={{ marginTop: 16 }}>
        <h3>最近 7 天新增</h3>
        <LineChart data={data.last7Days} />
      </div>
      <Row gutter={16} style={{ marginTop: 16 }}>
        <Col xs={24} lg={12}>
          <div className="app-container chart-wrapper">
            <h3>分类文章数</h3>
            <BarList data={data.articlesByCategory} color="#36a3f7" />
          </div>
        </Col>
        <Col xs={24} lg={12}>
          <div className="app-container chart-wrapper">
            <h3>标签文章数</h3>
            <BarList data={data.articlesByTag} color="#40c9c6" />
          </div>
        </Col>
      </Row>
    </>
  )
}
