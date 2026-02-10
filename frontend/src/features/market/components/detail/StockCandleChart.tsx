import { useEffect, useMemo, useState } from "react";
import { StockChartItem } from "../../types";
import { ApexOptions } from "apexcharts";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import ReactApexChart from "react-apexcharts";

interface StockCandleChartProps {
    originalData: StockChartItem[];
    currency: 'KRW' | 'USD';
    rate: number;
}

export function StockCandleChart({ originalData, currency, rate }: StockCandleChartProps) {
    const [period, setPeriod] = useState<'1W' | '1M' | '1Y'>('1W');
    const [chartData, setChartData] = useState<StockChartItem[]>([]);

    // 기간 필터링 로직
    useEffect(() => {
        if (originalData.length === 0) return;
        const today = new Date();
        const cutoffDate = new Date();

        if (period === '1W') cutoffDate.setDate(today.getDate() - 7);
        else if (period === '1M') cutoffDate.setMonth(today.getMonth() - 1);
        else if (period === '1Y') cutoffDate.setFullYear(today.getFullYear() - 1);

        setChartData(originalData.filter(item => new Date(item.date) >= cutoffDate));
    }, [period, originalData]);

    // 시리즈 데이터 생성
    const apexSeries = useMemo(() => {
        return [{
            data: chartData.map(item => ({
                x: item.date,
                y: [item.open * rate, item.high * rate, item.low * rate, item.close * rate]
            }))
        }];
    }, [chartData, rate]);

    const apexOptions: ApexOptions = useMemo(() => ({
        chart: {
            type: 'candlestick',
            height: 350,
            toolbar: { show: false },
            zoom: { enabled: true, type: 'x', autoScaleYaxis: true },
            animations: { enabled: false }
        },
        title: {
            text: '',
            align: 'left',
            style: { fontSize: '16px', fontWeight: 'bold', color: '#374151' }
        },
        xaxis: {
            type: 'category',
            tickAmount: period === '1W' ? undefined : 6,
            labels: {
                formatter: (val) => {
                    if (!val) return '';
                    const parts = String(val).split('-');
                    return parts.length === 3 ? `${parts[1]}-${parts[2]}` : String(val);
                },
                style: { fontSize: '11px' }
            },
            tooltip: { 
                enabled: true,
                formatter: function (val, opts) {
                    const index = opts?.dataPointIndex;
                    if (typeof index === 'number' && chartData[index]) {
                        return chartData[index].date;
                    }
                    return String(val);
                }  
            },
            axisBorder: { show: true, color: '#e5e7eb' },
            axisTicks: { show: true, color: '#e5e7eb' }
        },
        yaxis: {
            tooltip: { enabled: true },
            labels: { formatter: (val) => val.toLocaleString() },
            forceNiceScale: true
        },
        plotOptions: {
            candlestick: {
                colors: { upward: '#ef4444', downward: '#3b82f6' },
                wick: { useFillColor: true }
            }
        },
        tooltip: {
            enabled: true,
            theme: 'light',
            x: { formatter: (val) => val ? new Date(val).toISOString().split('T')[0]: '' },
            y: { formatter: (val) => new Intl.NumberFormat(currency === 'KRW' ? 'ko-KR' : 'en-US', { style: currency === 'USD' ? 'currency' : undefined }).format(val) }
        },
        grid: {
            borderColor: '#e5e7eb',
            xaxis: {
                lines: { show: false }
            },
            yaxis: {
                lines: { show: true }
            }
        }
    }), [period, rate, currency, chartData]);

    return (
        <Card className="p-4">
            <CardHeader className="px-0 pt-0 pb-4 flex flex-row items-center justify-between">
                <CardTitle>주가 차트</CardTitle>
                <Tabs value={period} onValueChange={(val) => setPeriod(val as any)} className="w-[120px]">
                    <TabsList className="grid w-full grid-cols-3">
                        <TabsTrigger value="1W">1주</TabsTrigger>
                        <TabsTrigger value="1M">1달</TabsTrigger>
                        <TabsTrigger value="1Y">1년</TabsTrigger>
                    </TabsList>
                </Tabs>
            </CardHeader>
            <CardContent className="h-[400px] w-full pl-0">
                {chartData.length > 0 ? (
                    <ReactApexChart options={apexOptions} series={apexSeries} type="candlestick" height="100%" width="100%" />
                ) : (
                    <div className="h-full flex items-center justify-center text-muted-foreground bg-muted/10 rounded-lg">
                        <p>표시할 차트 데이터가 없습니다.</p>
                    </div>
                )}
            </CardContent>
        </Card>
    )
}