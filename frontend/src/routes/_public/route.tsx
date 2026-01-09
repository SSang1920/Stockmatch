import { createFileRoute, Outlet } from '@tanstack/react-router'
import { Header } from '@/components/common/Header'
import { Sidebar } from '@/components/common/Sidebar'

export const Route = createFileRoute('/_public')({
  component: PublicLayout,
})

function PublicLayout() {
  return (
    <div className="min-h-screen flex flex-col bg-background">
      {/* 상단 헤더 */}
      <Header />

      {/* 본문: 좌측 사이드바 + 콘텐츠 */}
      <div className="container mx-auto flex flex-1 gap-6 p-6">
        
        {/* 사이드바 영역 지정 */}
        <aside className="hidden md:block flex-shrink-0 w-64 lg:w-72">
           <Sidebar />
        </aside>

        {/* 페이지 콘텐츠 */}
        <main className="flex-1 min-w-0 min-h-[80vh]">
          <Outlet />
        </main>
      </div>
    </div>
  )
}