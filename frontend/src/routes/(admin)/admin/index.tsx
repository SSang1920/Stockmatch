import { createFileRoute } from '@tanstack/react-router'
import { AdminDashboard } from '@/features/admin/components/AdminDashboard'

export const Route = createFileRoute('/(admin)/admin/')({
  component: AdminPage,
})

function AdminPage() {
  return (
    <div className="p-6">

      <AdminDashboard />
    </div>
  )
}