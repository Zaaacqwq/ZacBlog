import { Link } from 'react-router-dom'

export default function NotFoundPage() {
  return (
    <div className="page">
      <section className="card not-found">
        <div className="eyebrow">404</div>
        <h1>页面不存在</h1>
        <p className="muted">你要找的页面可能已经被移走，或者链接写错了。</p>
        <Link className="btn" to="/">
          回到首页
        </Link>
      </section>
    </div>
  )
}
