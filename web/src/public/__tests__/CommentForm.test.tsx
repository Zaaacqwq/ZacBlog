import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import CommentForm from '../components/CommentForm'
import { fail, mockApi, ok } from './helpers'

afterEach(() => {
  vi.unstubAllGlobals()
  localStorage.clear()
})

function fillAndSubmit() {
  fireEvent.change(screen.getByLabelText('昵称'), { target: { value: '访客' } })
  fireEvent.change(screen.getByLabelText('邮箱'), { target: { value: 'a@example.com' } })
  fireEvent.change(screen.getByLabelText('内容'), { target: { value: '写得好' } })
  fireEvent.click(screen.getByRole('button', { name: 'Publish' }))
}

describe('CommentForm', () => {
  it('renders a honeypot text field that is present but not visible', () => {
    mockApi()
    const { container } = render(<CommentForm target={{ kind: 'message' }} onPosted={() => {}} />)
    const honeypot = container.querySelector<HTMLInputElement>('input[name="homepage"]')
    expect(honeypot).not.toBeNull()
    expect(honeypot!.type).toBe('text')
    expect(honeypot).not.toBeVisible()
    expect(honeypot!.tabIndex).toBe(-1)
    expect(screen.getByLabelText('昵称')).toBeVisible()
  })

  it('posts nickname, email, content and an empty honeypot', async () => {
    const fetchMock = mockApi({
      'POST /api/public/articles/3/comments': () => ok({ id: 9 }),
    })
    const onPosted = vi.fn()
    render(<CommentForm target={{ kind: 'article', articleId: 3 }} onPosted={onPosted} />)
    fillAndSubmit()
    await waitFor(() => expect(onPosted).toHaveBeenCalledWith({ id: 9 }))
    const [, init] = fetchMock.mock.calls[0]
    expect(JSON.parse(String(init!.body))).toEqual({
      parentId: null,
      nickname: '访客',
      email: 'a@example.com',
      website: null,
      content: '写得好',
      homepage: '',
    })
  })

  it('requires nickname, email and content before sending', async () => {
    const fetchMock = mockApi()
    render(<CommentForm target={{ kind: 'message' }} onPosted={() => {}} />)
    fireEvent.click(screen.getByRole('button', { name: 'Publish' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('昵称、邮箱和内容都要填写')
    expect(fetchMock).not.toHaveBeenCalled()
  })

  it('shows a slow-down hint on 429', async () => {
    mockApi({ 'POST /api/public/messages': () => fail(429, '操作太频繁') })
    const onPosted = vi.fn()
    render(<CommentForm target={{ kind: 'message' }} onPosted={onPosted} />)
    fillAndSubmit()
    expect(await screen.findByRole('alert')).toHaveTextContent('发得太频繁了，请稍后再试')
    expect(onPosted).not.toHaveBeenCalled()
    // 内容保留，方便稍后重试
    expect(screen.getByLabelText('内容')).toHaveValue('写得好')
  })

  it('shows the server message on 403', async () => {
    mockApi({ 'POST /api/public/messages': () => fail(403, '暂时无法发表') })
    render(<CommentForm target={{ kind: 'message' }} onPosted={() => {}} />)
    fillAndSubmit()
    expect(await screen.findByRole('alert')).toHaveTextContent('暂时无法发表')
  })

  it('shows the validation message on 400', async () => {
    mockApi({ 'POST /api/public/messages': () => fail(400, '邮箱格式不正确') })
    render(<CommentForm target={{ kind: 'message' }} onPosted={() => {}} />)
    fillAndSubmit()
    expect(await screen.findByRole('alert')).toHaveTextContent('邮箱格式不正确')
  })
})
