import { createFileRoute, Link, Outlet } from '@tanstack/react-router'
import { TrendingUp, LineChart, Search, DollarSign, LayoutDashboard } from 'lucide-react'
import { Button } from '@/components/ui/button'

export const Route = createFileRoute('/_public')({
  component: publicLayout,
})

function publicLayout() {
  return (
    <div className="min-h-screen">
      {/* 상단 헤더 */}
      <Header />

      {/* 본문: 좌측 사이드바 + 콘텐츠 */}
      <div className="mx-auto grid max-w-7xl grid-cols-12 gap-6 p-6">
        <Sidebar />

        {/* 페이지 콘텐츠 */}
        <main className="col-span-12 md:col-span-9 lg:col-span-10 min-h-[80vh]">
          <Outlet />
        </main>
      </div>
    </div>
  )
}

function NavItem(props: {
  to: string
  icon: React.ReactNode
  children: React.ReactNode
}) {
  return (
    <Link
      to={props.to}
      className="flex items-center gap-2 rounded-md px-3 py-2 text-sm hover:bg-muted"
    >
      <span className="text-muted-foreground">{props.icon}</span>
      <span>{props.children}</span>
    </Link>
  )
}
