import { api } from "@/lib/api"

export type MarketTicker = {
  key: string              // 내부 식별자 (예: "NASDAQ", "USD_KRW")
  name: string             // 표시 이름
  symbol?: string          // 종목 심볼(선택)
  price: number            // 현재값
  change: number           // 변동값(절대값)
  changePct: number        // 변동률(%)
  updatedAtIso: string     // ISO 문자열
}

export type MarketOverviewResponse = {
  tickers: MarketTicker[]
  topMovers: Array<{
    symbol: string
    name?: string
    price: number
    changePct: number
  }>
}

/**
 * 서버에 /api/market/overview 가 있으면 그걸 호출
 * 없으면(404 등) 프론트에서 더미 데이터로 fallback 처리하도록 설계
 */
export async function getMarketOverview() {
  const res = await api.get<MarketOverviewResponse>("/market/overview")
  return res.data
}