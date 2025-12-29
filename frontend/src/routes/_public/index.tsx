import { createFileRoute } from '@tanstack/react-router'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { useState } from 'react'
import MarketDashboard from '@/features/market/components/MarketDashboard'

export const Route = createFileRoute('/_public/')({
  component: HomePage,
})

function HomePage() {
  const [q, setQ] = useState('')

  const handleSearch = () => {
    if (!q.trim()) return
    alert(`검색: ${q.trim().toUpperCase()}`)
    // 추후 검색 페이지 이동 로직: navigate({ to: '/search', search: { q } })
  }

  return (
    <div className="space-y-6">
      {/* 1. 헤더 영역 */}
      <div>
        <h1 className="text-2xl font-bold">Market Overview</h1>
        <p className="text-sm text-muted-foreground">
          로그인 없이 시장(지수/환율)을 확인하고, 로그인 후 포트폴리오를 관리합니다.
        </p>
      </div>

      {/* 2. 검색 영역 */}
      <Card>
        <CardHeader>
          <CardTitle>종목 검색</CardTitle>
        </CardHeader>
        <CardContent className="flex gap-2">
          <Input
            value={q}
            onChange={(e) => setQ(e.target.value)}
            placeholder="예: AAPL, TSLA, NVDA"
            onKeyDown={(e) => e.key === 'Enter' && handleSearch()} // 엔터키 지원
          />
          <Button
            onClick={handleSearch}
            className="w-28"
          >
            검색
          </Button>
        </CardContent>
      </Card>

      {/* 3. 시장 현황 영역 (여기가 바뀌었습니다!) */}
      <div className="mt-8">
        <h2 className="text-lg font-semibold mb-4 px-1">글로벌 주요 지수</h2>
        
        {/* 기존의 하드코딩된 Card들을 지우고 이거 하나면 끝납니다 */}
        <MarketDashboard />
      </div>
    </div>
  )
}
