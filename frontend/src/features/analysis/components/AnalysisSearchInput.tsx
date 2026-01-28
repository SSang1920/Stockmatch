import { useEffect, useRef, useState } from "react"
import { StockSearchResponse } from "../types";
import { searchStocks } from "../../market/api/marketApi";

interface Props {
    onSearch : (ticker: string, name?: string) =>void;
    isLoading?:boolean;
    }

export const AnalysisSearchInput = ({ onSearch, isLoading } : Props) => {
    const [query, setQuery] = useState('');
    const [results, setResults] = useState<StockSearchResponse[]>([]);
    const [isOpen, setIsOpen] = useState(false);
    const [recentSearches, setRecentSearches] = useState<StockSearchResponse[]>([]);
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
            if (query.trim().length >= 1 && !query.includes('(')) {     // 1글자 이상일 때만 검색
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

        setQuery(`${stock.ticker} (${stock.name})`);
        setIsOpen(false);
        onSearch(stock.ticker, stock.name);
    }
    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        if (query.trim()) {
            let tickerToSend = query;
            let nameToSend = '';

            if (query.includes('(')) {
                const parts = query.split('(');
                tickerToSend = parts[0].trim().toUpperCase();
                nameToSend = parts[1].replace(')', '').trim();
            } else {
                tickerToSend = query.trim().toUpperCase();
            }

            setIsOpen(false);
            onSearch(tickerToSend, nameToSend);
        }
    }

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
           <form onSubmit={handleSubmit} className="relative">
               <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                   <svg className="h-5 w-5 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                       <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/>
                   </svg>
               </div>

               <input
                   type="text"
                   className="block w-full pl-10 pr-4 py-3 bg-white border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent shadow-sm text-sm disabled:bg-gray-100"
                   placeholder="예: AAPL, 삼성전자, NVDA"
                   value={query}
                   onChange={(e) => setQuery(e.target.value)}
                   onFocus={() => query && setIsOpen(true)}
                   disabled={isLoading} //로딩 중 입력 비활성화
               />
           </form>

           {!query && recentSearches.length > 0 && (
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

           {isOpen && results.length > 0 && (
               <ul className="absolute z-10 w-full mt-1 bg-white border border-gray-200 rounded-lg shadow-lg max-h-96 overflow-y-auto">
                   {results.map((stock) => (
                       <li
                           key={stock.ticker}
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

           {isOpen && query.length > 0 && results.length === 0 && (
               <div className="absolute z-10 w-full mt-1 bg-white border border-gray-200 rounded-lg shadow-lg p-4 text-center text-gray-500">
                   검색 결과가 없습니다.
               </div>
           )}
       </div>
   );
}