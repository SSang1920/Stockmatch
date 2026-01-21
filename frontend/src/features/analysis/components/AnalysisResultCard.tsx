import React from 'react';
import { AnalysisResponse, AiRecommendation } from '../types';

interface Props {
  data: AnalysisResponse;
}

export const AnalysisResultCard = ({ data }: Props) => {
  const { recommendation, ticker } = data;

  const getStatusStyle = (type: AiRecommendation['type']) => {
    switch (type) {
      case 'STRONGLY_RECOMMENDED':
        return { bg: 'bg-green-50', border: 'border-green-200', text: 'text-green-700', label: '강력 추천' };
      case 'RECOMMENDED':
        return { bg: 'bg-blue-50', border: 'border-blue-200', text: 'text-blue-700', label: '매수 추천' };
      case 'NEUTRAL':
        return { bg: 'bg-gray-50', border: 'border-gray-200', text: 'text-gray-700', label: '중립' };
      case 'NOT_RECOMMENDED':
        return { bg: 'bg-orange-50', border: 'border-orange-200', text: 'text-orange-700', label: '비추천' };
      case 'WARNING':
        return { bg: 'bg-red-50', border: 'border-red-200', text: 'text-red-700', label: '위험' };
      default:
        return { bg: 'bg-gray-50', border: 'border-gray-200', text: 'text-gray-700', label: '알 수 없음' };
    }
  };

  const style = getStatusStyle(recommendation.type);

  return (
    <div className={`w-full bg-white rounded-xl border ${style.border} shadow-sm overflow-hidden mt-6`}>
      <div className={`${style.bg} px-6 py-4 border-b ${style.border} flex justify-between items-center`}>
        <div>
          <h2 className="text-xl font-bold text-gray-900">{ticker}</h2>
          <span className="text-xs text-gray-500">포트폴리오 맞춤 분석</span>
        </div>
        <span className={`px-3 py-1 rounded-full text-xs font-bold bg-white/60 ${style.text}`}>
          {style.label}
        </span>
      </div>

      <div className="p-6">
        <div className="mb-6">
          <h3 className="text-lg font-bold text-gray-900 mb-2">{recommendation.title}</h3>
          <p className="text-gray-700 leading-relaxed whitespace-pre-wrap text-sm">
            {recommendation.reasoning}
          </p>
        </div>

        {recommendation.riskFactors.length > 0 && (
          <div className="bg-red-50 rounded-lg p-4 border border-red-100 mb-6">
            <h4 className="text-red-800 font-semibold text-sm mb-2">주요 리스크 요인</h4>
            <ul className="list-disc list-inside space-y-1 text-sm text-red-700">
              {recommendation.riskFactors.map((risk, idx) => (
                <li key={idx}>{risk}</li>
              ))}
            </ul>
          </div>
        )}

        <div className="pt-4 border-t border-gray-100">
          <span className="text-xs font-bold text-gray-500 uppercase tracking-wider mb-2 block">
            참고 데이터
          </span>
          <div className="flex flex-wrap gap-2">
            {recommendation.references.map((ref, idx) => (
              <span key={idx} className="px-2 py-1 bg-gray-100 text-gray-600 text-xs rounded-md font-medium">
                #{ref}
              </span>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};