interface Props {
  pageNum: number
  pageSize: number
  total: number
  onChange: (pageNum: number) => void
}

/** 和旧站 el-pagination 的 “Total N  ‹ 1 2 3 ›” 一样的简洁分页 */
export default function Pagination({ pageNum, pageSize, total, onChange }: Props) {
  const pages = Math.max(1, Math.ceil(total / pageSize))
  if (total <= 0) return null
  const from = Math.max(1, Math.min(pageNum - 2, pages - 4))
  const to = Math.min(pages, from + 4)
  const numbers = Array.from({ length: to - from + 1 }, (_, i) => from + i)
  return (
    <nav className="pagination" aria-label="分页">
      <span className="pagination-total">Total {total}</span>
      <button type="button" aria-label="上一页" disabled={pageNum <= 1} onClick={() => onChange(pageNum - 1)}>
        ‹
      </button>
      {numbers.map((n) => (
        <button
          type="button"
          key={n}
          className={n === pageNum ? 'active' : undefined}
          aria-current={n === pageNum ? 'page' : undefined}
          onClick={() => n !== pageNum && onChange(n)}
        >
          {n}
        </button>
      ))}
      <button type="button" aria-label="下一页" disabled={pageNum >= pages} onClick={() => onChange(pageNum + 1)}>
        ›
      </button>
    </nav>
  )
}
