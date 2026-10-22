import { App } from 'antd'
import { useCallback, useEffect, useState } from 'react'
import type { PageResult } from '../shared/api'
import { errorText, type Query } from './api'

/** 分页表格的通用状态：页码、筛选条件、加载和刷新 */
export function usePaged<T>(fetcher: (q: Query) => Promise<PageResult<T>>, initialFilters: Query = {}) {
  const { message } = App.useApp()
  const [filters, setFilters] = useState<Query>(initialFilters)
  const [pageNum, setPageNum] = useState(1)
  const [pageSize, setPageSize] = useState(10)
  const [data, setData] = useState<PageResult<T>>({ rows: [], total: 0 })
  const [version, setVersion] = useState(0)
  // 最近一次完成的请求对应的参数；与当前参数不同就说明还在加载
  const [loadedKey, setLoadedKey] = useState<string | null>(null)
  const requestKey = JSON.stringify([filters, pageNum, pageSize, version])

  useEffect(() => {
    let alive = true
    fetcher({ ...filters, pageNum, pageSize })
      .then((res) => alive && setData(res))
      .catch((err) => alive && message.error(errorText(err)))
      .finally(() => alive && setLoadedKey(requestKey))
    return () => {
      alive = false
    }
    // fetcher 由调用方保证稳定（模块级函数或 useCallback）
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [requestKey])

  const search = useCallback((next: Query) => {
    setFilters(next)
    setPageNum(1)
  }, [])
  const reload = useCallback(() => setVersion((v) => v + 1), [])

  return {
    rows: data.rows,
    loading: loadedKey !== requestKey,
    filters,
    search,
    reload,
    pagination: {
      current: pageNum,
      pageSize,
      total: data.total,
      showSizeChanger: true,
      showTotal: (total: number) => `共 ${total} 条`,
      onChange: (page: number, size: number) => {
        setPageNum(size === pageSize ? page : 1)
        setPageSize(size)
      },
    },
  }
}
