import { Button } from "@/components/ui/button";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { ArrowLeft, RefreshCcw, Star } from "lucide-react";
import { useMemo, useState } from "react";
import { AddToWatchlistModal } from './AddToWatchlistModal';
import { Watchlist } from "@/features/watchlist/types";

interface StockHeaderProps {
    market: string;
    ticker: string;
    name: string;
    isKrMarket: boolean;
    currencyMode: 'KRW' | 'USD';
    onCurrencyChange: (mode: 'KRW' | 'USD') => void;
    onRefresh: () => void;
    onBack: () => void;
    watchlists: Watchlist[];
    onWatchlistChange: () => void;
}

export function StockHeader({
    market, ticker, name, isKrMarket, currencyMode, onCurrencyChange, onRefresh, onBack, watchlists, onWatchlistChange
}: StockHeaderProps) {
    const [isWatchlistModalOpen, setIsWatchlistModalOpen] = useState(false);

    // 이 종목이 내 관심종목에 있는지 검증
    const isBookmarked = useMemo(() => {
        return watchlists.some(list => list.items?.some(item => item.ticker === ticker));
    }, [watchlists, ticker]);

    return (
        <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
                <Button variant="ghost" size="icon" onClick={onBack}>
                    <ArrowLeft className="h-5 w-5" />
                </Button>
                <div>
                    <div className="flex items-center gap-2">
                        <span className="text-sm font-bold bg-muted px-2 py-0.5 rounded text-muted-foreground">
                            {market}
                        </span>
                        <h1 className="text-2xl font-bold tracking-tight">{name || ticker}</h1>
                    </div>
                    {name && <p className="text-muted-foreground">{ticker}</p>}
                </div>
            </div>

            <div className="flex items-center gap-2">
                {/* 관심종목 모달 버튼 */}
                <Button variant="outline" size="icon" className="h-9 w-9 shrink-0" onClick={() => setIsWatchlistModalOpen(true)}>
                    <Star className={`h-4 w-4 ${isBookmarked ? 'fill-yellow-400 text-yellow-400' : 'text-muted-foreground'}`} />
                </Button>

                <AddToWatchlistModal
                    ticker={ticker}
                    name={name}
                    isOpen={isWatchlistModalOpen}
                    onOpenChange={setIsWatchlistModalOpen}
                    watchlists={watchlists}
                    onWatchlistUpdate={onWatchlistChange}
                />

                {/* 통화 전환 탭 */}
                {!isKrMarket && (
                    <Tabs value={currencyMode} onValueChange={(v) => onCurrencyChange(v as 'KRW' | 'USD')} className="w-[60px]">
                        <TabsList className="grid w-full grid-cols-2 h-9">
                            <TabsTrigger value="USD" className="font-bold text-xs">＄</TabsTrigger>
                            <TabsTrigger value="KRW" className="font-bold text-xs">원</TabsTrigger>
                        </TabsList>
                    </Tabs>
                )}

                {/* 새로고침 버튼 */}
                <Button variant="outline" size="icon" onClick={onRefresh} className="h-9 w-9 shrink-0">
                    <RefreshCcw className="h-4 w-4" />
                </Button>
            </div>
        </div>
    );
}