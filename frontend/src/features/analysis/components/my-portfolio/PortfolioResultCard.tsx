import React from 'react';
import { PortfolioAnalysisResponse } from '../../types';
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip, Legend } from 'recharts';

const COLORS = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6'];

interface Props {
  data: PortfolioAnalysisResponse;
}

export const PortfolioResultCard = ({ data }: Props) => {
  const { conclusionCode, oneLineReview, detailedAnalysis, currentHoldings, disclaimer } = data;

  const getStatusStyle = (code: string) => {
    switch (code) {
        case 'WELL_BALANCED':
          return {
            bg: 'bg-blue-50',
            border: 'border-blue-200',
            text: 'text-blue-700',
            label: '분산 양호'
          };

        case 'CONCENTRATED':
          return {
            bg: 'bg-orange-50',
            border: 'border-orange-200',
            text: 'text-orange-700',
            label: '비중 쏠림'
          };

        case 'HIGH_RISK':
          return {
            bg: 'bg-red-50',
            border: 'border-red-200',
            text: 'text-red-700',
            label: '위험 부담'
          };

        case 'ERROR':
        default:
          return {
            bg: 'bg-gray-50',
            border: 'border-gray-200',
            text: 'text-gray-700',
            label: '분석 지연'
          };
      }
    };

  const style = getStatusStyle(conclusionCode);

 return (
     <div className={`w-full bg-white rounded-xl border ${style.border} shadow-sm overflow-hidden mt-6`}>
       {/* 헤더 */}
       <div className={`${style.bg} px-6 py-4 border-b ${style.border} flex justify-between items-center`}>
           <h2 className="text-2xl font-bold text-gray-900 flex items-center gap-2">
            포트폴리오 종합 진단
         </h2>
         <span className={`px-3 py-1 rounded-full text-xs font-bold bg-white/60 ${style.text}`}>
           {style.label}
         </span>
       </div>

       <div className="p-6">
       {/* 2. 본문 영역 (PC에서는 좌우 분할, 모바일에서는 상하 분할) */}
       <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 mb-6">

         {/* 2-1. 왼쪽: 현재 자산 구성 (차트) */}
         <div className="bg-gray-50 rounded-xl p-5 border border-gray-100 flex flex-col">
           <h4 className="text-gray-700 font-semibold text-sm mb-4">현재 자산 구성 비중</h4>
           <div className="flex-1 min-h-[250px]">
             <ResponsiveContainer width="100%" height="100%">
               <PieChart>
                 <Pie
                   data={currentHoldings}
                   dataKey="weightPct"
                   nameKey="name"
                   cx="50%"
                   cy="50%"
                   innerRadius={60}
                   outerRadius={80}
                   paddingAngle={0}
                   stroke="none"
                 >
                   {currentHoldings?.map((_, index) => (
                     <Cell key={index} fill={COLORS[index % COLORS.length]} />
                   ))}
                 </Pie>
                 <Tooltip />
                 <Legend verticalAlign="bottom" height={36} />
               </PieChart>
             </ResponsiveContainer>
           </div>
         </div>

         {/* 2-2. 오른쪽: AI 텍스트 분석 */}
         <div className="flex flex-col gap-5">
           {/* 한 줄 평 */}
           <div>
             <h3 className="text-lg font-bold text-gray-900 mb-2">AI 종합 의견</h3>
             <p className="text-gray-800 font-medium text-lg leading-relaxed">
               "{oneLineReview}"
             </p>
           </div>

           {/* 상세 진단 리포트 */}
           <div className="flex-1 bg-gray-50 rounded-lg p-5 border border-gray-100">
             <h4 className="text-gray-700 font-semibold text-sm mb-3">상세 진단 리포트</h4>
             <div className="text-sm text-gray-700 leading-relaxed whitespace-pre-wrap">
               {detailedAnalysis?.replace(/\\n/g, '\n')}
             </div>
           </div>
         </div>
       </div>

       {/* 3. 하단 태그 및 면책 조항 */}
       <div className="pt-4 border-t border-gray-100 flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
         <div className="flex gap-2">
           <span className="px-2 py-1 bg-gray-100 text-gray-600 text-xs rounded-md font-medium">#AI진단</span>
           <span className="px-2 py-1 bg-gray-100 text-gray-600 text-xs rounded-md font-medium">#리밸런싱</span>
         </div>
         <p className="text-xs text-gray-400 text-right">
           {disclaimer}
         </p>
       </div>
     </div>
   </div>
 );
 };