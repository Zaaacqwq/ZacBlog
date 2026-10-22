import { render, screen } from '@testing-library/react'
import { RouterProvider, createMemoryRouter } from 'react-router-dom'
import { routes } from './routes'

function renderAt(path: string) {
  const router = createMemoryRouter(routes, { initialEntries: [path] })
  render(<RouterProvider router={router} />)
}

describe('routes', () => {
  beforeEach(() => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => new Response(JSON.stringify({ code: 200, msg: 'ok', data: null }), { status: 200 })),
    )
  })
  afterEach(() => vi.unstubAllGlobals())

  it('renders the public home page', async () => {
    renderAt('/')
    expect(await screen.findByRole('heading', { name: "Zac's Blog" })).toBeInTheDocument()
  })

  it('renders the admin entry', () => {
    renderAt('/admin')
    expect(screen.getByRole('heading', { name: 'ZacBlog 管理后台' })).toBeInTheDocument()
  })
})
