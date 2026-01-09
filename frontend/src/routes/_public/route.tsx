import { createFileRoute, Link, Outlet } from '@tanstack/react-router'
import { TrendingUp, LineChart, Search, DollarSign, LayoutDashboard } from 'lucide-react'
import { Header } from '@/components/layout/header'

export const Route = createFileRoute('/_public')({
  component: publicLayout,
})

function publicLayout() {
  return (
      <div className="min-h-screen flex flex-col bg-background w-full">

                  <Header fixed>
                   <Link to="/" className="flex items-center gap-2">
                      <div className="flex h-9 w-9 items-center justify-center rounded-lg border bg-background">
                         <TrendingUp className="h-5 w-5" />
                       </div>
                       <div className="leading-tight text-left">
                       <div className="text-sm font-semibold">StockMatch</div>
                       <div className="text-xs text-muted-foreground">
                         Market · Portfolio
                      </div>
                     </div>
                    </Link>
                  </Header>

            {/* 본문: 좌측 사이드바 + 콘텐츠 */}
            <div className="mx-auto grid w-full max-w-7xl grid-cols-12 gap-6 px-6 pt-16">

              {/* 사이드바 */}
              <aside className="col-span-12 md:col-span-3 hidden md:block">
                 <nav className="space-y-2 rounded-lg border p-3 sticky top-20">
                  <NavItem to="/" icon={<LineChart className="h-4 w-4" />}>
                    시장 요약
                  </NavItem>
                  <NavItem to="/market/search" icon={<Search className="h-4 w-4" />}>
                    종목 검색
                  </NavItem>
                  <NavItem to="/market/fx" icon={<DollarSign className="h-4 w-4" />}>
                    환율
                  </NavItem>

                  <div className="my-2 border-t" />

                  {/* 로그인 필요 메뉴*/}
                  <NavItem to="/dashboard" icon={<LayoutDashboard className="h-4 w-4" />}>
                    내 포트폴리오
                  </NavItem>
                </nav>
              </aside>

              {/* 페이지 콘텐츠 */}
              <main className="col-span-12 md:col-span-9 min-w-0">
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
