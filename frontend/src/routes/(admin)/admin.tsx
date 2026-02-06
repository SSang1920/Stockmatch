import { createFileRoute, Outlet, Navigate } from '@tanstack/react-router'
import { Header } from '@/components/common/Header'
import { AdminSidebar } from '@/features/admin/components/AdminSidebar'
import { useUser } from '@/context/UserContext'

export const Route = createFileRoute('/(admin)/admin')({
  component: AdminLayout,
})

function AdminLayout() {
  const { user, isLoading } = useUser();

  if (isLoading) {
    return (
        <div className="flex h-screen items-center justify-center bg-background">
            <div className="text-muted-foreground animate-pulse">관리자 권한 확인 중...</div>
        </div>
    );
  }

  if (!user || user.role !== 'ADMIN') {
    return <Navigate to="/" />;
  }

  return (
    <div className="min-h-screen flex flex-col bg-background">
      <Header />

      <div className="container mx-auto flex flex-1 gap-6 p-6 items-start">

        <aside className="hidden md:block w-64 lg:w-72 flex-shrink-0 sticky top-24 h-[calc(100vh-140px)]">
           <AdminSidebar />
        </aside>

        <main className="flex-1 min-w-0 min-h-[80vh]">
          <Outlet />
        </main>
      </div>
    </div>
  )
}