import { useEffect, useRef, useState } from "react"
import { StockSearchResponse } from "../types";
import { useNavigate } from "@tanstack/react-router";
import { searchStocks } from "../api/stockApi";

interface StockSearchBarProps {
    onSelectStock?: (stock: StockSearchResponse) => void;
    hideRecent?: boolean;
}

export const StockSearchBar = ({ onSelectStock, hideRecent = false }: StockSearchBarProps) => {
    const [query, setQuery] = useState('');
    const [results, setResults] = useState<StockSearchResponse[]>([]);
    const [isOpen, setIsOpen] = useState(false);
    const [recentSearches, setRecentSearches] = useState<StockSearchResponse[]>([]);
    const navigate = useNavigate();
    const wrapperRef = useRef<HTMLDivElement>(null);

    // LocalStorage에서 최근 검색어 불러오기
    useEffect(() => {
        const saved = localStorage.getItem('recentStocks');
        if (saved) {
            try {
                setRecentSearches(JSON.parse(saved));
            } catch (e) {
                console.error('Failed to parse recent searches', e);
            }
        }
    }, []);

    // 최근 검색어 추가 함수 (중복 제거 + 최대 5개 제한)
    const addToRecent = (stock: StockSearchResponse) => {
        const prev = [...recentSearches];

        // 중복 방지
        const filtered = prev.filter((item) => item.ticker !== stock.ticker);

        // 새 종목 맨 앞 추가 및 최대 5개 제한
        const newRecent = [stock, ...filtered].slice(0, 5);

        setRecentSearches(newRecent);
        localStorage.setItem('recentStocks', JSON.stringify(newRecent));
    };

    // 최근 검색어 개별 삭제 함수
    const removeRecent = (e: React.MouseEvent, ticker: string) => {
        e.stopPropagation();
        const newRecent = recentSearches.filter((item) => item.ticker != ticker);
        setRecentSearches(newRecent);
        localStorage.setItem('recentStocks', JSON.stringify(newRecent));
    };

    // 입력값이 바뀔 때마다 검색
    useEffect(() => {
        const timer = setTimeout(async () => {
            if (query.trim().length >= 1) {     // 1글자 이상일 때만 검색
                try {
                    const data = await searchStocks(query);
                    setResults(data);
                    setIsOpen(true);
                } catch (e) {
                    console.error(e);
                    setResults([]);
                }
            } else {
                setResults([]);
                setIsOpen(false);
            }
        }, 300);    // 300ms 딜레이

        return () => clearTimeout(timer);
    }, [query]);

    // 검색 결과 클릭 시 상세 페이지로 이동
    const handleSelect = (stock: StockSearchResponse) => {
        addToRecent(stock);     // 최근 검색어 저장

        if (onSelectStock) {
            onSelectStock(stock);
        } else {
            navigate({
                to: '/stocks/$market/$ticker',
                params: {
                    market: stock.market,
                    ticker: stock.ticker
                },
            });
        }

        setIsOpen(false);
        setQuery('');   // 검색창 초기화
    }

    // 검색창에서 엔더키 누르거나 없는 주식 강제로 검색할 때 발동되는 핸들러
    const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
        if (e.key === 'Enter') {
            const cleanQuery = query.trim().toUpperCase();
            if (!cleanQuery) return;

            // 만약 검색 결과 리스트에 입력한 티커와 완벽히 일치하는 자산이 있다면, 그 자산을 정상 선택 처리
            const exactMatch = results.find(s => s.ticker.toUpperCase() === cleanQuery);
            if (exactMatch) {
                handleSelect(exactMatch);
                return;
            }

            const dummyStock: StockSearchResponse = {
                id: 0,
                ticker: cleanQuery,
                name: cleanQuery,
                englishName: cleanQuery,
                market: 'US',
                exchange: 'NASDAQ'
            };

            addToRecent(dummyStock);


            navigate({
                to: '/stocks/$market/$ticker',
                params: {
                    market: 'US',
                    ticker: cleanQuery
                }
            });

            setIsOpen(false);
            setQuery('');
        }
    };

    // 외부 클릭 시 드롭다운 닫기
    useEffect(() => {
        const handleClickOutside = (event: MouseEvent) => {
            if (wrapperRef.current && !wrapperRef.current.contains(event.target as Node)) {
                setIsOpen(false);
            }
        };

        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    return (
        <div ref={wrapperRef} className="relative w-full">
            {/* 돋보기 아이콘 */}
            <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                    <svg
                        className="h-5 w-5 text-gray-400"
                        fill="none"
                        viewBox="0 0 24 24"
                        stroke="currentColor"
                    >
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                    </svg>
                </div>

                {/* 입력창 */}
                <input
                    type="text"
                    className="block w-full pl-10 pr-4 py-3 bg-white border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent shadow-sm text-sm"
                    placeholder="예: AAPL, 삼성전자, NVDA"
                    value={query}
                    onChange={(e) => setQuery(e.target.value)}
                    onFocus={() => query && setIsOpen(true)}
                    onKeyDown={handleKeyDown}
                />
            </div>

            {/* 최근 검색어 영역 */}
            {!hideRecent && !query && recentSearches.length > 0 && (
                <div className="mt-3 flex flex-wrap gap-2 items-center">
                    <span className="text-xs text-gray-500 mr-1">최근 검색:</span>
                    {recentSearches.map((stock) => (
                        <div
                            key={stock.ticker}
                            onClick={() => handleSelect(stock)}
                            className="flex items-center gap-1 px-2.5 py-1 bg-gray-100 hover:bg-gray-200 text-gray-700 text-xs rounded-full cursor-pointer transition-colors"
                        >
                            <span>{stock.name}</span>
                            <button
                                onClick={(e) => removeRecent(e, stock.ticker)}
                                className="hover:text-red-500 focus:outline-none"
                            >X</button>
                        </div>
                    ))}
                </div>
            )}

            {/* 드롭다운 결과 목록 */}
            {isOpen && results.length > 0 && (
                <ul className="absolute z-10 w-full mt-1 bg-white border border-gray-200 rounded-lg shadow-lg max-h-96 overflow-y-auto">
                    {results.map((stock) => (
                        <li
                            key={stock.id}
                            onClick={() => handleSelect(stock)}
                            className="px-4 py-3 hover:bg-gray-50 cursor-pointer border-b last:border-b-0 flex justify-between items-center group"
                        >
                            <div>
                                <div className="font-bold text-gray-900">
                                    {stock.ticker}
                                    <span className="ml-2 text-xs text-gray-500 bg-gray-100 px-2 py-0.5 rounded">
                                        {stock.market}
                                    </span>
                                </div>
                                <div className="text-sm text-gray-600">
                                    {stock.name} <span className="text-gray-400 text-xs">| {stock.englishName}</span>
                                </div>
                            </div>
                            <span className="text-gray-400 group-hover:text-blue-500 text-sm">
                                →
                            </span>
                        </li>
                    ))}
                </ul>
            )}

            {/* 검색 결과 없음 표시 */}
            {isOpen && query.length > 0 && results.length === 0 && (
    <div
        onClick={() => {
            const cleanQuery = query.trim().toUpperCase();
            if (!cleanQuery) return;

            const dummyStock: StockSearchResponse = {
                id: 0,
                ticker: cleanQuery,
                name: cleanQuery,
                englishName: cleanQuery,
                market: 'US',
                exchange: 'NASDAQ'
            };

            addToRecent(dummyStock);

            navigate({
                to: '/stocks/$market/$ticker',
                params: { market: 'US', ticker: cleanQuery }
            });

            setIsOpen(false);
            setQuery('');
        }}
        className="absolute z-10 w-full mt-1 bg-white border border-gray-200 rounded-lg shadow-lg p-4 text-center text-gray-500 hover:bg-gray-50 hover:text-blue-500 cursor-pointer transition-all border-dashed border-2"
    >
        <div className="font-semibold">"{query.toUpperCase()}" 결과가 없습니다.</div>
        <div className="text-xs text-gray-400 mt-1">이곳을 클릭하거나 Enter를 동기화를 시작합니다.</div>
    </div>
)}
        </div>
    );
}