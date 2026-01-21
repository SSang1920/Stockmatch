import React, { useState } from 'react';
import { Search } from 'lucide-react';

interface Props {
  onSearch: (ticker: string) => void;
  isLoading: boolean;
}

export const AnalysisSearchInput = ({ onSearch, isLoading }: Props) => {
  const [ticker, setTicker] = useState('');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (ticker.trim()) {
      onSearch(ticker.toUpperCase());
    }
  };

  return (
    <form onSubmit={handleSubmit} className="relative flex gap-3 max-w-2xl">
      <div className="relative flex-1">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
        <input
          type="text"
          value={ticker}
          onChange={(e) => setTicker(e.target.value)}
          placeholder="티커 입력 (예: AAPL, 삼성전자)"
          className="w-full pl-10 pr-4 py-2 text-sm border rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all disabled:bg-gray-100"
          disabled={isLoading}
        />
      </div>
      <button
        type="submit"
        disabled={isLoading || !ticker.trim()}
        className="px-4 py-2 bg-gray-900 text-white text-sm font-medium rounded-md hover:bg-gray-800 disabled:bg-gray-300 transition-colors whitespace-nowrap"
      >
        {isLoading ? '분석 중...' : '분석하기'}
      </button>
    </form>
  );
};