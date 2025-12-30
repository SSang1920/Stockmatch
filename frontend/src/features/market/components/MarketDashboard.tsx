import { useEffect, useState } from "react"
import { MarketOverviewResponse } from "../types/market"
import { fetchMarketOverview } from "../api/marketApi";
import { MarketCard } from './MarketCard';
import { ExchangeCard } from './ExchangeCard';

const MarketDashboard = () => {
    const [data, setData] = useState<MarketOverviewResponse | null>(null);
    const [loading, setLoading] = useState(true);

    // 데이터 가져오는 함수
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

    useEffect(() => {
        // 컴포넌트 켜질 때 즉시 실행
        loadData();

        // 1분마다 자동으로 실행
        const intervalId = setInterval(() => {
            loadData();
        }, 60 * 1000);  // 60초

        // 페이지 나가면 타이머 정리
        return () => clearInterval(intervalId); 
    }, []);

    if (loading) return <div>로딩 중...</div>;
    if (!data) return <div>데이터 없음</div>;

    return (
        <div className="space-y-4">
            <div className="flex justify-between items-end px-1">
                <h2 className="text-lg font-semibold">글로벌 주요 지수</h2>
                {data.lastUpdateTime && (
                    <div className="text-xs text-gray-400 text-right">
                        <p>제공: 한국투자증권 (KIS)</p>
                        <p>업데이트: {data.lastUpdateTime}</p>
                    </div>
                )}
            </div>
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
                <MarketCard label="KOSPI" info={data.kospi} />
                <MarketCard label="NASDAQ" info={data.nasdaq} />
                <MarketCard label="S&P 500" info={data.sp500} />
                <ExchangeCard info={data.usdKrw} />
            </div>
        </div>
    );
};

export default MarketDashboard;