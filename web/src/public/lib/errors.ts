import { ApiError } from '../../shared/api'

/** 把接口错误翻成给读者看的提示；403 的 msg 是通用提示，直接展示即可（docs/api.md 第 4 节） */
export function describeError(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 429) return '发得太频繁了，请稍后再试。'
    if (error.status === 403) return error.message || '暂时无法发表，请联系博主。'
    if (error.status === 404) return '文章不存在或已下线。'
    if (error.status >= 500) return '服务器开小差了，请稍后再试。'
    return error.message || '提交失败，请检查后重试。'
  }
  return '网络异常，请稍后再试。'
}
