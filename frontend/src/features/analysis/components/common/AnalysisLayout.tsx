import React from 'react';

// 외부에서 받아올 데이터들의 이름
interface LayoutProps {
  title: string;
  description: string;
  children: React.ReactNode;
}

export const AnalysisLayout = ({ title, description, children }: LayoutProps) => {
  return (
    <div className="rounded-xl border bg-white text-card-foreground shadow-sm min-h-[500px]">
      <div className="p-6">

        {/* 페이지마다 달라지는 '제목'과 '설명'이 들어갈 자리 */}
        <div className="mb-8">
          <h2 className="text-lg font-semibold mb-1 text-gray-900">{title}</h2>
          <p className="text-sm text-gray-500">{description}</p>
        </div>

        {/* 페이지마다 달라지는 '핵심 기능(검색창, 차트 등)'이 들어갈 자리 */}
        <div className="w-full">
          {children}
        </div>

      </div>
    </div>
  );
};