import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

interface StockInfoGridProps {
    data: any;
    rate: number;
    currency: 'KRW' | 'USD';
}

export function StockInfoGrid({ data, rate, currency }: StockInfoGridProps) {
    const format = (val: number) => {
        if (!val) return '-';
        return new Intl.NumberFormat(currency === 'KRW' ? 'ko-KR' : 'en-US', {
            style: currency === 'USD' ? 'currency' : undefined,
            currency: 'USD',
            minimumFractionDigits: currency === 'KRW' ? 0 : 2
        }).format(val) + (currency === 'KRW' ? '원' : '');
    };

    return (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            <InfoCard label="시가 (Open)" value={format((data.openPrice || 0) * rate)} />
            <InfoCard label="고가 (High)" value={format((data.highPrice || 0) * rate)} color="text-red-500" />
            <InfoCard label="저가 (Low)" value={format((data.lowPrice || 0) * rate)} color="text-blue-500" />
            <InfoCard label="거래량 (Vol)" value={data.volume?.toLocaleString() || '0'} />
            {data.previousClose > 0 && (
                <InfoCard label="전일 종가" value={format(data.previousClose * rate)} />
            )}
        </div>
    );
}

function InfoCard({ label, value, color }: { label: string, value: string, color?: string }) {
    return (
        <Card>
            <CardHeader className="pb-2">
                <CardTitle className="text-sm font-medium text-muted-foreground">
                    {label}
                </CardTitle>
            </CardHeader>
            <CardContent>
                <div className={`text-xl font-bold ${color || ''}`}>{value}</div>
            </CardContent>
        </Card>
    )
}