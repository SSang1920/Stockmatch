import React from 'react';
import { ExchangeRateInfo } from '../types/market';

interface Props {
  info: ExchangeRateInfo;
}

export const ExchangeCard = ({ info }: Props) => {
  const isUp = info.change >= 0;
  const color = isUp ? '#ff3b30' : '#007aff';
  const sign = isUp ? '▲' : '▼';

  return (
    <div className="card">
      <h3>USD/KRW</h3>
      <div className="price">{info.rate.toLocaleString()} 원</div>
      <div className="change" style={{ color }}>
        {sign} {Math.abs(info.change)} ({info.changeRate}%)
      </div>
    </div>
  );
};