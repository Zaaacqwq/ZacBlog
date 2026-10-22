import { Link, useSearchParams } from 'react-router-dom'
import hero from '../assets/hero.jpg'
import { publicApi, type Article } from '../api'
import Icon from '../components/Icon'
import Pagination from '../components/Pagination'
import { TwoColumn } from '../components/PublicLayout'
import Sidebar from '../components/Sidebar'
import Wave from '../components/Wave'
import { formatDateTime, resolveFileUrl } from '../lib/content'
import { useApi } from '../lib/useApi'

const PAGE_SIZE = 10

function positiveInt(value: string | null): number | undefined {
  const n = Number(value)
  return Number.isInteger(n) && n > 0 ? n : undefined
}

export default function HomePage() {
  const [params, setParams] = useSearchParams()
  const pageNum = positiveInt(params.get('page')) ?? 1
  const categoryId = positiveInt(params.get('categoryId'))
  const tagId = positiveInt(params.get('tagId'))
  const keyword = params.get('keyword')?.trim() || undefined

  const query = { pageNum, pageSize: PAGE_SIZE, categoryId, tagId, keyword, essay: false }
  const list = useApi(`articles:${JSON.stringify(query)}`, () => publicApi.articles(query))
  const categories = useApi('categories', publicApi.categories).data ?? []
  const tags = useApi('tags', publicApi.tags).data ?? []
  const recommend = useApi('recommend', () => publicApi.recommend(5)).data ?? []

  const filterName = categoryId
    ? categories.find((c) => c.id === categoryId)?.name
    : tagId
      ? tags.find((t) => t.id === tagId)?.name
      : keyword
  const filtered = Boolean(categoryId || tagId || keyword)

  function setFilter(next: Record<string, string | number | undefined>) {
    const p = new URLSearchParams()
    for (const [k, v] of Object.entries(next)) if (v !== undefined) p.set(k, String(v))
    setParams(p)
  }

  function goPage(n: number) {
    const p = new URLSearchParams(params)
    p.set('page', String(n))
    setParams(p)
    document.getElementById('index')?.scrollIntoView?.({ behavior: 'smooth' })
  }

  const rows = list.data?.rows ?? []

  return (
    <div className="home">
      <section className="hero" style={{ backgroundImage: `url(${hero})` }}>
        <div className="hero-content">
          <h1 className="hero-title">Zac&apos;s Blog</h1>
          <p className="hero-subtitle">A beautiful, customized, personal blog</p>
        </div>
        <Wave />
      </section>
      <section id="index" className="home-content">
        <TwoColumn
          side={
            <Sidebar>
              <section className="card side-card">
                <div className="eyebrow">Category</div>
                <ul className="side-list">
                  {categories.map((c) => (
                    <li key={c.id}>
                      <button
                        type="button"
                        className={c.id === categoryId ? 'active' : undefined}
                        onClick={() => setFilter({ categoryId: c.id })}
                      >
                        <span>{c.name}</span>
                        <span>{c.articleCount}</span>
                      </button>
                    </li>
                  ))}
                </ul>
              </section>
              <section className="card side-card">
                <div className="eyebrow">Tags</div>
                <div className="tag-cloud">
                  {tags.map((t) => (
                    <button
                      type="button"
                      key={t.id}
                      className={`tag-chip${t.id === tagId ? ' active' : ''}`}
                      onClick={() => setFilter({ tagId: t.id })}
                    >
                      {t.name} <small>{t.articleCount}</small>
                    </button>
                  ))}
                </div>
              </section>
              <section className="card side-card">
                <div className="eyebrow">Latest Recommend</div>
                <ul className="recommend-list">
                  {recommend.map((a) => (
                    <li key={a.id}>
                      <Link to={`/posts/${a.id}`}>{a.title}</Link>
                    </li>
                  ))}
                </ul>
              </section>
            </Sidebar>
          }
        >
          <section className="card list-card">
            <header className="list-header">
              {filtered && (
                <button type="button" className="icon-btn" aria-label="返回全部文章" onClick={() => setFilter({})}>
                  <Icon name="back" />
                </button>
              )}
              <h2>{filtered ? filterName ?? '筛选结果' : 'All Blogs'}</h2>
            </header>
            {list.error ? <p className="empty">文章加载失败，请稍后刷新。</p> : null}
            {!list.loading && !list.error && rows.length === 0 && <p className="empty">这里还没有文章。</p>}
            <ul className="article-list">
              {rows.map((a) => (
                <ArticleItem key={a.id} article={a} />
              ))}
            </ul>
            <Pagination pageNum={pageNum} pageSize={PAGE_SIZE} total={list.data?.total ?? 0} onChange={goPage} />
          </section>
        </TwoColumn>
      </section>
    </div>
  )
}

function ArticleItem({ article: a }: { article: Article }) {
  return (
    <li className="article-item">
      <Link to={`/posts/${a.id}`} className="article-link">
        <div className="article-cover">
          {a.coverUrl ? <img src={resolveFileUrl(a.coverUrl)} alt="" loading="lazy" /> : <div className="cover-fallback" />}
        </div>
        <div className="article-body">
          <h3>
            {a.top && (
              <span className="top-mark" title="置顶">
                <Icon name="pin" />
                <span className="sr-only">置顶</span>
              </span>
            )}
            {a.title}
          </h3>
          {a.summary && <p className="muted">{a.summary}</p>}
          {a.tags.length > 0 && (
            <div className="tag-row">
              {a.tags.map((t) => (
                <span key={t.id} className="pill">
                  {t.name}
                </span>
              ))}
            </div>
          )}
          <div className="meta">
            <span>
              <Icon name="calendar" /> {formatDateTime(a.createdAt)}
            </span>
            <span>
              <Icon name="eye" /> {a.views}
            </span>
            <span>
              <Icon name="chat" /> {a.commentCount}
            </span>
            {a.categoryName && <span className="pill gray">{a.categoryName}</span>}
          </div>
        </div>
      </Link>
    </li>
  )
}
