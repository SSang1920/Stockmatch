import { StockDetailResponse } from '@/features/market/types';
import { createFileRoute } from '@tanstack/react-router'
import { useEffect, useState } from 'react';
import * as stockApi from '@/features/market/api'
import { ArrowLeft, Loader2, RefreshCcw } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';

// 라우트 정의
export const Route = createFileRoute('/_public/stocks/$market/$ticker')({
  component: StockDetailPage,
})

// 가격 포맷팅
const formatPrice = (price: number | undefined | null, market: string) => {
  if (price === undefined || price === null) return '-';

  const isKr = ['KOSPI', 'KOSDAQ', 'KR'].includes(market.toUpperCase());
  if (isKr) return price.toLocaleString() + '원';
  return `$${price.toLocaleString(undefined, { minimumFractionDigits: 2 })}`;
};

// 색상 결정
const getPriceColor = (rate: number | undefined | null) => {
  if (!rate) return 'text-gray-900';
  if (rate > 0) return 'text-red-500';
  if (rate < 0) return 'text-blue-500';
  return 'text-gray-900';
};

function StockDetailPage() {
  const { market, ticker } = Route.useParams()
  const navigate = Route.useNavigate();

  const [data, setData] = useState<StockDetailResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadData = async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await stockApi.getStockDetail(market, ticker);
      setData(result);
    } catch (err: any) {
      setError(err.message || '정보를 불러오는데 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [market, ticker]);

  if (loading) {
    return (
      <div className="flex h-screen items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
      </div>
    );
  }

  if (error || !data) {
    return (
      <div className="flex flex-col items-center justify-center h-[50vh] gap-4">
        <p className="text-red-500 font-medium">{error || '데이터가 없습니다.'}</p>
        <Button onClick={() => window.history.back()}>뒤로 가기</Button>
      </div>
    );
  }

  const currentPrice = data.currentPrice;
  const changeAmount = data.changeAmount ?? 0;
  const changeRate = data.changeRate ?? 0;
  const volume = data.volume ?? 0;

  return (
    <div className="container mx-auto p-4 space-y-6 max-w-5xl animate-in fade-in duration-500">
      
      {/* 1. 상단 네비게이션 & 헤더 */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <Button variant="ghost" size="icon" onClick={() => window.history.back()}>
            <ArrowLeft className="h-5 w-5" />
          </Button>
          <div>
            <div className="flex items-center gap-2">
              <span className="text-sm font-bold bg-muted px-2 py-0.5 rounded text-muted-foreground">
                {market}
              </span>
              <h1 className="text-2xl font-bold tracking-tight">{ticker}</h1>
            </div>
            {/* 종목명 (API 응답에 name이 있다면 표시) */}
            {data.name && <p className="text-muted-foreground">{data.name}</p>}
          </div>
        </div>
        <Button variant="outline" size="sm" onClick={loadData}>
          <RefreshCcw className="mr-2 h-4 w-4" /> 새로고침
        </Button>
      </div>

      {/* 2. 메인 가격 정보 섹션 */}
      <div className="flex items-end gap-4 pb-4 border-b">
        <span className={`text-4xl font-bold ${getPriceColor(data.changeRate)}`}>
          {formatPrice(data.currentPrice, market)}
        </span>
        <div className={`flex items-center gap-2 text-lg font-medium mb-1 ${getPriceColor(data.changeRate)}`}>
          <span>
            {data.changeAmount > 0 ? '▲' : data.changeAmount < 0 ? '▼' : ''} 
            {Math.abs(data.changeAmount).toLocaleString()}
          </span>
          <span>
            ({data.changeRate > 0 ? '+' : ''}{data.changeRate}%)
          </span>
        </div>
      </div>

      {/* 3. 상세 정보 카드 (시가, 고가, 저가, 거래량 등) */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <InfoCard label="시가 (Open)" value={formatPrice(data.openPrice, market)} />
        <InfoCard label="고가 (High)" value={formatPrice(data.highPrice, market)} color="text-red-500" />
        <InfoCard label="저가 (Low)" value={formatPrice(data.lowPrice, market)} color="text-blue-500" />
        <InfoCard label="거래량 (Vol)" value={volume.toLocaleString()} />
        {data.previousClose > 0 && (
           <InfoCard label="전일 종가" value={formatPrice(data.previousClose, market)} />
        )}
      </div>

      {/* 4. 차트 영역 (플레이스홀더) */}
      <Card className="min-h-[300px] flex items-center justify-center bg-muted/20">
        <div className="text-center text-muted-foreground">
          <p>차트 데이터가 준비중입니다.</p>
          <p className="text-sm">(TradingView 등 차트 라이브러리 연동 예정)</p>
        </div>
      </Card>
      
    </div>
  )
}

function InfoCard({ label, value, color }: { label: string, value: string, color?: string}) {
  return (
    <Card>
      <CardHeader className="pb-2">
        <CardTitle className="text-sm font-medium text-muted-foreground">
          {label}
        </CardTitle>
      </CardHeader>
      <CardContent>
        <div className={`text-xl font-bold ${color || ''}`}>{value}</div>
      </CardContent>
    </Card>
  )
}
