import { createFileRoute, Link } from "@tanstack/react-router"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { useState } from "react"

export const Route = createFileRoute("/")({
  component: HomePage,
})

function HomePage() {
  const [q, setQ] = useState("")

  return (
    <div className="mx-auto w-full max-w-6xl space-y-6 p-6">
      <div className="flex items-end justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold">Market Overview</h1>
          <p className="text-sm text-muted-foreground">
            로그인 없이 시장(지수/환율)을 확인하고, 로그인 후 포트폴리오를 관리합니다.
          </p>
        </div>

        <div className="flex gap-2">
          <Button asChild variant="outline">
            <Link to="/sign-in">로그인</Link>
          </Button>
          <Button asChild>
            <Link to="/dashboard">대시보드</Link>
          </Button>
        </div>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>종목 검색</CardTitle>
        </CardHeader>
        <CardContent className="flex gap-2">
          <Input
            value={q}
            onChange={(e) => setQ(e.target.value)}
            placeholder="예: AAPL, TSLA, NVDA"
          />
          <Button
            onClick={() => alert(`검색: ${q.trim().toUpperCase()}`)}
            className="w-28"
          >
            검색
          </Button>
        </CardContent>
      </Card>

      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        <Card>
          <CardHeader className="pb-2"><CardTitle className="text-sm">NASDAQ</CardTitle></CardHeader>
          <CardContent><div className="text-2xl font-semibold">-</div></CardContent>
        </Card>
        <Card>
          <CardHeader className="pb-2"><CardTitle className="text-sm">S&amp;P 500</CardTitle></CardHeader>
          <CardContent><div className="text-2xl font-semibold">-</div></CardContent>
        </Card>
        <Card>
          <CardHeader className="pb-2"><CardTitle className="text-sm">USD/KRW</CardTitle></CardHeader>
          <CardContent><div className="text-2xl font-semibold">-</div></CardContent>
        </Card>
        <Card>
          <CardHeader className="pb-2"><CardTitle className="text-sm">JPY/KRW(100)</CardTitle></CardHeader>
          <CardContent><div className="text-2xl font-semibold">-</div></CardContent>
        </Card>
      </div>
    </div>
  )
}