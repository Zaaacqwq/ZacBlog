import { PlusOutlined } from '@ant-design/icons'
import { App, Button, Form, Input, Modal, Popconfirm, Select, Table, Tabs, Tag } from 'antd'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import type { PageResult } from '../../shared/api'
import { adminApi, errorText, formatTime, type IpBlock, type KeywordBlock, type Query, type Reader } from '../api'
import { usePaged } from '../usePaged'

const fetchReaders = adminApi.readers

function ReaderTable() {
  const { message, modal } = App.useApp()
  const table = usePaged<Reader>(fetchReaders)
  const [keyword, setKeyword] = useState('')
  const [blocked, setBlocked] = useState<string | undefined>()

  const apply = (next: { keyword?: string; blocked?: string }) => {
    const k = next.keyword ?? keyword
    const b = 'blocked' in next ? next.blocked : blocked
    table.search({ keyword: k.trim(), blocked: b })
  }

  const block = (r: Reader) => {
    let reason = ''
    modal.confirm({
      title: `拉黑 ${r.email}？`,
      content: (
        <Input.TextArea
          aria-label="拉黑原因"
          placeholder="原因（可选）"
          maxLength={255}
          onChange={(e) => (reason = e.target.value)}
          style={{ marginTop: 8 }}
        />
      ),
      okText: '拉黑',
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await adminApi.blockReader(r.email, reason.trim() || undefined)
          message.success('已拉黑')
          table.reload()
        } catch (err) {
          message.error(errorText(err))
        }
      },
    })
  }

  const unblock = async (r: Reader) => {
    try {
      await adminApi.unblockReader(r.email)
      message.success('已解除')
      table.reload()
    } catch (err) {
      message.error(errorText(err))
    }
  }

  return (
    <>
      <div className="toolbar">
        <Input.Search
          placeholder="搜索邮箱或昵称"
          allowClear
          style={{ width: 260 }}
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          onSearch={(k) => apply({ keyword: k })}
        />
        <Select
          aria-label="状态筛选"
          placeholder="全部状态"
          allowClear
          style={{ width: 120 }}
          value={blocked}
          onChange={(b) => {
            setBlocked(b)
            apply({ blocked: b })
          }}
          options={[
            { value: 'false', label: '正常' },
            { value: 'true', label: '已拉黑' },
          ]}
        />
      </div>
      <Table<Reader>
        rowKey="email"
        loading={table.loading}
        dataSource={table.rows}
        pagination={table.pagination}
        columns={[
          { title: '昵称', dataIndex: 'nickname' },
          { title: '邮箱', dataIndex: 'email' },
          {
            title: '评论数',
            dataIndex: 'commentCount',
            width: 90,
            render: (n: number, r) => <Link to={`/admin/comments?reader=${encodeURIComponent(r.email)}`}>{n}</Link>,
          },
          {
            title: '留言数',
            dataIndex: 'messageCount',
            width: 90,
            render: (n: number, r) => <Link to={`/admin/messages?reader=${encodeURIComponent(r.email)}`}>{n}</Link>,
          },
          { title: '最近一次', dataIndex: 'lastActiveAt', width: 170, render: formatTime },
          {
            title: '状态',
            dataIndex: 'blocked',
            width: 90,
            render: (b: boolean) => (b ? <Tag color="error">已拉黑</Tag> : <Tag color="success">正常</Tag>),
          },
          {
            title: '操作',
            key: 'op',
            width: 100,
            render: (_, r) =>
              r.blocked ? (
                <Popconfirm title={`解除拉黑 ${r.email}？`} onConfirm={() => unblock(r)}>
                  <Button type="link" size="small">
                    解除
                  </Button>
                </Popconfirm>
              ) : r.email.startsWith('user:') ? (
                <Button type="link" size="small" disabled title="没有邮箱的旧注册读者无法按邮箱拉黑">
                  拉黑
                </Button>
              ) : (
                <Button type="link" size="small" danger onClick={() => block(r)}>
                  拉黑
                </Button>
              ),
          },
        ]}
      />
    </>
  )
}

interface BlockRow {
  id: number
  reason: string | null
  createdAt: string
}

/** IP 黑名单和关键字黑名单结构相同，只有主字段不同 */
function BlockTable<T extends BlockRow>({
  field,
  label,
  fetcher,
  add,
  remove,
}: {
  field: 'ip' | 'keyword'
  label: string
  fetcher: (q: Query) => Promise<PageResult<T>>
  add: (value: string, reason: string) => Promise<unknown>
  remove: (id: number) => Promise<unknown>
}) {
  const { message } = App.useApp()
  const table = usePaged<T>(fetcher)
  const [open, setOpen] = useState(false)
  const [form] = Form.useForm<{ value: string; reason: string }>()

  const submit = async () => {
    const v = await form.validateFields()
    try {
      await add(v.value.trim(), v.reason?.trim() ?? '')
      message.success('添加成功')
      setOpen(false)
      form.resetFields()
      table.reload()
    } catch (err) {
      message.error(errorText(err))
    }
  }

  const del = async (id: number) => {
    try {
      await remove(id)
      message.success('已删除')
      table.reload()
    } catch (err) {
      message.error(errorText(err))
    }
  }

  return (
    <>
      <div className="toolbar">
        <Button type="primary" ghost icon={<PlusOutlined />} onClick={() => setOpen(true)}>
          新增{label}
        </Button>
      </div>
      <Table<T>
        rowKey="id"
        loading={table.loading}
        dataSource={table.rows}
        pagination={table.pagination}
        columns={[
          { title: label, dataIndex: field },
          { title: '原因', dataIndex: 'reason' },
          { title: '添加时间', dataIndex: 'createdAt', width: 170, render: formatTime },
          {
            title: '操作',
            key: 'op',
            width: 100,
            render: (_, row) => (
              <Popconfirm title="确认删除？" onConfirm={() => del(row.id)}>
                <Button type="link" size="small" danger>
                  删除
                </Button>
              </Popconfirm>
            ),
          },
        ]}
      />
      <Modal title={`新增${label}`} open={open} onOk={submit} onCancel={() => setOpen(false)}>
        <Form form={form} layout="vertical">
          <Form.Item name="value" label={label} rules={[{ required: true, whitespace: true, message: `请输入${label}` }]}>
            <Input placeholder={field === 'ip' ? '例如 203.0.113.7' : '命中昵称或内容即拦截，不区分大小写'} />
          </Form.Item>
          <Form.Item name="reason" label="原因">
            <Input maxLength={255} />
          </Form.Item>
        </Form>
      </Modal>
    </>
  )
}

export default function ReaderPage() {
  return (
    <div className="app-container">
      <Tabs
        destroyOnHidden
        items={[
          { key: 'readers', label: '读者', children: <ReaderTable /> },
          {
            key: 'ip',
            label: 'IP 黑名单',
            children: (
              <BlockTable<IpBlock>
                field="ip"
                label="IP"
                fetcher={adminApi.ipBlocks}
                add={adminApi.addIpBlock}
                remove={adminApi.deleteIpBlock}
              />
            ),
          },
          {
            key: 'keyword',
            label: '关键字黑名单',
            children: (
              <BlockTable<KeywordBlock>
                field="keyword"
                label="关键字"
                fetcher={adminApi.keywordBlocks}
                add={adminApi.addKeywordBlock}
                remove={adminApi.deleteKeywordBlock}
              />
            ),
          },
        ]}
      />
    </div>
  )
}
