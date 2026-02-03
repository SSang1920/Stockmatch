import { StockChartItem, StockDetailResponse } from '@/features/market/types';
import { createFileRoute } from '@tanstack/react-router'
import { useEffect, useMemo, useState } from 'react';
import * as stockApi from '@/features/market/api'
import { ArrowLeft, Loader2, RefreshCcw } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import ReactApexChart from 'react-apexcharts';
import { ApexOptions } from 'apexcharts';
import { Tabs, TabsList, TabsTrigger } from '@/components/ui/tabs';

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

  const [oringinalData, setOriginalData] = useState<StockChartItem[]>([]);
  const [chartData, setChartData] = useState<StockChartItem[]>([]);
  const [period, setPeriod] = useState<'1W' | '1M' | '1Y'>('1W');

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadData = async () => {
    setLoading(true);
    setError(null);
    try {
      const [detailResult, chartResult] = await Promise.all([
        stockApi.getStockDetail(market, ticker),
        stockApi.getStockChart(ticker)
      ]);

      setData(detailResult);
      setChartData(chartResult);

      const filteredWeekends = chartResult.filter(item => {
        const date = new Date(item.date);
        const day = date.getDay();
        return day !== 0 && day !== 6;
      });

      setOriginalData(filteredWeekends);

    } catch (err: any) {
      setError(err.message || '정보를 불러오는데 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [market, ticker]);

  useEffect(() => {
    if (oringinalData.length === 0) return;

    const today = new Date();
    const cutoffDate = new Date();

    // 기간에 따른 시작 날짜 계산
    if (period === '1W') {
      cutoffDate.setDate(today.getDate() - 7);
    } else if (period === '1M') {
      cutoffDate.setMonth(today.getMonth() - 1);
    } else if ( period === '1Y') {
      cutoffDate.setFullYear(today.getFullYear() - 1);
    }

    const filtered = oringinalData.filter(item => new Date(item.date) >= cutoffDate);
    setChartData(filtered);
  }, [period, oringinalData]);

  const apexSeries = useMemo(() => {
    if (chartData.length === 0) return [];
    return [{
      data: chartData.map(item => ({
        x: item.date,
        y: [item.open, item.high, item.low, item.close]
      }))
    }];
  }, [chartData]);

  const apexOptions: ApexOptions = useMemo(() => ({
    chart: {
      type: 'candlestick',
      height: 350,
      toolbar: { 
        show: false,
        tools: {
          download: false,
        }
      },
      zoom: {
        enabled: true,
        type: 'x',
        autoScaleYaxis: true
      },
    },
    title: {
      text: '',
      align: 'left',
      style: { fontSize: '16px', fontWeight: 'bold', color: '#374151' }
    },
    xaxis: {
      type: 'category',
      tickAmount: period === '1W' ? undefined : 6,
      labels: {
        formatter: function(val) {
          if (!val) return '';
          const parts = String(val).split('-');
          if (parts.length === 3) {
            return `${parts[1]}-${parts[2]}`;
          }

          return String(val);
        },
        style: {
          fontSize: '11px',
        }
      },
      tooltip: {
        enabled: false
      }
    },
    yaxis: {
      tooltip: { enabled: true },
      labels: {
        formatter: (value) => value.toLocaleString()
      },
      forceNiceScale: true
    },
    plotOptions: {
      candlestick: {
        colors: {
          upward: '#ef4444',
          downward: '#3b82f6'
        },
        wick: {
          useFillColor: true,
        }
      }
    },
    tooltip: {
      enabled: true,
      theme: 'light',
      x: {
        formatter: function(val) {
          if (!val) return '';
          const date = new Date(val);
          return date.toISOString().split('T')[0];
        }
      }
    },
    grid: {
      borderColor: '#e5e7eb',
      xaxis: {
        lines: { show: false }
      },
      yaxis: {
        lines: { show: true }
      }
    }
  }), [period]);

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

  const isKr = ['KOSPI', 'KOSDAQ', 'KR'].includes(market.toUpperCase());

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
              <h1 className="text-2xl font-bold tracking-tight">{data.name || ticker}</h1>
            </div>
            {/* 종목명 (API 응답에 name이 있다면 표시) */}
            {data.name && <p className="text-muted-foreground">{ticker}</p>}
          </div>
        </div>
        <Button variant="outline" size="sm" onClick={loadData}>
          <RefreshCcw className="mr-2 h-4 w-4" /> 새로고침
        </Button>
      </div>

      {/* 2. 메인 가격 정보 섹션 */}
      <div className="flex items-end gap-4 pb-4 border-b">
        <span className={`text-4xl font-bold ${getPriceColor(changeRate)}`}>
          {formatPrice(currentPrice, market)}
        </span>
        <div className={`flex items-center gap-2 text-lg font-medium mb-1 ${getPriceColor(changeRate)}`}>
          <span>
            {changeAmount > 0 ? '+' : ''} 
            {changeAmount.toLocaleString(undefined, {
              minimumFractionDigits: isKr ? 0 : 2,
              maximumFractionDigits: 2
            })}
          </span>
          <span>
            ({changeRate > 0 ? '+' : ''}{changeRate.toFixed(2)}%)
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

      {/* 4. 차트 영역 */}
      <Card className="p-4">
        <CardHeader className="px-0 pt-0 pb-4 flex flex-row items-center justify-between">
          <CardTitle>주가 차트</CardTitle>

          <Tabs value={period} onValueChange={(val) => setPeriod(val as any)} className="w-[200px]">
            <TabsList className="grid w-full grid-cols-3">
              <TabsTrigger value="1W">1주</TabsTrigger>
              <TabsTrigger value="1M">1달</TabsTrigger>
              <TabsTrigger value="1Y">1년</TabsTrigger>
            </TabsList>
          </Tabs>

        </CardHeader>
        <CardContent className="h-[400px] w-full pl-0">
          {chartData.length > 0 ? (
            <ReactApexChart
              options={apexOptions}
              series={apexSeries}
              type="candlestick"
              height="100%"
              width="100%"
            />
          ) : (
            <div className="h-full flex items-center justify-center text-muted-foreground bg-muted/10 rounded-lg">
              <p>표시할 차트 데이터가 없습니다.</p>
            </div>
          )}
        </CardContent>
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
