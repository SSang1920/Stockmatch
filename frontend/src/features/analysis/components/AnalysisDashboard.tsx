import React, { useState } from 'react';
import { Sparkles, SearchX, History, X } from 'lucide-react';
import { AnalysisSearchInput } from './AnalysisSearchInput';
import { AnalysisResultCard } from './AnalysisResultCard';
import { AiResponseDto, ApiResponse, AnalysisHistoryResponse } from '../types';
import { MOCK_ANALYSIS_RESULT } from '../api/mockData';
import { fetchAiAnalysis, fetchUserHistoryList, fetchHistoryDetail } from '../api/analysis';

export const AnalysisDashboard = () => {
  const [isLoading, setIsLoading] = useState(false);
  const [result, setResult] = useState<AiResponseDto | null>(null);
  const [searchTicker, setSearchTicker] = useState<string>(''); // 현재 보여지는 티커
  const [searchName, setSearchName] = useState<string>('');
  const [error, setError] = useState<string | null>(null);

  const [history, setHistory] = useState<AnalysisHistoryListResponse[]>([]);
  const [isDrawerOpen, setIsDrawerOpen] = useState(false);

const handleOpenHistory = async () => {
    try {
      const response = await fetchUserHistoryList();
      if (response && response.data) {
        setHistory(response.data);
        setIsDrawerOpen(true);
    }
    } catch (err) {
      console.error("히스토리 로딩 실패", err);
    }
  };

  // 2. 히스토리 항목 클릭 시 메인 결과 업데이트
  const handleHistoryItemClick = async (item: AnalysisHistoryListResponse) => {
    try {
        setIsLoading(true); // 상세 데이터를 가져오는 동안 로딩 표시

        // 개별 상세 조회 API 호출
        const response = await fetchHistoryDetail(item.id);
        if (response && response.data) {
          setResult(response.data);      // 상세 분석 결과(AiResponseDto) 세팅
          setSearchTicker(item.symbol);  // 티커 세팅
          setSearchName('');             // 필요 시 이름 필드 초기화
          setIsDrawerOpen(false);        // 사이드바 닫기
        }
      } catch (err) {
        console.error("상세 내역 로딩 실패", err);
        alert("상세 데이터를 불러오지 못했습니다.");
      } finally {
        setIsLoading(false);
      }
  };

  const handleSearch = async (inputTicker: string, inputName?: string) => {
    //  상태 초기화
    setIsLoading(true);
    setResult(null);
    setError(null);
    setSearchTicker(inputTicker);
    setSearchName(inputName || '');

    try {
      const response = await fetchAiAnalysis(inputTicker);

      if(response && response.data) {
          setResult(response.data);
          } else {
              throw new Error("데이터 형식이 올바르지 않습니다.");
          }


    } catch (err: any) {
      console.error("API Error:", err);

      // 에러 상황별 메시지 처리
      if (err.response) {
        if (err.response.status === 401) {
            setError("로그인 세션이 만료되었습니다. 다시 로그인해주세요.");
        } else if (err.response.status === 404) {
            setError(`'${inputTicker}'에 대한 정보를 찾을 수 없습니다.`);
        } else {
            setError("서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
        }
      }
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="relative space-y-6">
      {/* 헤더 */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
            <Sparkles className="h-6 w-6 text-blue-600" />
            <h1 className="text-2xl font-bold text-gray-900">포트폴리오 맞춤 분석</h1>
        </div>
        <button
            onClick={handleOpenHistory}
            className="flex items-center gap-2 px-3 py-2 text-sm font-medium text-gray-600 hover:text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
            >
            <History className="h-5 w-5" />
            분석 기록
        </button>
    </div>

      {/* 메인 박스 */}
      <div className="rounded-xl border bg-white text-card-foreground shadow-sm min-h-[500px]">
        <div className="p-6">
          <div className="mb-8">
            <h2 className="text-lg font-semibold mb-1">포트폴리오 맞춤 분석 AI 서비스</h2>
            <p className="text-sm text-gray-500">
              보유 중인 포트폴리오와 시장 데이터를 결합하여 최적의 투자의견을 제시합니다.
            </p>
          </div>

          {/* 검색 입력창 */}
          <div className="mb-8">
             <AnalysisSearchInput onSearch={handleSearch} isLoading={isLoading} />
          </div>

          {/*  로딩 화면 */}
          {isLoading && (
            <div className="py-20 text-center border-t border-gray-100 animate-in fade-in">
              <div className="inline-block w-10 h-10 border-4 border-gray-100 border-t-blue-600 rounded-full animate-spin mb-6"></div>
              <h3 className="text-lg font-semibold text-gray-900">AI가 분석 중입니다...</h3>
              <p className="text-sm text-gray-500 mt-2">최대 1~2분 정도 소요될 수 있습니다.</p>
            </div>
          )}

          {/* 에러 화면 */}
          {!isLoading && error && (
             <div className="py-20 text-center border-t border-gray-100 animate-in fade-in">
                <div className="inline-flex items-center justify-center w-16 h-16 rounded-full bg-red-50 mb-6">
                    <SearchX className="h-8 w-8 text-red-400" />
                </div>
                <h3 className="text-lg font-bold text-gray-900 mb-2">분석 실패</h3>
                <p className="text-gray-500 max-w-sm mx-auto">{error}</p>
             </div>
          )}

          {/*성공 결과 화면 */}
          {!isLoading && !error && result && (
            <div className="animate-in fade-in slide-in-from-bottom-4 duration-500 mt-6 pt-6 border-t border-gray-100">
              <AnalysisResultCard ticker={searchTicker} name={searchName} data={result} />
            </div>
          )}

          {/*  초기 예시 화면 */}
          {!isLoading && !error && !result && (
              <div className="mt-8 pt-8 border-t border-gray-100">
                  <div className="flex items-center justify-between mb-4 px-2">
                      <span className="text-sm font-bold text-gray-500">검색 예시 화면</span>
                      <span className="text-xs text-blue-600 bg-blue-50 px-2 py-1 rounded font-medium">Example</span>
                  </div>
                  <div className="opacity-100 scale-100">
                      <AnalysisResultCard ticker="AAPL" data={MOCK_ANALYSIS_RESULT} />
                  </div>
              </div>
          )}
        </div>
      </div>
      {isDrawerOpen && (
              <>
                 {/* 사이드바 본체: 우측 고정 슬라이드 애니메이션 적용 */}
                <div className="fixed right-0 top-0 h-full w-80 bg-white shadow-2xl z-50 p-6 flex flex-col animate-in slide-in-from-right duration-300">

                  {/* 사이드바 헤더 */}
                  <div className="flex items-center justify-between mb-6 border-b pb-4">
                    <h3 className="text-lg font-bold flex items-center gap-2">
                      <History className="h-5 w-5 text-blue-600" /> 분석 기록
                    </h3>
                    <button onClick={() => setIsDrawerOpen(false)} className="p-1 hover:bg-gray-100 rounded-full">
                      <X className="h-6 w-6 text-gray-400" />
                    </button>
                  </div>

                  {/* 히스토리 리스트 영역 (스크롤 가능) */}
                  <div className="flex-1 overflow-y-auto space-y-3 pr-2">
                    {history.length === 0 ? (
                      <div className="py-20 text-center text-gray-400">
                        <p className="text-sm">저장된 기록이 없습니다.</p>
                      </div>
                    ) : (
                      history.map((item) => (
                        <div
                          key={item.id}
                          onClick={() => handleHistoryItemClick(item)} // 3. [로직] 클릭 시 결과 업데이트
                          className="p-4 border rounded-xl hover:border-blue-500 hover:bg-blue-50 cursor-pointer transition-all duration-200"
                        >
                          <div className="flex justify-between items-start mb-2">
                            <span className="font-bold text-gray-900 group-hover:text-blue-700 text-sm">
                               {item.symbol}
                            </span>
                            <span className="text-[10px] text-gray-400">
                              {item.analyzedAt ? new Date(item.analyzedAt.toString()).toLocaleDateString() : '날짜 없음'}
                            </span>
                          </div>
                        </div>
                      ))
                    )}
                  </div>
                </div>
              </>
            )}
    </div>
  );
};