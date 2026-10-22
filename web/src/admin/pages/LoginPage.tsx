import { LockOutlined, SafetyOutlined, UserOutlined } from '@ant-design/icons'
import { Alert, Button, Checkbox, Form, Input } from 'antd'
import { useCallback, useEffect, useState } from 'react'
import { Navigate, useNavigate, useSearchParams } from 'react-router-dom'
import { getToken, setToken } from '../../shared/token'
import { adminApi, errorText, type Captcha } from '../api'
import bg from '../assets/login-bg.jpg'

const REMEMBER_KEY = 'zacblog_admin_username'

interface LoginForm {
  username: string
  password: string
  captchaCode: string
  rememberMe: boolean
}

/** 只接受站内后台地址作为登录后的跳转目标，防止开放重定向 */
function safeRedirect(raw: string | null): string {
  if (raw && raw.startsWith('/admin') && !raw.startsWith('/admin/login')) return raw
  return '/admin'
}

export default function LoginPage() {
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const [form] = Form.useForm<LoginForm>()
  const [captcha, setCaptcha] = useState<Captcha | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const redirect = safeRedirect(params.get('redirect'))

  const refreshCaptcha = useCallback(() => {
    form.setFieldValue('captchaCode', '')
    adminApi.captcha().then(setCaptcha, (err) => setError(errorText(err)))
  }, [form])

  useEffect(() => {
    refreshCaptcha()
  }, [refreshCaptcha])

  if (getToken()) return <Navigate to={redirect} replace />

  const onFinish = async (values: LoginForm) => {
    if (!captcha) return
    setLoading(true)
    setError(null)
    try {
      const { token } = await adminApi.login({
        username: values.username.trim(),
        password: values.password,
        captchaId: captcha.captchaId,
        captchaCode: values.captchaCode.trim(),
      })
      if (values.rememberMe) localStorage.setItem(REMEMBER_KEY, values.username.trim())
      else localStorage.removeItem(REMEMBER_KEY)
      setToken(token)
      navigate(redirect, { replace: true })
    } catch (err) {
      setError(errorText(err))
      // 验证码只能用一次，失败后必须换一张
      refreshCaptcha()
    } finally {
      setLoading(false)
    }
  }

  const remembered = localStorage.getItem(REMEMBER_KEY)

  return (
    <div className="login" style={{ backgroundImage: `url(${bg})` }}>
      <Form
        form={form}
        className="login-form"
        onFinish={onFinish}
        initialValues={{ username: remembered ?? '', rememberMe: remembered !== null }}
      >
        <h3 className="title">Zac Admin System</h3>
        {error && <Alert type="error" showIcon message={error} style={{ marginBottom: 16 }} />}
        <Form.Item name="username" rules={[{ required: true, whitespace: true, message: '请输入您的账号' }]}>
          <Input prefix={<UserOutlined />} placeholder="Username" autoComplete="username" aria-label="账号" />
        </Form.Item>
        <Form.Item name="password" rules={[{ required: true, message: '请输入您的密码' }]}>
          <Input.Password
            prefix={<LockOutlined />}
            placeholder="Password"
            autoComplete="current-password"
            aria-label="密码"
          />
        </Form.Item>
        <Form.Item>
          <div className="login-code">
            <Form.Item name="captchaCode" noStyle rules={[{ required: true, whitespace: true, message: '请输入验证码' }]}>
              <Input prefix={<SafetyOutlined />} placeholder="Captcha" autoComplete="off" aria-label="验证码" />
            </Form.Item>
            <button type="button" className="login-code-img" onClick={refreshCaptcha} aria-label="换一张验证码">
              {captcha && <img src={captcha.image} alt="验证码" />}
            </button>
          </div>
        </Form.Item>
        <Form.Item name="rememberMe" valuePropName="checked" style={{ marginBottom: 25 }}>
          <Checkbox>Remember Me</Checkbox>
        </Form.Item>
        <Form.Item>
          <Button type="primary" htmlType="submit" block loading={loading} size="large">
            {loading ? 'Logging in...' : 'Login'}
          </Button>
        </Form.Item>
      </Form>
      <div className="el-login-footer">
        <span>Copyright © 2023-{new Date().getFullYear()} Zac All Rights Reserved.</span>
      </div>
    </div>
  )
}
