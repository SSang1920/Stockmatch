import { StockChartItem, StockDetailResponse } from '@/features/market/types';
import { createFileRoute } from '@tanstack/react-router';
import { useCallback, useEffect, useState } from 'react';
import { Loader2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { StockHeader } from '@/features/market/components/detail/StockHeader';
import { StockPriceInfo } from '@/features/market/components/detail/StockPriceInfo';
import { StockInfoGrid } from '@/features/market/components/detail/StockInfoGrid';
import { StockCandleChart } from '@/features/market/components/detail/StockCandleChart';
import { Watchlist } from '@/features/watchlist/types';
import * as stockApi from '@/features/market/api';
import * as watchlistApi from '@/features/watchlist/api';

// 라우트 정의
export const Route = createFileRoute('/_public/stocks/$market/$ticker')({
  component: StockDetailPage,
})

function StockDetailPage() {
  const { market, ticker } = Route.useParams()

  // 데이터 상태
  const [data, setData] = useState<StockDetailResponse | null>(null);
  const [originalData, setOriginalData] = useState<StockChartItem[]>([]);
  const [watchlists, setWatchlists] = useState<Watchlist[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // 설정 상태
  const [currencyMode, setCurrencyMode] = useState<'KRW' | 'USD'>(() => {
    const saved = localStorage.getItem('stock-currency-mode');
    return (saved === 'KRW' || saved === 'USD') ? saved : 'USD';
  });

  const exchangeRate = 1450;
  const isKrMarket = ['KOSPI', 'KOSDAQ', 'KR'].includes(market.toUpperCase());

  // 설정 저장
  useEffect(() => {
    localStorage.setItem('stock-currency-mode', currencyMode);
  }, [currencyMode]);

  const loadData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [detailResult, chartResult, watchlistResult] = await Promise.all([
        stockApi.getStockDetail(market, ticker),
        stockApi.getStockChart(ticker),
        watchlistApi.getWatchlists()
      ]);

      setData(detailResult);

      // 주말 제거 로직
      const filteredWeekends = chartResult.filter(item => {
        const date = new Date(item.date);
        const day = date.getDay();
        return day !== 0 && day !== 6;
      });
      setOriginalData(filteredWeekends);

      setWatchlists(watchlistResult);

    } catch (err: any) {
      setError(err.message || '정보를 불러오는데 실패했습니다.');
    } finally {
      setLoading(false);
    }
  }, [market, ticker]);

  // 관심종목만 따로 갱신
  const refreshWatchlists = async () => {
    try {
      const response = await watchlistApi.getWatchlists();
      setWatchlists(response);
    } catch (e) {
      console.error("관심종목 갱신 실패");
    }
  };

  useEffect(() => { loadData(); }, [loadData]);

  if (loading) return <div className="flex h-screen items-center justify-center"><Loader2 className="h-8 w-8 animate-spin text-primary" /></div>;
  if (error || !data) return <div className="flex flex-col items-center justify-center h-[50vh] gap-4"><p className="text-red-500 font-medium">{error || '데이터가 없습니다.'}</p><Button onClick={() => window.history.back()}>뒤로 가기</Button></div>;
  

  // 계산 로직
  const displayCurrency = isKrMarket ? 'KRW' : currencyMode;
  const rate = (displayCurrency === 'KRW' && !isKrMarket) ? exchangeRate : 1;
  const currentPrice = (data.currentPrice || 0) * rate;
  const changeAmount = (data.changeAmount || 0) * rate;

  return (
    <div className="container mx-auto p-4 space-y-6 max-w-5xl animate-in fade-in duration-500">
      
      {/* 헤더 (뒤로가기, 타이틀, 우측 컨트롤) */}
      <StockHeader 
        market={market}
        ticker={ticker}
        name={data.name || ticker}
        isKrMarket={isKrMarket}
        currencyMode={currencyMode}
        onCurrencyChange={setCurrencyMode}
        onRefresh={loadData}
        onBack={() => window.history.back()}
        watchlists={watchlists}
        onWatchlistChange={refreshWatchlists}
      />

      {/* 가격 정보 */}
      <StockPriceInfo 
        currentPrice={currentPrice}
        changeAmount={changeAmount}
        changeRate={data.changeRate || 0}
        currency={displayCurrency}
        exchangeRate={exchangeRate}
        isKrMarket={isKrMarket}
      />

      {/* 상세 정보 그리드 */}
      <StockInfoGrid 
        data={data}
        rate={rate}
        currency={displayCurrency}
      />

      {/* 차트 */}
      <StockCandleChart 
        originalData={originalData}
        currency={displayCurrency}
        rate={rate}
        ticker={ticker}
      />

    </div>
  )
}