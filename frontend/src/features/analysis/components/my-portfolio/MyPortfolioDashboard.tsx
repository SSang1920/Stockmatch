import React, { useState, useRef, useEffect } from 'react';
import { Sparkles, SearchX, History, X, Send, Lightbulb ,RefreshCw } from 'lucide-react';
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip, Legend } from 'recharts';
import { AnalysisLayout } from '../common/AnalysisLayout';
import { MOCK_PORTFOLIO_ANALYSIS } from '../../api/mockData';
import { PortfolioAnalysisResponse, AnalysisHistoryListResponse } from '../../types';
import { fetchUserHistoryList,fetchHistoryDetail, fetchAiPortfolioAnalysis } from '../../api/analysis';
import { PortfolioResultCard } from './PortfolioResultCard';

const COLORS = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6'];

export const MyPortfolioDashboard = () => {
    const [isLoading, setIsLoading] = useState(false);
    const [result, setResult] = useState<PortfolioAnalysisResponse | null>(null);
    const [userComment, setUserComment] = useState("");
    const [isHistoryMode, setIsHistoryMode] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [history, setHistory] = useState<AnalysisHistoryListResponse[]>([]);
    const [isDrawerOpen, setIsDrawerOpen] = useState(false);
    const textareaRef = useRef<HTMLTextAreaElement>(null);

    const handleOpenHistory = async () => {
        try {
            const response = await fetchUserHistoryList();
            if (response && response.data) {
                const portfolioHistory = response.data.filter(item => item.type === 'PORTFOLIO');
                setHistory(portfolioHistory);
                setIsDrawerOpen(true);
            }
        } catch (err) {
                console.error("히스토리 로딩 실패", err);
          }
    };

      //  히스토리 항목 클릭 시 메인 결과 업데이트
    const handleHistoryItemClick = async (item: AnalysisHistoryListResponse) => {
        try {
            setIsLoading(true); // 상세 데이터를 가져오는 동안 로딩 표시
            const response = await fetchHistoryDetail(item.id)        // 개별 상세 조회 API 호출

            if (response && response.data) {
              setResult(response.data);      // 상세 분석 결과(AiResponseDto) 세팅

              const pastComment = item.userComment || "";
                        setUserComment(pastComment);
                        setIsHistoryMode(true);
              setIsDrawerOpen(false);        // 사이드바 닫기
            }
          } catch (err) {
            console.error("상세 내역 로딩 실패", err);
            alert("상세 데이터를 불러오지 못했습니다.");
          } finally {
            setIsLoading(false);
          }
      };

    useEffect(() => {
      if (textareaRef.current) {
        textareaRef.current.style.height = 'auto';
        // 현재 안의 텍스트 콘텐츠가 차지하는 실제 높이를 구함
        const scrollHeight = textareaRef.current.scrollHeight;
        // 구한 높이를 적용
        textareaRef.current.style.height = `${scrollHeight}px`;
      }
    }, [userComment]); // userComment가 변경될 때마다 실행

    const handleReset = () => {
      setResult(null);        // 예시 화면으로 롤백
      setUserComment("");     // 입력창 비우기
      setError(null);         // 에러 메시지 비우기
      setIsHistoryMode(false); // 히스토리 모드 해제

      // 텍스트 영역 높이 초기화
      if (textareaRef.current) {
        textareaRef.current.style.height = 'auto';
      }
    };

    //높이 자동 조절 핸들러
    const handleTextareaChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
          setUserComment(e.target.value);
          setIsHistoryMode(false);

    };

    const handleAnalyze = async (comment : string) => {
        if (!comment.trim()) return;

        setIsLoading(true);
        setResult(null);
        setError(null);

        try {
          const response = await fetchAiPortfolioAnalysis(comment);

          if(response && response.data) {
              setResult(response.data);
              } else {
                  throw new Error("데이터 형식이 올바르지 않습니다.");
              }


        } catch (err: any) {
          console.error("API Error:", err);

          // 에러 상황별 메시지 처리
          if (err.response) {
              const serverMessage = err.response.data?.error?.message;
            if (err.response.status === 401) {
                setError("로그인 세션이 만료되었습니다. 다시 로그인해주세요.");
           } else if(serverMessage){
               setError(serverMessage);
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
            <h1 className="text-2xl font-bold text-gray-900">내 포트폴리오 추천</h1>
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
      <AnalysisLayout
        title="내 포트폴리오 분석"
        description="내 포트폴리오 자산현황을 분석하여 보완점을 제시합니다."
      >

      {/* 입력창 */}
      <div className="mb-8 flex gap-3">
        <textarea
          ref={textareaRef}
          value={userComment}
          onChange={handleTextareaChange}
          readOnly={isHistoryMode || isLoading}
          placeholder="포트폴리오에 대해 궁금한 점을 입력하세요 (예: 기술주 비중이 너무 높은가요?)"
          rows={1}
          className="flex-1 px-4 py-3 rounded-xl border border-gray-200 focus:ring-2 focus:ring-blue-500 outline-none transition-all resize-none min-h-[50px] max-h-[200px] overflow-y-auto leading-normal"
        />
        <button
          onClick={() => {
              handleAnalyze(userComment);
        }}
          disabled={isLoading || isHistoryMode || !userComment.trim()}
            className={`px-6 py-3 h-[50px] rounded-xl font-bold flex items-center justify-center gap-2 transition-colors whitespace-nowrap shrink-0 ${
              isHistoryMode
                ? 'bg-gray-100 text-gray-400 cursor-not-allowed' // 히스토리 모드일 때 스타일
                : 'bg-blue-600 text-white hover:bg-blue-700 disabled:bg-gray-400'
            }`}
          >
            <Send className="h-4 w-4" />
            {isHistoryMode ? '분석 완료' : '분석 요청'}
          </button>

          {(result || error || userComment.length > 0 || isHistoryMode) && (
            <button
              onClick={handleReset}
              disabled={isLoading} // 로딩 중에는 초기화 버튼 막기
              className="px-4 py-3 h-[50px] rounded-xl font-bold flex items-center justify-center gap-2 transition-all whitespace-nowrap bg-white border border-gray-300 text-gray-700 hover:bg-red-50 hover:text-red-600 hover:border-red-200 disabled:opacity-50"
              title="초기화"
            >
              <RefreshCw className="h-4 w-4" />
              <span className="hidden sm:inline">초기화</span>
            </button>
          )}
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
          <PortfolioResultCard data={result} />
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
                  <PortfolioResultCard data={MOCK_PORTFOLIO_ANALYSIS} />
              </div>
          </div>
      )}
      </AnalysisLayout>
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
                       {item.symbol || '포트폴리오 분석'}
                    </span>
                    <span className="text-[10px] text-gray-400">
                      {item.analyzedAt ? new Date(item.analyzedAt.toString()).toLocaleDateString() : '날짜 없음'}
                    </span>
                  </div>
                  {item.userComment && (
                    <p className="text-xs text-gray-500 mt-1 truncate" title={item.userComment}>
                      <span className="font-medium text-blue-500 mr-1">Q.</span>
                      {item.userComment}
                    </p>
                  )}
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