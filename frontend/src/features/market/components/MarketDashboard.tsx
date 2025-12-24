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

        console.log("서버에서 받은 데이터:", result);
        
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
    <div className="market-container">
      <MarketCard label="KOSPI" info={data.kospi} />
      <MarketCard label="NASDAQ" info={data.nasdaq} />
      <MarketCard label="S&P 500" info={data.sp500} />
      <ExchangeCard info={data.usdKrw} />
    </div>
  );
};

export default MarketDashboard;