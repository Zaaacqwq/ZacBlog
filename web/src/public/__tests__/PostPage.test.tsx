import { act, fireEvent, screen, waitFor, within } from '@testing-library/react'
import { fail, mockApi, ok, renderAt } from './helpers'

afterEach(() => {
  vi.unstubAllGlobals()
  localStorage.clear()
})

const LEGACY_IMG = 'https://api.zaaac.vip/profile/upload/2025/09/04/cover_20250904.png'

function article(overrides: Record<string, unknown> = {}) {
  return {
    id: 5,
    title: 'VibeIsland',
    summary: 'notch app',
    coverUrl: null,
    contentMarkdown: `# Building VibeIsland\n\n![shot](${LEGACY_IMG})\n\n![local](/profile/upload/2026/01/01/a.png)\n\n<script>alert(1)</script>`,
    contentHtml: '',
    categoryId: 1,
    categoryName: 'Essay',
    tags: [{ id: 2, name: 'Tech' }],
    top: false,
    essay: false,
    views: 5,
    commentCount: 1,
    createdAt: '2026-07-07T06:02:03Z',
    updatedAt: '2026-07-07T06:02:03Z',
    files: [{ id: 1, name: 'slides.pdf', url: '/profile/upload/2026/01/01/slides.pdf', size: 2048, createdAt: '' }],
    ...overrides,
  }
}

const comments = {
  rows: [
    {
      id: 11,
      parentId: null,
      nickname: '读者甲',
      website: null,
      avatarUrl: null,
      content: '第一条评论',
      likeCount: 2,
      fromAdmin: false,
      replyToNickname: null,
      createdAt: '2026-07-08T00:00:00Z',
      children: [
        {
          id: 12,
          parentId: 11,
          nickname: 'Zac',
          website: null,
          avatarUrl: null,
          content: '谢谢',
          likeCount: 0,
          fromAdmin: true,
          replyToNickname: '读者甲',
          createdAt: '2026-07-08T01:00:00Z',
          children: [],
        },
      ],
    },
  ],
  total: 1,
}

describe('PostPage', () => {
  it('renders markdown, keeps api.zaaac.vip URLs untouched and lists attachments', async () => {
    mockApi({ 'GET /api/public/articles/5': article(), 'GET /api/public/articles/5/comments': comments })
    renderAt('/posts/5')

    expect(await screen.findByRole('heading', { level: 1, name: 'VibeIsland' })).toBeInTheDocument()
    const body = screen.getByTestId('post-content')
    expect(within(body).getByRole('heading', { name: 'Building VibeIsland' })).toBeInTheDocument()
    expect(within(body).getByAltText('shot')).toHaveAttribute('src', LEGACY_IMG)
    expect(within(body).getByAltText('local')).toHaveAttribute('src', '/api/profile/upload/2026/01/01/a.png')
    expect(body.querySelector('script')).toBeNull()
    expect(screen.getByText('2026-07-07 14:02:03')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /slides\.pdf/ })).toHaveAttribute(
      'href',
      '/api/profile/upload/2026/01/01/slides.pdf',
    )
  })

  it('falls back to rich-text HTML for old articles', async () => {
    mockApi({
      'GET /api/public/articles/5': article({
        contentMarkdown: '',
        contentHtml: `<p>旧文章<img src="${LEGACY_IMG}" alt="old" onerror="alert(1)"></p>`,
      }),
    })
    renderAt('/posts/5')
    const img = await screen.findByAltText('old')
    expect(img).toHaveAttribute('src', LEGACY_IMG)
    expect(img).not.toHaveAttribute('onerror')
    expect(screen.getByText('旧文章')).toBeInTheDocument()
  })

  it('shows floors, replies and likes in the comment section', async () => {
    const fetchMock = mockApi({
      'GET /api/public/articles/5': article(),
      'GET /api/public/articles/5/comments': comments,
      'POST /api/public/comments/11/like': () => ok(3),
    })
    renderAt('/posts/5?commentId=11')

    expect(await screen.findByText('第一条评论')).toBeInTheDocument()
    expect(document.getElementById('comment-11')).toHaveClass('highlighted')
    expect(screen.getByText('#1')).toBeInTheDocument()
    expect(screen.getByText('博主')).toBeInTheDocument()

    const like = within(document.getElementById('comment-11')!).getAllByRole('button', { name: '点赞' })[0]
    fireEvent.click(like)
    await waitFor(() => expect(like).toHaveTextContent('3'))
    expect(fetchMock).toHaveBeenCalledWith('/api/public/comments/11/like', expect.objectContaining({ method: 'POST' }))

    fireEvent.click(within(document.getElementById('comment-11')!).getAllByRole('button', { name: 'Reply' })[0])
    expect(screen.getByPlaceholderText('回复 @读者甲')).toBeInTheDocument()
  })

  describe('locating ?commentId= beyond the first page', () => {
    // 25 个楼层，按时间倒序：第 1 页 id 125..116，第 2 页 115..106，第 3 页 105..101
    function floor(id: number) {
      const reply = { ...comments.rows[0].children[0], id: id * 10, parentId: id, content: `回复 ${id}` }
      return { ...comments.rows[0], id, content: `楼层 ${id}`, children: id === 110 ? [reply] : [] }
    }
    function pagedComments() {
      return (url: URL) => {
        const pageNum = Number(url.searchParams.get('pageNum'))
        const ids = Array.from({ length: 25 }, (_, i) => 125 - i).slice((pageNum - 1) * 10, pageNum * 10)
        return ok({ rows: ids.map(floor), total: 25 })
      }
    }

    it('loads the page that holds a top-level comment', async () => {
      mockApi({ 'GET /api/public/articles/5': article(), 'GET /api/public/articles/5/comments': pagedComments() })
      renderAt('/posts/5?commentId=112')

      expect(await screen.findByText('楼层 112')).toBeInTheDocument()
      expect(document.getElementById('comment-112')).toHaveClass('highlighted')
      expect(screen.queryByText('楼层 125')).not.toBeInTheDocument()
      expect(screen.getByRole('button', { name: '2' })).toHaveAttribute('aria-current', 'page')
    })

    it('loads the page that holds a reply inside a later floor', async () => {
      mockApi({ 'GET /api/public/articles/5': article(), 'GET /api/public/articles/5/comments': pagedComments() })
      renderAt('/posts/5?commentId=1100')

      expect(await screen.findByText(/回复 110/)).toBeInTheDocument()
      expect(document.getElementById('comment-1100')).toHaveClass('highlighted')
      expect(screen.getByRole('button', { name: '2' })).toHaveAttribute('aria-current', 'page')
    })

    it('falls back to the first page when the comment is gone', async () => {
      const fetchMock = mockApi({
        'GET /api/public/articles/5': article(),
        'GET /api/public/articles/5/comments': pagedComments(),
      })
      renderAt('/posts/5?commentId=999')

      expect(await screen.findByText('楼层 125')).toBeInTheDocument()
      const commentCalls = fetchMock.mock.calls.filter(([u]) => String(u).includes('/comments'))
      expect(commentCalls).toHaveLength(3)
    })

    it('re-locates when only ?commentId= changes on the same article', async () => {
      mockApi({ 'GET /api/public/articles/5': article(), 'GET /api/public/articles/5/comments': pagedComments() })
      const router = renderAt('/posts/5')
      expect(await screen.findByText('楼层 125')).toBeInTheDocument()

      await act(() => router.navigate('/posts/5?commentId=103'))
      expect(await screen.findByText('楼层 103')).toBeInTheDocument()
      expect(document.getElementById('comment-103')).toHaveClass('highlighted')
      expect(screen.getByRole('button', { name: '3' })).toHaveAttribute('aria-current', 'page')

      await act(() => router.navigate('/posts/5?commentId=112'))
      expect(await screen.findByText('楼层 112')).toBeInTheDocument()
      expect(document.getElementById('comment-112')).toHaveClass('highlighted')
    })

    it('still lets the reader page normally afterwards', async () => {
      mockApi({ 'GET /api/public/articles/5': article(), 'GET /api/public/articles/5/comments': pagedComments() })
      renderAt('/posts/5?commentId=103')
      expect(await screen.findByText('楼层 103')).toBeInTheDocument()
      fireEvent.click(screen.getByRole('button', { name: '1' }))
      expect(await screen.findByText('楼层 125')).toBeInTheDocument()
    })
  })

  it('shows the 404 page when the article does not exist', async () => {
    mockApi({ 'GET /api/public/articles/99': () => fail(404, '文章不存在') })
    renderAt('/posts/99')
    expect(await screen.findByRole('heading', { name: '页面不存在' })).toBeInTheDocument()
  })
})
