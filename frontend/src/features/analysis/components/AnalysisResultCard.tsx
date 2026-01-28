import React from 'react';
import { AiResponseDto } from '../types';

interface Props {
  ticker : string;
  name? : string;
  data: AiResponseDto;
}

export const AnalysisResultCard = ({ ticker, name, data }: Props) => {
  const { conclusionCode, oneLineReview, reasons, disclaimer } = data;

  const displayName = name && name !== ticker ? name : ticker;

  const showBadge = name && name !== ticker;

  const getStatusStyle = (code: string) => {
    if (code === 'COMPLEMENTARY' || code === '보완') {
          return { bg: 'bg-blue-50', border: 'border-blue-200', text: 'text-blue-700', label: '추천' };
        } else if (code === 'BURDEN' || code === '부담') {
          return { bg: 'bg-red-50', border: 'border-red-200', text: 'text-red-700', label: '투자 유의' };
        } else {
          return { bg: 'bg-gray-50', border: 'border-gray-200', text: 'text-gray-700', label: '중립' };
        }
  };

  const style = getStatusStyle(conclusionCode);

 return (
     <div className={`w-full bg-white rounded-xl border ${style.border} shadow-sm overflow-hidden mt-6`}>
       {/* 헤더 */}
       <div className={`${style.bg} px-6 py-4 border-b ${style.border} flex justify-between items-center`}>
         <div>
           <h2 className="text-2xl font-bold text-gray-900 flex items-center gap-2">
            {displayName}

            {showBadge && (
              <span className="text-sm font-medium text-gray-400 bg-gray-100 px-2 py-1 rounded">
                {ticker}
                </span>
            )}
         </h2>
         </div>
         <span className={`px-3 py-1 rounded-full text-xs font-bold bg-white/60 ${style.text}`}>
           {style.label}
         </span>
       </div>

       <div className="p-6">
         {/* 한 줄 평 */}
         <div className="mb-6">
           <h3 className="text-lg font-bold text-gray-900 mb-2">AI 투자 의견</h3>
           <p className="text-gray-800 font-medium text-lg leading-relaxed">
             "{oneLineReview}"
           </p>
         </div>

         {/* 판단 근거 리스트  */}
         <div className="bg-gray-50 rounded-lg p-5 border border-gray-100 mb-6">
           <h4 className="text-gray-700 font-semibold text-sm mb-3">주요 판단 근거</h4>
           <ul className="space-y-2">
             {reasons?.map((reason, idx) => (
               <li key={idx} className="flex items-start text-sm text-gray-600">
                 <span className={`mr-2 mt-1.5 w-1.5 h-1.5 rounded-full flex-shrink-0 ${style?.text ? style.text.replace('text-', 'bg-') : 'bg-gray-500'}`}></span>
                 {reason}
               </li>
             ))}
           </ul>
         </div>

         {/* 하단 태그 및 면책 조항 */}
         <div className="pt-4 border-t border-gray-100 flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
           <div className="flex gap-2">
             <span className="px-2 py-1 bg-gray-100 text-gray-600 text-xs rounded-md font-medium">#AI분석</span>
             <span className="px-2 py-1 bg-gray-100 text-gray-600 text-xs rounded-md font-medium">#포트폴리오</span>
           </div>
           <p className="text-xs text-gray-400 text-right">
             {disclaimer}
           </p>
         </div>
       </div>
     </div>
   );
 };