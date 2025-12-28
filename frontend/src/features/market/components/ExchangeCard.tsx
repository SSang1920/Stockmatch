import React from 'react';
import { ExchangeRateInfo } from '../types/market';

interface Props {
  info: ExchangeRateInfo;
}

export const ExchangeCard = ({ info }: Props) => {

  if (!info) {
    return (
      <div className="card p-4 border rounded-lg shadow-sm bg-white">
        <h3 className="text-gray-500 font-bold">USD/KRW</h3>
        <div className="text-sm text-gray-400">로딩 중...</div>
      </div>
    );
  }

  const isUp = info.change > 0;
  const isDown = info.change < 0;

  let color = '#333';
  if (isUp) color = '#ff3b30';
  if (isDown) color = '#007aff';

  const sign = isUp ? '+' : '';

  return (
    <div className="card p-4 border rounded-lg shadow-sm bg-white">
      <h3 className="font-bold text-lg">USD/KRW</h3>
      <div className="text-2xl font-semibold my-1">
        {info.rate.toLocaleString()} <span className="text-sm text-gray-500">원</span>
      </div>
      <div className="text-sm font-medium" style={{ color }}>
        {sign}{info.change} ({sign}{info.changeRate}%)
      </div>
    </div>
  );
};