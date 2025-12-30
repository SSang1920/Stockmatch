import { createFileRoute } from '@tanstack/react-router'

export const Route = createFileRoute('/stocks/$market/$ticker')({
  component: StockDetailPage,
})

function StockDetailPage() {
  // URL 파라미터 꺼내기
  const { market, ticker } = Route.useParams()

  return (
    <div className="p-4">
      <h1 className="text-2xl font-bold">주식 상세 페이지</h1>
      <p>시장: {market}</p>
      <p>티커: {ticker}</p>
      {/* 여기에 차트와 가격 정보를 추가할 예정 */}
    </div>
  )
}
