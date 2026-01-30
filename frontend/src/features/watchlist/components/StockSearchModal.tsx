import { StockSearchResponse } from "@/features/market/types";
import { useEffect, useState } from "react";
import * as stockApi from '@/features/market/api/marketApi'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Loader2, Plus, Search } from "lucide-react";
import { Input } from "@/components/ui/input";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Button } from "@/components/ui/button";

interface Props {
    open: boolean;
    onOpenChange: (open: boolean) => void;
    onAdd: (ticker: string) => Promise<void>;
}

export function StockSearchModal({ open, onOpenChange, onAdd }: Props) {
    const [query, setQuery] = useState('');
    const [results, setResults] = useState<StockSearchResponse[]>([]);
    const [loading, setLoading] = useState(false);

    // 검색어 입력 시 API 호출
    useEffect(() => {
        const timer = setTimeout(async () => {
            if (query.trim().length < 1) {
                setResults([]);
                return;
            }

            setLoading(true);
            try {
                const data = await stockApi.searchStocks(query);
                setResults(data);
            } catch (error) {
                console.error("검색 실패: ", error);
            } finally {
                setLoading(false);
            }
        }, 300);

        return () => clearTimeout(timer);
    }, [query]);

    // 모달 닫힐 때 초기화
    useEffect(() => {
        if (!open) {
            setQuery('');
            setResults([]);
        }
    }, [open]);

    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent className="sm:max-w-[500px]">
                <DialogHeader>
                    <DialogTitle>종목 검색</DialogTitle>
                </DialogHeader>

                <div className="space-y-4 py-2">
                    {/* 검색 입력창 */}
                    <div className="relative">
                        <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
                        <Input
                            placeholder="종목명(삼성전자, Apple) 또는 티커(005930, AAPL) 검색"
                            className="pl-9"
                            value={query}
                            onChange={(e) => setQuery(e.target.value)}
                            autoFocus // 모달 열리면 바로 포커스
                        />
                    </div>

                    {/* 검색 결과 리스트 */}
                    <ScrollArea className="h-[300px] border rounded-md p-2">
                        {loading ? (
                            <div className="flex justify-center p-4">
                                <Loader2 className="animate-spin h-6 w-6 text-primary" />
                            </div>
                        ) : results.length === 0 ? (
                            <div className="text-center text-sm text-muted-foreground p-4">
                                {query.length < 1 ? "검색어를 입력하세요." : "검색 결과가 없습니다."}
                            </div>
                        ) : (
                            <div className="space-y-1">
                                {results.map((stock) => (
                                    <div key={stock.id} className="flex items-center justify-between p-2 hover:bg-muted rounded-md transition-colors">
                                        <div className="flex flex-col">
                                            <div className="font-bold flex items-center gap-2">
                                                {stock.ticker}
                                                <span className={`text-[10px] px-1.5 rounded text-white ${stock.market === 'KOSPI' || stock.market === 'KOSDAQ'
                                                        ? 'bg-blue-500' // 국내장은 파란색 뱃지 (예시)
                                                        : 'bg-orange-500' // 미장은 주황색 뱃지
                                                    }`}>
                                                    {stock.market}
                                                </span>
                                            </div>
                                            <div className="text-sm text-muted-foreground">
                                                {stock.name}
                                                {stock.englishName && <span className="ml-1 text-xs text-muted-foreground/70">({stock.englishName})</span>}
                                            </div>
                                        </div>

                                        <Button size="sm" variant="ghost" onClick={() => {
                                            onAdd(stock.ticker);
                                            // 검색어 초기화는 선택사항 (연속 추가를 위해 유지할 수도 있음)
                                        }}>
                                            <Plus className="h-4 w-4" />
                                        </Button>
                                    </div>
                                ))}
                            </div>
                        )}
                    </ScrollArea>
                </div>
            </DialogContent>
        </Dialog>
    );
}