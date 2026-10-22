import { App, ConfigProvider } from 'antd'
// 用 es 目录下的 ESM 版本：antd/locale/zh_CN 是 CommonJS，经 Vite 转换后默认导出会多包一层，表格、分页、日期选择器会退回英文
import zhCN from 'antd/es/locale/zh_CN'
import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn'
import { Outlet } from 'react-router-dom'
import './admin.css'

dayjs.locale('zh-cn')

/** 后台所有页面（含登录页）共用的 antd 主题和中文语言包 */
export default function AdminRoot() {
  return (
    <ConfigProvider locale={zhCN} theme={{ token: { colorPrimary: '#409EFF', borderRadius: 8 } }}>
      <App className="admin-root">
        <Outlet />
      </App>
    </ConfigProvider>
  )
}
