import React from 'react';
import { IndexInfo } from '../types/market'; // 경로 확인 필요

interface Props {
  label: string;
  info: IndexInfo | null;
}

export const MarketCard = ({ label, info }: Props) => {
  if (!info) {
    return (
      <div className="card p-4 border rounded-lg shadow-sm">
        <h3 className="text-gray-500">{label}</h3>
        <div className="text-sm text-gray-400">데이터 없음</div>
      </div>
    );
  }

  const price = info.price ?? 0;
  const change = info.change ?? 0;
  const changeRate = info.changeRate ?? 0;

  const getColor = (val: number) => {
    if (val > 0) return '#ef4444';
    if (val < 0) return '#3b82f6';
    return '#374151';
  };

  const color = getColor(change);
  const sign = info.change > 0 ? '+' : '';

  return (
    <div className="card p-4 border rounded-lg shadow-sm bg-white">
      <h3 className="font-bold text-lg">{label}</h3>
      <div className="text-2xl font-semibold my-1">{price.toLocaleString()}</div>
      <div className="text-sm font-medium flex items-center gap-1" style={{ color }}>
        <span>
        {sign}{change.toLocaleString()}
        </span>
         ({sign}{changeRate.toFixed(2)}%)
      </div>
    </div>
  );
};