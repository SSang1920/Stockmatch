import React from 'react';
import { ExchangeRateInfo } from '../types';

interface Props {
  label: string;
  info: ExchangeRateInfo | null;
}

export const ExchangeCard = ({ label, info }: Props) => {

  if (!info) {
    return (
      <div className="card p-4 border rounded-lg shadow-sm bg-white">
        <h3 className="text-gray-500 font-bold">{label}</h3>
        <div className="text-sm text-gray-400">데이터 없음</div>
      </div>
    );
  }

  const rate = info.rate ?? 0;
  const change = info.change ?? 0;
  const changeRate = info.changeRate ?? 0;

  const getColor = (val: number) => {
    if (val > 0) return '#ef4444';
    if (val < 0) return '#3b82f6';
    return '#374151';
  }

  const color = getColor(change);
  const sign = change > 0 ? '+' : '';

  return (
    <div className="card p-4 border rounded-lg shadow-sm bg-white">
      <h3 className="font-bold text-lg">{label}</h3>
      <div className="text-2xl font-semibold my-1">
        {rate.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
        <span className="text-lg font-normal ml-1">원</span>
      </div>
      <div className="text-sm font-medium flex items-center gap-1" style={{ color }}>
        <span>
          {sign}{change.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
        </span>
         ({sign}{changeRate.toFixed(2)}%)
      </div>
    </div>
  );
};