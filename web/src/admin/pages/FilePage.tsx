import { App, Button, Input, Popconfirm, Space, Table, Typography } from 'antd'
import { adminApi, errorText, fileUrl, formatSize, formatTime, type FileInfo } from '../api'
import FileUploadButton from '../components/FileUploadButton'
import { usePaged } from '../usePaged'

const fetchFiles = adminApi.files

export default function FilePage() {
  const { message } = App.useApp()
  const table = usePaged<FileInfo>(fetchFiles)

  const remove = async (f: FileInfo) => {
    try {
      await adminApi.deleteFile(f.id)
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
          placeholder="搜索文件名"
          allowClear
          style={{ width: 240 }}
          onSearch={(keyword) => table.search({ keyword })}
        />
        <FileUploadButton onUploaded={table.reload} />
      </div>
      <Table<FileInfo>
        rowKey="id"
        loading={table.loading}
        dataSource={table.rows}
        pagination={table.pagination}
        columns={[
          { title: '编号', dataIndex: 'id', width: 80 },
          {
            title: '文件名',
            dataIndex: 'name',
            render: (name: string, f) => (
              <a href={fileUrl(f.url)} target="_blank" rel="noreferrer">
                {name}
              </a>
            ),
          },
          {
            title: '地址',
            dataIndex: 'url',
            render: (url: string) => (
              <Typography.Text copyable style={{ fontSize: 12 }}>
                {url}
              </Typography.Text>
            ),
          },
          { title: '大小', dataIndex: 'size', width: 100, render: formatSize },
          { title: '上传时间', dataIndex: 'createdAt', width: 170, render: formatTime },
          {
            title: '操作',
            key: 'op',
            width: 90,
            render: (_, f) => (
              <Space>
                <Popconfirm title="删除后文章和公告里的附件也会移除，确认删除？" onConfirm={() => remove(f)}>
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
