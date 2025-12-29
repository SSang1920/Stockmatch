import { useEffect, useState } from "react"
import { MarketOverviewResponse } from "../types/market"
import { fetchMarketOverview } from "../api/marketApi";
import { MarketCard } from './MarketCard';
import { ExchangeCard } from './ExchangeCard';

const MarketDashboard = () => {
  const [data, setData] = useState<MarketOverviewResponse | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const loadData = async () => {
      try {
        const result = await fetchMarketOverview();
        setData(result);
      } catch (e) {
        console.error(e);
      } finally {
        setLoading(false);
      }
    };
    loadData();
  }, []);

  if (loading) return <div>로딩 중...</div>;
  if (!data) return <div>데이터 없음</div>;

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
      <MarketCard label="KOSPI" info={data.kospi} />
      <MarketCard label="NASDAQ" info={data.nasdaq} />
      <MarketCard label="S&P 500" info={data.sp500} />
      <ExchangeCard info={data.usdKrw} />
    </div>
  );
};

export default MarketDashboard;