import { PlusOutlined } from '@ant-design/icons'
import { App, Button, Form, Input, Modal, Popconfirm, Space, Table } from 'antd'
import { useCallback, useEffect, useState } from 'react'
import { adminApi, errorText, type NamedItem } from '../api'

/** 分类和标签结构相同：只有名称和文章数 */
export default function NamedListPage({ kind }: { kind: 'categories' | 'tags' }) {
  const label = kind === 'categories' ? '分类' : '标签'
  const { message } = App.useApp()
  const [rows, setRows] = useState<NamedItem[]>([])
  const [loading, setLoading] = useState(true)
  const [keyword, setKeyword] = useState('')
  const [editing, setEditing] = useState<NamedItem | 'new' | null>(null)
  const [form] = Form.useForm<{ name: string }>()

  const load = useCallback(() => {
    adminApi
      .named(kind)
      .then(setRows, (err) => message.error(errorText(err)))
      .finally(() => setLoading(false))
  }, [kind, message])

  useEffect(() => {
    load()
  }, [load])

  const open = (item: NamedItem | 'new') => {
    setEditing(item)
    form.setFieldsValue({ name: item === 'new' ? '' : item.name })
  }

  const submit = async () => {
    const { name } = await form.validateFields()
    try {
      if (editing === 'new') await adminApi.createNamed(kind, name.trim())
      else if (editing) await adminApi.updateNamed(kind, editing.id, name.trim())
      message.success('保存成功')
      setEditing(null)
      load()
    } catch (err) {
      message.error(errorText(err))
    }
  }

  const remove = async (item: NamedItem) => {
    try {
      await adminApi.deleteNamed(kind, item.id)
      message.success('删除成功')
      load()
    } catch (err) {
      message.error(errorText(err))
    }
  }

  const shown = keyword ? rows.filter((r) => r.name.toLowerCase().includes(keyword.toLowerCase())) : rows

  return (
    <div className="app-container">
      <div className="toolbar">
        <Input.Search
          placeholder={`搜索${label}名称`}
          allowClear
          style={{ width: 240 }}
          onSearch={setKeyword}
          onChange={(e) => !e.target.value && setKeyword('')}
        />
        <Button type="primary" ghost icon={<PlusOutlined />} onClick={() => open('new')}>
          新增
        </Button>
      </div>
      <Table<NamedItem>
        rowKey="id"
        loading={loading}
        dataSource={shown}
        pagination={{ pageSize: 20, showTotal: (t) => `共 ${t} 条` }}
        columns={[
          { title: '编号', dataIndex: 'id', width: 90 },
          { title: `${label}名称`, dataIndex: 'name' },
          { title: '文章数', dataIndex: 'articleCount', width: 100 },
          {
            title: '操作',
            key: 'op',
            width: 140,
            render: (_, item) => (
              <Space size={0}>
                <Button type="link" size="small" onClick={() => open(item)}>
                  修改
                </Button>
                <Popconfirm title={`确认删除${label}「${item.name}」？`} onConfirm={() => remove(item)}>
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
        title={editing === 'new' ? `新增${label}` : `修改${label}`}
        open={editing !== null}
        onOk={submit}
        onCancel={() => setEditing(null)}
        destroyOnHidden
      >
        <Form form={form} layout="vertical" preserve={false}>
          <Form.Item name="name" label={`${label}名称`} rules={[{ required: true, whitespace: true, message: `请输入${label}名称` }]}>
            <Input maxLength={50} onPressEnter={submit} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
