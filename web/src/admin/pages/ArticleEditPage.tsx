import { DeleteOutlined, PaperClipOutlined } from '@ant-design/icons'
import { App, Button, Col, Form, Image, Input, Radio, Row, Select, Space, Spin, Switch } from 'antd'
import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { adminApi, errorText, fileUrl, formatSize, type ArticleBody, type FileInfo, type NamedItem } from '../api'
import FileUploadButton from '../components/FileUploadButton'
import { renderMarkdown, sanitize } from '../components/markdown'

interface FormValues {
  title: string
  summary: string
  coverUrl: string
  contentType: 'markdown' | 'html'
  contentMarkdown: string
  contentHtml: string
  categoryId: number | null
  tagIds: number[]
  top: boolean
  essay: boolean
}

const EMPTY: FormValues = {
  title: '',
  summary: '',
  coverUrl: '',
  contentType: 'markdown',
  contentMarkdown: '',
  contentHtml: '',
  categoryId: null,
  tagIds: [],
  top: false,
  essay: false,
}

/** 按文章 id 重新挂载编辑器：标签页从文章 A 切到文章 B 时不会复用 A 的表单和加载状态 */
export default function ArticleEditRoute() {
  const { id } = useParams()
  return <ArticleEditor key={id ?? 'new'} articleId={id ? Number(id) : null} />
}

function ArticleEditor({ articleId }: { articleId: number | null }) {
  const navigate = useNavigate()
  const { message } = App.useApp()
  const [form] = Form.useForm<FormValues>()
  const [loading, setLoading] = useState(articleId !== null)
  const [saving, setSaving] = useState(false)
  // 编辑已有文章时，加载失败就不允许保存，避免把空表单写回去
  const [loadFailed, setLoadFailed] = useState(false)
  const [categories, setCategories] = useState<NamedItem[]>([])
  const [tags, setTags] = useState<NamedItem[]>([])
  const [files, setFiles] = useState<FileInfo[]>([])

  const contentType = Form.useWatch('contentType', form) ?? 'markdown'
  const markdown = Form.useWatch('contentMarkdown', form) ?? ''
  const html = Form.useWatch('contentHtml', form) ?? ''
  const coverUrl = Form.useWatch('coverUrl', form) ?? ''
  const preview = useMemo(
    () => (contentType === 'markdown' ? renderMarkdown(markdown) : sanitize(html)),
    [contentType, markdown, html],
  )

  useEffect(() => {
    adminApi.named('categories').then(setCategories, () => {})
    adminApi.named('tags').then(setTags, () => {})
  }, [])

  useEffect(() => {
    if (articleId === null) return
    let alive = true
    adminApi
      .article(articleId)
      .then((a) => {
        if (!alive || a.id !== articleId) return
        form.setFieldsValue({
          title: a.title,
          summary: a.summary ?? '',
          coverUrl: a.coverUrl ?? '',
          contentType: a.contentType ?? 'markdown',
          contentMarkdown: a.contentMarkdown ?? '',
          contentHtml: a.contentHtml ?? '',
          categoryId: a.categoryId,
          tagIds: a.tags.map((t) => t.id),
          top: a.top,
          essay: a.essay,
        })
        setFiles(a.files ?? [])
        setLoading(false)
      })
      .catch((err) => {
        if (!alive) return
        message.error(errorText(err))
        setLoadFailed(true)
        setLoading(false)
      })
    return () => {
      alive = false
    }
  }, [articleId, form, message])

  const canSave = !loading && !loadFailed && !saving

  const save = async (status: ArticleBody['status']) => {
    if (!canSave) return
    let values: FormValues
    try {
      values = await form.validateFields()
    } catch {
      return
    }
    const body: ArticleBody = {
      title: values.title.trim(),
      summary: values.summary?.trim() ?? '',
      coverUrl: values.coverUrl?.trim() || null,
      contentType: values.contentType,
      contentMarkdown: values.contentMarkdown ?? '',
      // Markdown 文章同时保存渲染好的 HTML，旧前台和摘要等场景可直接使用
      contentHtml: values.contentType === 'markdown' ? renderMarkdown(values.contentMarkdown ?? '') : values.contentHtml ?? '',
      categoryId: values.categoryId ?? null,
      tagIds: values.tagIds ?? [],
      top: values.top,
      essay: values.essay,
      status,
      fileIds: files.map((f) => f.id),
    }
    setSaving(true)
    try {
      const saved = articleId === null ? await adminApi.createArticle(body) : await adminApi.updateArticle(articleId, body)
      message.success(status === 'published' ? '发布成功' : '已暂存')
      if (articleId === null) navigate(`/admin/articles/${saved.id}/edit`, { replace: true })
    } catch (err) {
      message.error(errorText(err))
    } finally {
      setSaving(false)
    }
  }

  return (
    <Spin spinning={loading}>
      <div className="app-container">
        <Form form={form} layout="vertical" initialValues={EMPTY}>
          <Row gutter={16}>
            <Col xs={24} lg={12}>
              <Form.Item name="title" label="标题" rules={[{ required: true, whitespace: true, message: '请输入标题' }, { max: 50 }]}>
                <Input placeholder="文章标题" showCount maxLength={50} />
              </Form.Item>
            </Col>
            <Col xs={12} lg={6}>
              <Form.Item name="categoryId" label="分类">
                <Select allowClear placeholder="请选择分类" options={categories.map((c) => ({ value: c.id, label: c.name }))} />
              </Form.Item>
            </Col>
            <Col xs={12} lg={6}>
              <Form.Item name="tagIds" label="标签">
                <Select
                  mode="multiple"
                  allowClear
                  placeholder="请选择标签"
                  optionFilterProp="label"
                  options={tags.map((t) => ({ value: t.id, label: t.name }))}
                />
              </Form.Item>
            </Col>
            <Col xs={24} lg={12}>
              <Form.Item name="summary" label="简介" rules={[{ max: 100 }]}>
                <Input.TextArea rows={3} showCount maxLength={100} placeholder="文章简介" />
              </Form.Item>
            </Col>
            <Col xs={24} lg={12}>
              <Form.Item label="封面" required={false}>
                <Space.Compact style={{ width: '100%' }}>
                  <Form.Item name="coverUrl" noStyle rules={[{ max: 100 }]}>
                    <Input placeholder="填写图片链接，或右侧上传" allowClear />
                  </Form.Item>
                  <FileUploadButton accept="image/*" onUploaded={(f) => form.setFieldValue('coverUrl', f.url)}>
                    上传封面
                  </FileUploadButton>
                </Space.Compact>
                {coverUrl && (
                  <Image src={fileUrl(coverUrl)} height={72} style={{ marginTop: 8, borderRadius: 6 }} alt="封面预览" />
                )}
              </Form.Item>
            </Col>
            <Col>
              <Form.Item name="top" label="置顶" valuePropName="checked">
                <Switch />
              </Form.Item>
            </Col>
            <Col>
              <Form.Item name="essay" label="随笔" valuePropName="checked">
                <Switch />
              </Form.Item>
            </Col>
            <Col>
              <Form.Item name="contentType" label="正文格式">
                <Radio.Group
                  optionType="button"
                  options={[
                    { value: 'markdown', label: 'Markdown' },
                    { value: 'html', label: 'HTML（旧文章）' },
                  ]}
                />
              </Form.Item>
            </Col>
          </Row>

          <Form.Item label="正文">
            <div className="md-editor">
              <Form.Item name={contentType === 'markdown' ? 'contentMarkdown' : 'contentHtml'} noStyle>
                <Input.TextArea
                  aria-label="正文"
                  style={{ minHeight: 420, maxHeight: 640 }}
                  autoSize={{ minRows: 20, maxRows: 30 }}
                  placeholder={contentType === 'markdown' ? '# 用 Markdown 写正文' : '<p>HTML 正文</p>'}
                />
              </Form.Item>
              <div className="md-preview" aria-label="预览" dangerouslySetInnerHTML={{ __html: preview }} />
            </div>
          </Form.Item>
          {/* 另一种格式的内容也保留在表单里，切换格式时不丢 */}
          <Form.Item name={contentType === 'markdown' ? 'contentHtml' : 'contentMarkdown'} hidden>
            <Input />
          </Form.Item>

          <Form.Item label="附件">
            <Space direction="vertical" style={{ width: '100%' }}>
              {files.map((f) => (
                <Space key={f.id}>
                  <PaperClipOutlined />
                  <a href={fileUrl(f.url)} target="_blank" rel="noreferrer">
                    {f.name}
                  </a>
                  <span style={{ color: '#999' }}>{formatSize(f.size)}</span>
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

          <Space>
            <Button type="primary" loading={saving} disabled={!canSave && !saving} onClick={() => save('published')}>
              发布
            </Button>
            <Button loading={saving} disabled={!canSave && !saving} onClick={() => save('draft')}>
              暂存
            </Button>
            <Button onClick={() => navigate('/admin/articles')}>返回</Button>
          </Space>
        </Form>
      </div>
    </Spin>
  )
}
