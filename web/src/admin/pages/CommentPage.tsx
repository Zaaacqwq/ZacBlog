import { ReloadOutlined, SearchOutlined } from '@ant-design/icons'
import { App, Button, DatePicker, Form, Input, Modal, Popconfirm, Space, Table, Tag, Typography } from 'antd'
import type { Dayjs } from 'dayjs'
import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { adminApi, errorText, formatTime, type AdminComment, type CommentKind } from '../api'
import { usePaged } from '../usePaged'

interface Filters {
  keyword?: string
  reader?: string
  articleId?: string
  range?: [Dayjs | null, Dayjs | null] | null
}

/** 评论管理和留言管理共用；?reader= 可从读者管理跳过来按读者筛选 */
export default function CommentPage({ kind }: { kind: CommentKind }) {
  const label = kind === 'comments' ? '评论' : '留言'
  const { message } = App.useApp()
  const [params] = useSearchParams()
  const [form] = Form.useForm<Filters>()
  const [replying, setReplying] = useState<AdminComment | null>(null)
  const [reply, setReply] = useState('')
  const [sending, setSending] = useState(false)
  const initialReader = params.get('reader') ?? undefined
  const table = usePaged<AdminComment>((q) => adminApi.comments(kind, q), { reader: initialReader })

  useEffect(() => {
    form.setFieldsValue({ reader: initialReader })
  }, [form, initialReader])

  const onSearch = (v: Filters) => {
    table.search({
      keyword: v.keyword?.trim(),
      reader: v.reader?.trim(),
      articleId: v.articleId?.trim(),
      from: v.range?.[0]?.format('YYYY-MM-DD'),
      to: v.range?.[1]?.format('YYYY-MM-DD'),
    })
  }

  const remove = async (c: AdminComment) => {
    try {
      const n = await adminApi.deleteComment(kind, c.id)
      message.success(n > 1 ? `已删除 ${n} 条（含回复）` : '删除成功')
      table.reload()
    } catch (err) {
      message.error(errorText(err))
    }
  }

  const sendReply = async () => {
    if (!replying) return
    const content = reply.trim()
    if (!content || content.length > 255) {
      message.warning('回复内容为 1–255 字')
      return
    }
    setSending(true)
    try {
      await adminApi.replyComment(kind, replying.id, content)
      message.success('回复成功')
      setReplying(null)
      setReply('')
      table.reload()
    } catch (err) {
      message.error(errorText(err))
    } finally {
      setSending(false)
    }
  }

  return (
    <div className="app-container">
      <Form form={form} layout="inline" className="search-form" onFinish={onSearch}>
        <Form.Item name="keyword" label="内容">
          <Input placeholder={`${label}内容包含`} allowClear />
        </Form.Item>
        <Form.Item name="reader" label="读者">
          <Input placeholder="邮箱或 user:id" allowClear />
        </Form.Item>
        {kind === 'comments' && (
          <Form.Item name="articleId" label="文章编号">
            <Input placeholder="文章 id" allowClear style={{ width: 100 }} />
          </Form.Item>
        )}
        <Form.Item name="range" label="日期">
          <DatePicker.RangePicker allowEmpty={[true, true]} />
        </Form.Item>
        <Form.Item>
          <Space>
            <Button type="primary" htmlType="submit" icon={<SearchOutlined />}>
              搜索
            </Button>
            <Button
              icon={<ReloadOutlined />}
              onClick={() => {
                form.resetFields()
                form.setFieldsValue({ reader: undefined })
                table.search({})
              }}
            >
              重置
            </Button>
          </Space>
        </Form.Item>
      </Form>
      <Table<AdminComment>
        rowKey="id"
        loading={table.loading}
        dataSource={table.rows}
        pagination={table.pagination}
        scroll={{ x: 1000 }}
        columns={[
          {
            title: '昵称',
            dataIndex: 'nickname',
            width: 140,
            render: (n: string, c) => (
              <Space size={4}>
                {n}
                {c.fromAdmin && <Tag color="blue">博主</Tag>}
              </Space>
            ),
          },
          { title: '邮箱', dataIndex: 'email', width: 180, ellipsis: true },
          {
            title: '内容',
            dataIndex: 'content',
            render: (text: string, c) => (
              <>
                {c.replyToNickname && <Typography.Text type="secondary">回复 @{c.replyToNickname}：</Typography.Text>}
                {text}
              </>
            ),
          },
          ...(kind === 'comments'
            ? [
                {
                  title: '文章',
                  dataIndex: 'articleTitle',
                  width: 160,
                  ellipsis: true,
                  render: (t: string | null, c: AdminComment) =>
                    c.articleId ? <Link to={`/admin/articles/${c.articleId}/edit`}>{t}</Link> : t,
                },
              ]
            : []),
          { title: 'IP', dataIndex: 'ip', width: 130 },
          { title: '时间', dataIndex: 'createdAt', width: 170, render: formatTime },
          {
            title: '操作',
            key: 'op',
            width: 120,
            fixed: 'right' as const,
            render: (_: unknown, c: AdminComment) => (
              <Space size={0}>
                <Button type="link" size="small" onClick={() => setReplying(c)}>
                  回复
                </Button>
                <Popconfirm title={`删除这条${label}及其所有回复？`} onConfirm={() => remove(c)}>
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
        title={`回复 ${replying?.nickname ?? ''}`}
        open={replying !== null}
        confirmLoading={sending}
        onOk={sendReply}
        okText="回复"
        onCancel={() => setReplying(null)}
      >
        <Typography.Paragraph type="secondary">{replying?.content}</Typography.Paragraph>
        <Input.TextArea
          aria-label="回复内容"
          rows={4}
          maxLength={255}
          showCount
          value={reply}
          onChange={(e) => setReply(e.target.value)}
        />
      </Modal>
    </div>
  )
}
