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

  it('renders the admin entry', () => {
    renderAt('/admin')
    expect(screen.getByRole('heading', { name: 'ZacBlog 管理后台' })).toBeInTheDocument()
  })
})
