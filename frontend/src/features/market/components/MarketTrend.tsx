import { useEffect, useState } from "react"
import { MarketTrendData, MarketTrendResponse, StockTrend } from "../types/stock"
import { useNavigate } from "@tanstack/react-router";
import { fetchMarketTrends } from "../api/marketApi";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";

export const MarketTrend = () => {
    const [data, setData] = useState<MarketTrendResponse | null>(null);
    const [loading, setLoading] = useState(true);
    const navigate = useNavigate();

    useEffect(() => {
        const loadTrends = async () => {
            try {
                const result = await fetchMarketTrends();
                setData(result);
            } catch (e) {
                console.error("트렌드 로딩 실패", e);
            } finally {
                setLoading(false);
            }
        };
        loadTrends();
    }, []);

    const handleClick = (stock: StockTrend) => {
        navigate({
            to: '/stocks/$market/$ticker',
            params: {
                market: stock.market,
                ticker: stock.ticker,
            }
        });
    };

    if (loading) return <div className="h-64 bg-gray-50 rounded-xl animate-pulse"></div>
    if (!data) return <div className="text-center py-10 text-red-500">데이터를 불러오지 못했습니다.</div>;

    return (
        <div className="w-full">
            {/* 탭 컴포넌트 적용 */}
            <Tabs defaultValue="KR" className="w-full">
                <div className="flex justify-between items-center mb-6">
                    <h2 className="text-xl font-bold text-gray-800">시장 트렌드 Top 10</h2>
                    <TabsList>
                        <TabsTrigger value="KR">국내</TabsTrigger>
                        <TabsTrigger value="US">해외</TabsTrigger>
                    </TabsList>
                </div>

                {/* 국내 탭 내용 */}
                <TabsContent value="KR" className="mt-0">
                    <TrendGrid data={data.KR} onItemClick={handleClick} />
                </TabsContent>

                {/* 해외 탭 내용 */}
                <TabsContent value="US" className="mt-0">
                    <TrendGrid data={data.US} onItemClick={handleClick} />
                </TabsContent>
            </Tabs>
        </div>
    );
};

// ===== 3열 그리드 컴포넌트 =====
interface TrendGridProps {
    data: MarketTrendData;
    onItemClick: (item: StockTrend) => void;
}

const TrendGrid = ({ data, onItemClick }: TrendGridProps) => {
    return (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            <TrendListCard
                title="거래량 상위"
                items={data.mostActive}
                onItemClick={onItemClick}
            />
            <TrendListCard
                title="급등"
                items={data.gainers}
                onItemClick={onItemClick}
            />
            <TrendListCard
                title="급락"
                items={data.losers}
                onItemClick={onItemClick}
            />
        </div>
    );
};
    
// ===== 개별 리스트 카드 컴포넌트 =====
interface TrendListCardProps {
    title: string;
    items: StockTrend[];
    onItemClick: (item: StockTrend) => void;
}

const TrendListCard = ({ title, items, onItemClick }: TrendListCardProps) => {
    return (
        <div className="bg-white p-5 rounded-xl border border-gray-100 shadow-sm flex flex-col h-full">
            <h3 className="font-bold text-lg mb-4 text-gray-800 border-b pb-2">{title}</h3>

            <ul className="space-y-3 flex-1">
                {items && items.length > 0 ? (
                    items.map((stock: StockTrend, idx: number) => {
                        // 색상 처리 로직
                        const isPlus = stock.changeRate.includes('+') || stock.change.includes('▲');
                        const isMinus = stock.changeRate.includes('-') || stock.change.includes('▼');

                        let colorClass = 'text-gray-900';
                        if (isPlus) colorClass = 'text-red-500';
                        else if (isMinus) colorClass = 'text-blue-500';

                        return (
                            <li
                                key={stock.ticker}
                                onClick={() => onItemClick(stock)}
                                className="flex justify-between items-center group cursor-pointer hover:gb-gray-50 p-2 rounded transition-colors"
                            >
                                <div className="flex items-center gap-3 overflow-hidden">
                                    {/* 1위, 2위, 3위 강조 */}
                                    <span className={`font-bold w-6 h-6 flex items-center justify-center rounded text-sm shrink-0 ${idx < 3 ? 'bg-primary/10 text-primary' : 'text-gray-400'}`}>
                                        {idx + 1}
                                    </span>
                                    <div className="min-w-0">
                                        <div className="font-bold text-sm text-gray-900 truncate">{stock.name}</div>
                                        <div className="text-xs text-gray-500 flex items-center gap-1">
                                            <span className="bg-gray-100 px-1.5 py-0.5 rounded text-[10px] font-medium text-gray-600">
                                                {stock.market}
                                            </span>
                                            {stock.ticker}
                                        </div>
                                    </div>
                                </div>
                                <div className="text-right shrink-0">
                                    <div className="font-bold text-sm text-gray-900">{stock.price}</div>
                                    <div className={`text-xs font-bold ${colorClass}`}>
                                        {stock.changeRate}
                                    </div>
                                </div>
                            </li>
                        );
                    })
                ) : (
                    // 데이터가 없을 경우
                    <div className="flex flex-col items-center justify-center py-20 text-gray-400">
                        <span className="text-2xl mb-2">💤</span>
                        <span className="text-sm">데이터 준비 중</span>
                    </div>
                )}
            </ul>
        </div>
    );
};