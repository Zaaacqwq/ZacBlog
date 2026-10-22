import { UploadOutlined } from '@ant-design/icons'
import { App, Button, Upload } from 'antd'
import { useState } from 'react'
import { adminApi, errorText, type FileInfo } from '../api'

const MAX_SIZE = 10 * 1024 * 1024

/** 上传到 POST /admin/files，成功后回调 FileInfo */
export default function FileUploadButton({
  onUploaded,
  accept,
  children = '上传文件',
}: {
  onUploaded: (file: FileInfo) => void
  accept?: string
  children?: string
}) {
  const { message } = App.useApp()
  const [busy, setBusy] = useState(false)
  return (
    <Upload
      accept={accept}
      showUploadList={false}
      beforeUpload={(file) => {
        if (file.size > MAX_SIZE) {
          message.error('单个文件不能超过 10MB')
          return Upload.LIST_IGNORE
        }
        setBusy(true)
        adminApi
          .uploadFile(file)
          .then((info) => {
            message.success(`已上传 ${info.name}`)
            onUploaded(info)
          })
          .catch((err) => message.error(errorText(err)))
          .finally(() => setBusy(false))
        return false
      }}
    >
      <Button icon={<UploadOutlined />} loading={busy}>
        {children}
      </Button>
    </Upload>
  )
}
