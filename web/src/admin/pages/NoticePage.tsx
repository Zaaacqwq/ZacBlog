import { DeleteOutlined, PaperClipOutlined, PlusOutlined } from '@ant-design/icons'
import { App, Button, Form, Input, Modal, Popconfirm, Radio, Space, Table, Tag } from 'antd'
import { useState } from 'react'
import { adminApi, errorText, fileUrl, formatTime, type FileInfo, type Notice, type NoticeBody } from '../api'
import FileUploadButton from '../components/FileUploadButton'
import { sanitize } from '../components/markdown'
import { usePaged } from '../usePaged'

const fetchNotices = adminApi.notices

export default function NoticePage() {
  const { message } = App.useApp()
  const table = usePaged<Notice>(fetchNotices)
  const [editing, setEditing] = useState<number | 'new' | null>(null)
  const [files, setFiles] = useState<FileInfo[]>([])
  const [saving, setSaving] = useState(false)
  const [form] = Form.useForm<Omit<NoticeBody, 'fileIds'>>()
  const content = Form.useWatch('content', form) ?? ''

  const open = async (id: number | 'new') => {
    form.resetFields()
    setFiles([])
    if (id === 'new') {
      form.setFieldsValue({ title: '', content: '', status: 'normal' })
      setEditing('new')
      return
    }
    try {
      const n = await adminApi.notice(id)
      form.setFieldsValue({ title: n.title, content: n.content, status: n.status })
      setFiles(n.files ?? [])
      setEditing(id)
    } catch (err) {
      message.error(errorText(err))
    }
  }

  const submit = async () => {
    const values = await form.validateFields()
    const body: NoticeBody = { ...values, title: values.title.trim(), fileIds: files.map((f) => f.id) }
    setSaving(true)
    try {
      if (editing === 'new') await adminApi.createNotice(body)
      else if (editing !== null) await adminApi.updateNotice(editing, body)
      message.success('保存成功')
      setEditing(null)
      table.reload()
    } catch (err) {
      message.error(errorText(err))
    } finally {
      setSaving(false)
    }
  }

  const remove = async (id: number) => {
    try {
      await adminApi.deleteNotice(id)
      message.success('删除成功')
      table.reload()
    } catch (err) {
      message.error(errorText(err))
    }
  }

  return (
    <div className="app-container">
      <div className="toolbar">
        <Input.Search
          placeholder="搜索公告标题"
          allowClear
          style={{ width: 240 }}
          onSearch={(keyword) => table.search({ keyword })}
        />
        <Button type="primary" ghost icon={<PlusOutlined />} onClick={() => open('new')}>
          新增
        </Button>
      </div>
      <Table<Notice>
        rowKey="id"
        loading={table.loading}
        dataSource={table.rows}
        pagination={table.pagination}
        columns={[
          { title: '编号', dataIndex: 'id', width: 80 },
          { title: '公告标题', dataIndex: 'title' },
          {
            title: '状态',
            dataIndex: 'status',
            width: 90,
            render: (s: Notice['status']) => (s === 'normal' ? <Tag color="success">正常</Tag> : <Tag>关闭</Tag>),
          },
          { title: '创建时间', dataIndex: 'createdAt', width: 170, render: formatTime },
          {
            title: '操作',
            key: 'op',
            width: 130,
            render: (_, n) => (
              <Space size={0}>
                <Button type="link" size="small" onClick={() => open(n.id)}>
                  修改
                </Button>
                <Popconfirm title={`确认删除公告「${n.title}」？`} onConfirm={() => remove(n.id)}>
                  <Button type="link" size="small" danger>
                    删除
                  </Button>
                </Popconfirm>
              </Space>
            ),
          },
        ]}
      />
      <Modal
        title={editing === 'new' ? '新增公告' : '修改公告'}
        open={editing !== null}
        width={860}
        confirmLoading={saving}
        onOk={submit}
        onCancel={() => setEditing(null)}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="title" label="公告标题" rules={[{ required: true, whitespace: true, message: '请输入公告标题' }, { max: 50 }]}>
            <Input maxLength={50} showCount />
          </Form.Item>
          <Form.Item name="status" label="状态">
            <Radio.Group
              options={[
                { value: 'normal', label: '正常' },
                { value: 'closed', label: '关闭' },
              ]}
            />
          </Form.Item>
          <Form.Item label="内容（HTML）">
            <div className="md-editor">
              <Form.Item name="content" noStyle>
                <Input.TextArea aria-label="公告内容" autoSize={{ minRows: 10, maxRows: 20 }} />
              </Form.Item>
              <div className="md-preview" style={{ minHeight: 200 }} dangerouslySetInnerHTML={{ __html: sanitize(content) }} />
            </div>
          </Form.Item>
          <Form.Item label="附件">
            <Space direction="vertical">
              {files.map((f) => (
                <Space key={f.id}>
                  <PaperClipOutlined />
                  <a href={fileUrl(f.url)} target="_blank" rel="noreferrer">
                    {f.name}
                  </a>
                  <Button
                    type="text"
                    size="small"
                    danger
                    icon={<DeleteOutlined />}
                    aria-label={`移除附件 ${f.name}`}
                    onClick={() => setFiles((prev) => prev.filter((x) => x.id !== f.id))}
                  />
                </Space>
              ))}
              <FileUploadButton onUploaded={(f) => setFiles((prev) => [...prev, f])}>上传附件</FileUploadButton>
            </Space>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
