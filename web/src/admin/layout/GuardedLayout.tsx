import RequireAuth from '../RequireAuth'
import AdminLayout from './AdminLayout'

export default function GuardedLayout() {
  return (
    <RequireAuth>
      <AdminLayout />
    </RequireAuth>
  )
}
