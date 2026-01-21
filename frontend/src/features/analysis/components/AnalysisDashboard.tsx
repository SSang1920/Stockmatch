import React, { useState } from 'react';
import { Sparkles, SearchX } from 'lucide-react';
import { AnalysisSearchInput } from './AnalysisSearchInput';
import { AnalysisResultCard } from './AnalysisResultCard';
import { AnalysisResponse } from '../types';
import { MOCK_ANALYSIS_RESULT } from '../api/mockData';

export const AnalysisDashboard = () => {
  const [isLoading, setIsLoading] = useState(false);
  const [result, setResult] = useState<AnalysisResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  const handleSearch = async (inputTicker: string) => {
    // 1. 검색 시작: 상태 초기화 (로딩 시작, 기존 결과/에러 삭제)
    setIsLoading(true);
    setResult(null);
    setError(null);

    // --- [여기서부터가 실제 로직이 들어갈 자리입니다] ---

    // 지금은 API가 없으니 setTimeout으로 흉내만 냅니다.
    // 나중에 이 부분을 const response = await axios.get(...) 으로 바꾸면 됩니다.
    setTimeout(() => {

      const mockDatabase = ['AAPL', 'NVDA', 'TSLA', 'MSFT', '삼성전자'];

      // 2. 데이터 조회 시도
      const dataFound = mockDatabase.includes(inputTicker); // 실제 코드: if (response.data)

      if (!dataFound) {

        setError(`'${inputTicker}'에 대한 분석 데이터가 존재하지 않습니다.`);
        setIsLoading(false);
        return;
      }


      const mockResponse = { ...MOCK_ANALYSIS_RESULT, ticker: inputTicker };
      setResult(mockResponse);
      setIsLoading(false);

    }, 1500); // 1.5초 딜레이
  };

  return (
    <div className="space-y-6">
      {/* 헤더 */}
      <div className="flex items-center gap-2">
        <Sparkles className="h-6 w-6 text-blue-600" />
        <h1 className="text-2xl font-bold text-gray-900">포트폴리오 맞춤 분석</h1>
      </div>

      {/* 메인 컨텐츠 박스 */}
      <div className="rounded-xl border bg-white text-card-foreground shadow-sm min-h-[500px]">
        <div className="p-6">

          <div className="mb-8">
            <h2 className="text-lg font-semibold mb-1">포트폴리오 맞춤 분석 AI 서비스</h2>
            <p className="text-sm text-gray-500">
              보유 중인 포트폴리오와 시장 데이터를 결합하여 최적의 투자의견을 제시합니다.
            </p>
          </div>

          {/* 검색창 */}
          <div className="mb-8">
             <AnalysisSearchInput onSearch={handleSearch} isLoading={isLoading} />
          </div>

          {/* --- [상태에 따른 화면 전환] --- */}

          {/* (1) 로딩 중 */}
          {isLoading && (
            <div className="py-20 text-center border-t border-gray-100 animate-in fade-in">
              <div className="inline-block w-10 h-10 border-4 border-gray-100 border-t-blue-600 rounded-full animate-spin mb-6"></div>
              <h3 className="text-lg font-semibold text-gray-900">AI가 분석 중입니다...</h3>
              <p className="text-sm text-gray-500 mt-2">서버에서 최신 데이터를 조회하고 있습니다.</p>
            </div>
          )}

          {/* (2) 실패: 데이터가 Null일 때 (검색은 했으나 결과가 없음) */}
          {!isLoading && error && (
             <div className="py-20 text-center border-t border-gray-100 animate-in fade-in slide-in-from-bottom-2">
                <div className="inline-flex items-center justify-center w-16 h-16 rounded-full bg-red-50 mb-6">
                    <SearchX className="h-8 w-8 text-red-400" />
                </div>
                <h3 className="text-lg font-bold text-gray-900 mb-2">데이터를 찾을 수 없습니다</h3>
                <p className="text-gray-500 max-w-sm mx-auto mb-6">
                    {error}
                </p>
                <div className="bg-gray-50 rounded-lg p-4 text-sm text-gray-600 inline-block text-left">
                    <p className="font-bold mb-2">💡 확인해주세요:</p>
                    <ul className="list-disc list-inside space-y-1">
                        <li>티커(심볼) 철자가 정확한지 확인하세요. (예: GOOG)</li>
                        <li>지원하지 않는 종목이거나 상장 폐지된 종목일 수 있습니다.</li>
                    </ul>
                </div>
             </div>
          )}

          {/* (3) 성공: 데이터가 있을 때 */}
          {!isLoading && !error && result && (
            <div className="animate-in fade-in slide-in-from-bottom-4 duration-500 mt-6 pt-6 border-t border-gray-100">
              <div className="mb-4 flex items-center gap-2">
                 <span className="bg-blue-100 text-blue-700 text-xs font-bold px-2.5 py-1 rounded-md">Analysis Complete</span>
                 <p className="text-sm text-gray-600">요청하신 종목 분석이 완료되었습니다.</p>
              </div>
              <AnalysisResultCard data={result} />
            </div>
          )}

          {/* (4) 초기 상태: 아무것도 안 했을 때 (예시 보여주기) */}
          {!isLoading && !error && !result && (
            <div className="mt-12 py-12 border-t border-dashed border-gray-200 text-center opacity-60">
                <p className="text-sm font-medium text-gray-400 mb-2">검색 예시 화면</p>
                <div className="pointer-events-none select-none grayscale blur-[1px] scale-95 origin-top">
                    <AnalysisResultCard data={{...MOCK_ANALYSIS_RESULT, ticker: 'AAPL'}} />
                </div>
            </div>
          )}

        </div>
      </div>
    </div>
  );
};