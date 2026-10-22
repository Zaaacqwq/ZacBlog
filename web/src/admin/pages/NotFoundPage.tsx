import { Button, Result } from 'antd'
import { Link } from 'react-router-dom'

export default function NotFoundPage() {
  return (
    <div className="app-container">
      <Result
        status="404"
        title="404"
        subTitle="页面不存在"
        extra={
          <Link to="/admin">
            <Button type="primary">返回首页</Button>
          </Link>
        }
      />
    </div>
  )
}
