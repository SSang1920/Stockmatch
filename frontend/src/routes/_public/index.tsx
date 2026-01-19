import { createFileRoute } from '@tanstack/react-router'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import MarketDashboard from '@/features/market/components/MarketDashboard'
import { StockSearchBar } from '@/features/market/components/StockSearchBar'
import { MarketTrend } from '@/features/market/components/MarketTrend'
import { BarChart3 } from 'lucide-react'
import { useEffect } from 'react'

export const Route = createFileRoute('/_public/')({
  component: HomePage,
})

function HomePage() {

  useEffect(() => {
    const savedRedirect = localStorage.getItem('loginRedirect')

    if (savedRedirect) {
      localStorage.removeItem('loginRedirect')
      window.location.href = savedRedirect
    }
  }, [])

  return (
    <div className="space-y-8 pb-10">
      {/* 헤더 영역 */}
      <div>
        <h1 className="text-2xl font-bold flex items-center gap-2">
          <BarChart3 className="h-8 w-8 text-primary" />
          Market Overview
        </h1>
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

      {/* 글로벌 주요 지수 */}
      <div className="mt-10">
        <MarketDashboard />
      </div>

      {/* 시장 트렌드/랭킹 */}
      <section className="mt-10">
        <MarketTrend />
      </section>
    </div>
  )
}
