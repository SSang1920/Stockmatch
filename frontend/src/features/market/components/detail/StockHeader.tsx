import { Button } from "@/components/ui/button";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { ArrowLeft, Lock, RefreshCcw, Star } from "lucide-react";
import { useMemo, useState } from "react";
import { AddToWatchlistModal } from './AddToWatchlistModal';
import { Watchlist } from "@/features/watchlist/types";
import { useNavigate } from "@tanstack/react-router";
import { toast } from "sonner";

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
    const navigate = useNavigate();
    const [isWatchlistModalOpen, setIsWatchlistModalOpen] = useState(false);

    // 이 종목이 내 관심종목에 있는지 검증
    const isBookmarked = useMemo(() => {
        return watchlists.some(list => list.items?.some(item => item.ticker === ticker));
    }, [watchlists, ticker]);

    // 관심종목 버튼 클릭 핸들러
    const handleWatchlistClick = () => {
        const hasToken = document.cookie.includes('accessToken');

        if (!hasToken) {
            // 비로그인 토스트 메시지 출력
            toast.error("로그인이 필요한 서비스입니다.", {
                action: {
                    label: "로그인",
                    onClick: () => navigate({ to: '/sign-in' })
                },
                duration: 3000,
            });
            return;
        }

        // 로그인 상태면 모달 열기
        setIsWatchlistModalOpen(true);
    }

    return (
        <div className="relative flex items-center justify-between h-14 mb-6">
            <div className="flex items-center gap-3 overflow-hidden">
                <Button variant="ghost" size="icon" onClick={onBack} className="-ml-2 shrink-0">
                    <ArrowLeft className="h-6 w-6" />
                </Button>
                
                <div className="flex flex-col min-w-0">
                    <div className="flex items-center gap-2">
                        <span className="text-xs font-bold bg-muted px-1.5 py-0.5 rounded text-muted-foreground shrink-0">
                            {market}
                        </span>
                        <h1 className="text-xl font-bold tracking-tight truncate">
                            {name || ticker}
                        </h1>
                    </div>
                    {name && <p className="text-xs text-muted-foreground mt-0.5 truncate">{ticker}</p>}
                </div>
            </div>

            <div className="flex items-center gap-2 z-10">
                {/* 관심종목 모달 버튼 */}
                <Button variant="outline" size="icon" className="h-9 w-9 shrink-0" onClick={handleWatchlistClick}>
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