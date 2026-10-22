import { render, screen } from '@testing-library/react'
import { RouterProvider, createMemoryRouter } from 'react-router-dom'
import { routes } from './routes'

function renderAt(path: string) {
  const router = createMemoryRouter(routes, { initialEntries: [path] })
  render(<RouterProvider router={router} />)
}

describe('routes', () => {
  it('renders the public home page', () => {
    renderAt('/')
    expect(screen.getByRole('heading', { name: 'ZacBlog' })).toBeInTheDocument()
  })

  it('sends anonymous visitors of the admin to the login page', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('{}', { status: 500 })))
    renderAt('/admin')
    expect(await screen.findByText('Zac Admin System', undefined, { timeout: 5000 })).toBeInTheDocument()
    vi.unstubAllGlobals()
  })
})
