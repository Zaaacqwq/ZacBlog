import { useCallback, useEffect, useState } from 'react'

interface State<T> {
  key: string | null
  data?: T
  error?: unknown
}

/**
 * 按 key 拉数据：key 变了就重新请求，key 为 null 时不请求。
 * 重新请求期间保留上一次的 data，避免列表闪烁；reload() 用同一个 key 再拉一次。
 */
export function useApi<T>(key: string | null, fetcher: () => Promise<T>) {
  const [nonce, setNonce] = useState(0)
  const [state, setState] = useState<State<T>>({ key: null })
  const fullKey = key === null ? null : `${key}#${nonce}`

  useEffect(() => {
    if (fullKey === null) return
    let cancelled = false
    fetcher().then(
      (data) => !cancelled && setState({ key: fullKey, data }),
      (error: unknown) => !cancelled && setState((prev) => ({ key: fullKey, data: prev.data, error })),
    )
    return () => {
      cancelled = true
    }
    // fetcher 每次渲染都是新函数，请求只由 key 决定
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [fullKey])

  const reload = useCallback(() => setNonce((n) => n + 1), [])
  const settled = state.key === fullKey
  return {
    data: state.data,
    error: settled ? state.error : undefined,
    loading: fullKey !== null && !settled,
    reload,
  }
}
