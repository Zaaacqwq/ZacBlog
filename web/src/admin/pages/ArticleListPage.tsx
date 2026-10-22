import { PlusOutlined, ReloadOutlined, SearchOutlined } from '@ant-design/icons'
import { App, Button, Form, Input, Popconfirm, Select, Space, Table, Tag } from 'antd'
import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { adminApi, errorText, formatTime, type Article, type NamedItem } from '../api'
import { usePaged } from '../usePaged'

const fetchArticles = adminApi.articles

export default function ArticleListPage() {
  const { message } = App.useApp()
  const navigate = useNavigate()
  const [form] = Form.useForm()
  const [categories, setCategories] = useState<NamedItem[]>([])
  const [tags, setTags] = useState<NamedItem[]>([])
  const table = usePaged<Article>(fetchArticles)

  useEffect(() => {
    adminApi.named('categories').then(setCategories, () => {})
    adminApi.named('tags').then(setTags, () => {})
  }, [])

  const remove = async (id: number) => {
    try {
      await adminApi.deleteArticle(id)
      message.success('删除成功')
      table.reload()
    } catch (err) {
      message.error(errorText(err))
    }
  }

  return (
    <div className="app-container">
      <Form form={form} layout="inline" className="search-form" onFinish={table.search}>
        <Form.Item name="keyword" label="标题">
          <Input placeholder="请输入标题" allowClear />
        </Form.Item>
        <Form.Item name="categoryId" label="分类">
          <Select
            placeholder="全部"
            allowClear
            style={{ width: 140 }}
            options={categories.map((c) => ({ value: c.id, label: c.name }))}
          />
        </Form.Item>
        <Form.Item name="tagId" label="标签">
          <Select
            placeholder="全部"
            allowClear
            style={{ width: 140 }}
            options={tags.map((t) => ({ value: t.id, label: t.name }))}
          />
        </Form.Item>
        <Form.Item name="status" label="状态">
          <Select
            placeholder="全部"
            allowClear
            style={{ width: 110 }}
            options={[
              { value: 'published', label: '已发布' },
              { value: 'draft', label: '草稿' },
            ]}
          />
        </Form.Item>
        <Form.Item name="essay" label="类型">
          <Select
            placeholder="全部"
            allowClear
            style={{ width: 110 }}
            options={[
              { value: false, label: '文章' },
              { value: true, label: '随笔' },
            ]}
          />
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
                table.search({})
              }}
            >
              重置
            </Button>
          </Space>
        </Form.Item>
      </Form>
      <div className="toolbar">
        <Button type="primary" ghost icon={<PlusOutlined />} onClick={() => navigate('/admin/articles/new')}>
          新增
        </Button>
      </div>
      <Table<Article>
        rowKey="id"
        loading={table.loading}
        dataSource={table.rows}
        pagination={table.pagination}
        scroll={{ x: 900 }}
        columns={[
          {
            title: '标题',
            dataIndex: 'title',
            render: (title: string, a) => (
              <Space size={4}>
                {a.top && <Tag color="red">置顶</Tag>}
                {a.essay && <Tag color="purple">随笔</Tag>}
                <Link to={`/admin/articles/${a.id}/edit`}>{title}</Link>
              </Space>
            ),
          },
          { title: '分类', dataIndex: 'categoryName', width: 110 },
          {
            title: '标签',
            dataIndex: 'tags',
            width: 180,
            render: (list: Article['tags']) => list.map((t) => <Tag key={t.id}>{t.name}</Tag>),
          },
          {
            title: '状态',
            dataIndex: 'status',
            width: 80,
            render: (s: Article['status']) =>
              s === 'published' ? <Tag color="success">已发布</Tag> : <Tag color="warning">草稿</Tag>,
          },
          { title: '阅读', dataIndex: 'views', width: 70 },
          { title: '评论', dataIndex: 'commentCount', width: 70 },
          { title: '创建时间', dataIndex: 'createdAt', width: 170, render: formatTime },
          {
            title: '操作',
            key: 'op',
            width: 120,
            fixed: 'right',
            render: (_, a) => (
              <Space size={0}>
                <Button type="link" size="small" onClick={() => navigate(`/admin/articles/${a.id}/edit`)}>
                  修改
                </Button>
                <Popconfirm title={`确认删除文章「${a.title}」？`} onConfirm={() => remove(a.id)}>
                  <Button type="link" size="small" danger>
                    删除
                  </Button>
                </Popconfirm>
              </Space>
            ),
          },
        ]}
      />
    </div>
  )
}
