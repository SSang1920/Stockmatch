interface StockPriceInfoProps {
    currentPrice: number;
    changeAmount: number;
    changeRate: number;
    currency: 'KRW' | 'USD';
    exchangeRate: number;
    isKrMarket: boolean;
}

export function StockPriceInfo({
    currentPrice, changeAmount, changeRate, currency, exchangeRate, isKrMarket
}: StockPriceInfoProps) {
    // 가격 색상
    const getPriceColor = (rate: number) => {
        if (rate > 0) return 'text-red-500';
        if (rate < 0) return 'text-blue-500';
        return 'text-gray-900';
    };

    // 가격 포맷
    const formatVal = (val: number) => {
        return new Intl.NumberFormat(currency === 'KRW' ? 'ko-KR' : 'en-US', {
            style: currency === 'KRW' ? 'decimal' : 'currency',
            currency: 'USD',
            minimumFractionDigits: currency === 'KRW' ? 0 : 2,
            maximumFractionDigits: currency === 'KRW' ? 0 : 2
        }).format(val) + (currency === 'KRW' ? '원' : '');
    }

    return (
        <div className="flex items-end gap-4 pb-4 border-b">
            <span className={`text-4xl font-bold ${getPriceColor(changeRate)}`}>
                {formatVal(currentPrice)}
            </span>
            <div className={`flex items-center gap-2 text-lg font-medium mb-1 ${getPriceColor(changeRate)}`}>
                <span>
                    {changeAmount > 0 ? '+' : ''}
                    {formatVal(changeAmount)}
                </span>
                <span>
                    ({changeRate > 0 ? '+' : ''}{changeRate.toFixed(2)}%)
                </span>
            </div>
            {!isKrMarket && currency === 'KRW' && (
                <span className="text-xs text-muted-foreground mb-2">
                    (환율 {exchangeRate.toLocaleString()}원 적용)
                </span>
            )}
        </div>
    );
}