import { createFileRoute } from '@tanstack/react-router'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import MarketDashboard from '@/features/market/components/MarketDashboard'
import { StockSearchBar } from '@/features/market/components/StockSearchBar'

export const Route = createFileRoute('/_public/')({
  component: HomePage,
})

function HomePage() {
  return (
    <div className="space-y-6">
      {/* 헤더 영역 */}
      <div>
        <h1 className="text-2xl font-bold">Market Overview</h1>
        <p className="text-sm text-muted-foreground">
          로그인 없이 시장(지수/환율)을 확인하고, 로그인 후 포트폴리오를 관리합니다.
        </p>
      </div>

      {/* 검색 영역 */}
      <Card className="overflow-visible">
        <CardHeader>
          <CardTitle>종목 검색</CardTitle>
        </CardHeader>
        <CardContent>
          <StockSearchBar />
        </CardContent>
      </Card>

      {/* 대시보드 영역 */}
      <div className="mt-8">
        <MarketDashboard />
      </div>
    </div>
  )
}
