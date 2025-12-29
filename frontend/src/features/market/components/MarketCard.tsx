import React from 'react';
import { IndexInfo } from '../types/market'; // 경로 확인 필요

interface Props {
  label: string;
  info: IndexInfo;
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

  const getColor = (val: number) => {
    if (val > 0) return '#ff3b30';
    if (val < 0) return '#007aff';
    return '#333';
  };

  const color = getColor(info.change);
  const sign = info.change > 0 ? '+' : '';

  return (
    <div className="card p-4 border rounded-lg shadow-sm">
      <h3 className="font-bold text-lg">{label}</h3>
      <div className="text-2xl font-semibold my-1">{info.price.toLocaleString()}</div>
      <div className="text-sm font-medium" style={{ color }}>
        {sign}{info.change} ({sign}{info.changeRate}%)
      </div>
    </div>
  );
};