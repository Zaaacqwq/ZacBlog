import { Navigate, useSearchParams } from 'react-router-dom'

/** 旧站文章页 /cms/main/blog?id=X[&commentId=Y] → /posts/X[?commentId=Y] */
export function LegacyBlogRedirect() {
  const [params] = useSearchParams()
  const id = params.get('id')
  if (!id || !/^\d+$/.test(id)) return <Navigate to="/" replace />
  const commentId = params.get('commentId')
  const search = commentId && /^\d+$/.test(commentId) ? `?commentId=${commentId}` : ''
  return <Navigate to={`/posts/${id}${search}`} replace />
}
