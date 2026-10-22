import { fireEvent, screen, waitFor } from '@testing-library/react'
import { clearToken, getToken, setToken } from '../../shared/token'
import { mockApi, renderAt, route, type Call } from './helpers'

let calls: Call[]
let passwordResponse: { status?: number; msg?: string } = {}

beforeEach(() => {
  setToken('test-only-token')
  passwordResponse = {}
  calls = mockApi((c) => {
    if (route(c) === 'GET /api/admin/me') return { data: { username: 'admin', nickname: 'Zac', avatarUrl: null } }
    if (route(c) === 'PUT /api/admin/profile/password') return { data: null, ...passwordResponse }
  })
})

afterEach(() => {
  vi.unstubAllGlobals()
  clearToken()
})

async function fill(oldPassword: string, newPassword: string, confirm: string) {
  fireEvent.change(await screen.findByPlaceholderText('请输入旧密码'), { target: { value: oldPassword } })
  fireEvent.change(screen.getByPlaceholderText('请输入新密码'), { target: { value: newPassword } })
  fireEvent.change(screen.getByPlaceholderText('请确认新密码'), { target: { value: confirm } })
  fireEvent.click(screen.getByRole('button', { name: /保\s*存/ }))
}

const putCalls = () => calls.filter((c) => route(c) === 'PUT /api/admin/profile/password')

describe('change password', () => {
  it('only offers the password form', async () => {
    renderAt('/admin/profile')
    expect(await screen.findByText('修改密码')).toBeInTheDocument()
    expect(document.querySelectorAll('.app-container input')).toHaveLength(3)
    // 个人中心不在侧栏菜单里，不能高亮任何菜单项
    expect(document.querySelector('.ant-menu-item-selected')).toBeNull()
  })

  it('requires all fields', async () => {
    renderAt('/admin/profile')
    await screen.findByText('修改密码')
    fireEvent.click(screen.getByRole('button', { name: /保\s*存/ }))
    expect(await screen.findByText('旧密码不能为空')).toBeInTheDocument()
    expect(await screen.findByText('新密码不能为空')).toBeInTheDocument()
    expect(await screen.findByText('确认密码不能为空')).toBeInTheDocument()
    expect(putCalls()).toHaveLength(0)
  })

  it('rejects short, too long, same-as-old and mismatched passwords', async () => {
    renderAt('/admin/profile')
    await fill('old-pass', '12345', '12345')
    expect(await screen.findByText('长度在 6 到 64 个字符')).toBeInTheDocument()

    await fill('old-pass', '密'.repeat(25), '密'.repeat(25))
    expect(await screen.findByText('新密码太长（最多 72 字节）')).toBeInTheDocument()

    await fill('old-pass', 'old-pass', 'old-pass')
    expect(await screen.findByText('新密码不能与旧密码相同')).toBeInTheDocument()

    await fill('old-pass', 'new-pass-1', 'new-pass-2')
    expect(await screen.findByText('两次输入的密码不一致')).toBeInTheDocument()
    expect(putCalls()).toHaveLength(0)
  })

  it('submits a valid change', async () => {
    renderAt('/admin/profile')
    await fill('old-pass', 'new-pass-1', 'new-pass-1')
    await waitFor(() => expect(putCalls()[0]?.body).toEqual({ oldPassword: 'old-pass', newPassword: 'new-pass-1' }))
    expect(await screen.findByText('修改成功')).toBeInTheDocument()
  })

  it('shows a 400 from the server without logging out', async () => {
    passwordResponse = { status: 400, msg: '旧密码错误' }
    renderAt('/admin/profile')
    await fill('wrong-pass', 'new-pass-1', 'new-pass-1')
    expect(await screen.findByText('旧密码错误')).toBeInTheDocument()
    expect(getToken()).toBe('test-only-token')
  })
})
