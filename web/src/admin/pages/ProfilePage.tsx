import { App, Button, Form, Input } from 'antd'
import { useState } from 'react'
import { adminApi, errorText } from '../api'

interface PasswordForm {
  oldPassword: string
  newPassword: string
  confirmPassword: string
}

const utf8Bytes = (s: string) => new TextEncoder().encode(s).length

/** 个人中心只有修改密码（docs/api.md 6.3） */
export default function ProfilePage() {
  const { message } = App.useApp()
  const [form] = Form.useForm<PasswordForm>()
  const [saving, setSaving] = useState(false)

  const onFinish = async (values: PasswordForm) => {
    setSaving(true)
    try {
      await adminApi.changePassword(values.oldPassword, values.newPassword)
      message.success('修改成功')
      form.resetFields()
    } catch (err) {
      message.error(errorText(err))
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="app-container" style={{ maxWidth: 560 }}>
      <h3 style={{ marginTop: 0 }}>修改密码</h3>
      <Form form={form} layout="vertical" onFinish={onFinish}>
        <Form.Item name="oldPassword" label="旧密码" rules={[{ required: true, message: '旧密码不能为空' }]}>
          <Input.Password placeholder="请输入旧密码" autoComplete="current-password" />
        </Form.Item>
        <Form.Item
          name="newPassword"
          label="新密码"
          dependencies={['oldPassword']}
          rules={[
            { required: true, message: '新密码不能为空' },
            { min: 6, max: 64, message: '长度在 6 到 64 个字符' },
            {
              validator: (_, v: string | undefined) =>
                !v || utf8Bytes(v) <= 72 ? Promise.resolve() : Promise.reject(new Error('新密码太长（最多 72 字节）')),
            },
            ({ getFieldValue }) => ({
              validator: (_, v: string | undefined) =>
                !v || v !== getFieldValue('oldPassword')
                  ? Promise.resolve()
                  : Promise.reject(new Error('新密码不能与旧密码相同')),
            }),
          ]}
        >
          <Input.Password placeholder="请输入新密码" autoComplete="new-password" />
        </Form.Item>
        <Form.Item
          name="confirmPassword"
          label="确认密码"
          dependencies={['newPassword']}
          rules={[
            { required: true, message: '确认密码不能为空' },
            ({ getFieldValue }) => ({
              validator: (_, v: string | undefined) =>
                !v || v === getFieldValue('newPassword')
                  ? Promise.resolve()
                  : Promise.reject(new Error('两次输入的密码不一致')),
            }),
          ]}
        >
          <Input.Password placeholder="请确认新密码" autoComplete="new-password" />
        </Form.Item>
        <Form.Item>
          <Button type="primary" htmlType="submit" loading={saving}>
            保存
          </Button>
          <Button style={{ marginLeft: 8 }} onClick={() => form.resetFields()}>
            重置
          </Button>
        </Form.Item>
      </Form>
    </div>
  )
}
