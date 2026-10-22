import { App, ConfigProvider } from 'antd'
import zhCN from 'antd/locale/zh_CN'
import { Outlet } from 'react-router-dom'
import './admin.css'

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
