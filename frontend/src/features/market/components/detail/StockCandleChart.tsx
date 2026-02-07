import { useEffect, useMemo, useState } from "react";
import { StockChartItem } from "../../types";
import { ApexOptions } from "apexcharts";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import ReactApexChart from "react-apexcharts";
import * as stockApi from '@/features/market/api'
import { Loader2 } from "lucide-react";

interface StockCandleChartProps {
    originalData: StockChartItem[];
    currency: 'KRW' | 'USD';
    rate: number;
    ticker: string;
}

// 기간 타입 정의
type PeriodType = '1D' | '1W' | '1M' | '1Y';
type MinuteInterval = 1 | 3 | 5 | 10 | 30 | 60;

export function StockCandleChart({ originalData, currency, rate, ticker }: StockCandleChartProps) {
    const [period, setPeriod] = useState<PeriodType>('1D');
    const [minuteInterval, setMinuteInterval] = useState<MinuteInterval>(1);
    const [chartData, setChartData] = useState<StockChartItem[]>([]);
    const [rawMinuteData, setRawMinuteData] = useState<StockChartItem[]>([]);
    const [isLoading, setIsLoading] = useState(false);

    // 캔들 합치기 함수
    const aggregateCandles = (data: StockChartItem[], interval: number): StockChartItem[] => {
        if (interval === 1) return data;
        if (data.length === 0) return [];

        const aggregated: StockChartItem[] = [];
        let currentChunk: StockChartItem[] = [];
        let currentBucketTime: number | null = null;

        data.forEach((item) => {
            const itemDate = new Date(item.date);
            const timestamp = itemDate.getTime();

            const minutes = itemDate.getMinutes();
            const roundedMinutes = Math.floor(minutes / interval) * interval;

            const bucketDate = new Date(itemDate);
            bucketDate.setMinutes(roundedMinutes);
            bucketDate.getSeconds(0);
            bucketDate.setMilliseconds(0);

            const bucketTime = bucketDate.getTime();

            // 새로운 버킷이 시작되면 기존 캔들 저장
            if (currentBucketTime !== null && bucketTime !== currentBucketTime) {
                if (currentChunk.length > 0) {
                    aggregated.push(makeCandle(currentChunk));
                }
                currentChunk = [];
            }

            // 현재 데이터 추가
            currentBucketTime = bucketTime;
            currentChunk.push(item);
        });

        // 마지막 남은 캔들 처리
        if (currentChunk.length > 0) {
            aggregated.push(makeCandle(currentChunk));
        }

        return aggregated;
    }

    // 캔들 생성 헬퍼 함수
    const makeCandle = (chunk: StockChartItem[]): StockChartItem => {
        const first = chunk[0];
        const last = chunk[chunk.length - 1];
        const high = Math.max(...chunk.map(c => c.high));
        const low = Math.min(...chunk.map(c => c.low));
        const volume = chunk.reduce((sum, c) => sum + c.volume, 0);

        return {
            date: first.date,
            open: first.open,
            high: high,
            low: low,
            close: last.close,
            volume: volume,
            change: 0,
            changeRate: 0
        };
    };

    // 기간 필터링 로직
    useEffect(() => {
        const loadData = async () => {
            if (period === '1D') {
                if (rawMinuteData.length > 0) {
                    setChartData(aggregateCandles(rawMinuteData, minuteInterval));
                    return;
                }

                setIsLoading(true);
                try {
                    const response = await stockApi.getStockMinuteChart(ticker);
                    const mappedData: StockChartItem[] = response.map(item => ({
                        date: item.dateTime,
                        open: item.open,
                        high: item.high,
                        low: item.low,
                        close: item.close,
                        volume: item.volume,
                        change: 0,
                        changeRate: 0
                    }));

                    setRawMinuteData(mappedData);
                    setChartData(aggregateCandles(mappedData, minuteInterval));
                } catch (e) {
                    console.error("분봉 로드 실패", e);
                    setChartData([]);
                } finally {
                    setIsLoading(false);
                }
            } else {
                if (originalData.length === 0) return;
                const today = new Date();
                const cutoffDate = new Date();

                if (period === '1W') cutoffDate.setDate(today.getDate() - 7);
                else if (period === '1M') cutoffDate.setMonth(today.getMonth() - 1);
                else if (period === '1Y') cutoffDate.setFullYear(today.getFullYear() - 1);

                setChartData(originalData.filter(item => new Date(item.date) >= cutoffDate));
            }
        };

        loadData();
    }, [period, originalData, ticker]);

    useEffect(() => {
        if (period === '1D' && rawMinuteData.length > 0) {
            setChartData(aggregateCandles(rawMinuteData, minuteInterval));
        }
    }, [minuteInterval, rawMinuteData, period]);

    // 시리즈 데이터 생성
    const apexSeries = useMemo(() => {
        return [{
            data: chartData.map(item => ({
                x: item.date,
                y: [item.open * rate, item.high * rate, item.low * rate, item.close * rate]
            }))
        }];
    }, [chartData, rate]);

    const apexOptions: ApexOptions = useMemo(() => {

        // 날짜 포맷 함수
        const formatDateTime = (val: string | number) => {
            if (!val) return '';
            const date = new Date(val);
            if (isNaN(date.getTime())) return '';

            const month = String(date.getMonth() + 1).padStart(2, '0');
            const day = String(date.getDate()).padStart(2, '0');

            // 1D 경우: 월-일 시:분
            if (period === '1D') {
                const hours = String(date.getHours()).padStart(2, '0');
                const mins = String(date.getMinutes()).padStart(2, '0');
                return `${month}-${day} ${hours}:${mins}`;
            }

            return `${date.getFullYear()}-${month}-${day}`;
        };

        // 가격 포맷 함수
        const formatPrice = (val: number) => {
            return new Intl.NumberFormat(currency === 'KRW' ? 'ko-KR' : 'en-US', {
                style: currency === 'USD' ? 'currency' : 'decimal',
                currency: 'USD',
                minimumFractionDigits: currency === 'KRW' ? 0 : 2,
                maximumFractionDigits: currency === 'KRW' ? 0 : 2
            }).format(val) + (currency === 'KRW' ? '원' : '');
        }

        return {
            chart: {
                type: 'candlestick',
                height: 350,
                toolbar: { show: false },
                zoom: { enabled: true, type: 'x', autoScaleYaxis: true },
                animations: { enabled: false },
                fontFamily: 'inherit'
            },
            title: {
                text: '',
                align: 'left',
                style: { fontSize: '16px', fontWeight: 'bold', color: '#374151' }
            },
            xaxis: {
                type: 'category',
                tickAmount: period === '1D' ? 6 : 8,
                labels: {
                    formatter: (val) => formatDateTime(val),
                    style: { fontSize: '11px', colors: '#6b7280' }
                },
                tooltip: { enabled: false },
                axisBorder: { show: false },
                axisTicks: { show: false }
            },
            yaxis: {
                tooltip: { enabled: true },
                labels: {
                    formatter: (val) => formatPrice(val),
                    style: { colors: '#6b7280' }
                },
            },
            plotOptions: {
                candlestick: {
                    colors: { upward: '#ef4444', downward: '#3b82f6' },
                    wick: { useFillColor: true }
                }
            },
            tooltip: {
                enabled: true,
                shared: true,
                custom: function ({ seriesIndex, dataPointIndex, w }) {
                    // 데이터 가져오기
                    const o = w.globals.seriesCandleO[seriesIndex][dataPointIndex];
                    const h = w.globals.seriesCandleH[seriesIndex][dataPointIndex];
                    const l = w.globals.seriesCandleL[seriesIndex][dataPointIndex];
                    const c = w.globals.seriesCandleC[seriesIndex][dataPointIndex];

                    // 날짜 가져오기
                    const categoryLabel = w.globals.categoryLabels[dataPointIndex];
                    const dateStr = formatDateTime(categoryLabel);

                    // 툴팁 HTML 생성
                    return `
                        <div class="px-3 py-2 bg-white border border-gray-200 rounded shadow-lg text-sm">
                            <div class="mb-2 font-bold text-gray-700 border-b pb-1">${dateStr}</div>
                            <div class="grid grid-cols-2 gap-x-4 gap-y-1">
                                <span class="text-gray-500">시가:</span> <span class="text-right font-medium">${formatPrice(o)}</span>
                                <span class="text-gray-500">고가:</span> <span class="text-right font-medium text-red-500">${formatPrice(h)}</span>
                                <span class="text-gray-500">저가:</span> <span class="text-right font-medium text-blue-500">${formatPrice(l)}</span>
                                <span class="text-gray-500">종가:</span> <span class="text-right font-medium">${formatPrice(c)}</span>
                            </div>
                        </div>
                    `;
                }
            },
            grid: {
                borderColor: '#f3f4f6',
                xaxis: {
                    lines: { show: false }
                },
                yaxis: {
                    lines: { show: true }
                }
            }
        };
    }, [period, rate, currency, chartData]);

    return (
        <Card className="p-4 shadow-sm border-none bg-white">
            <CardHeader className="px-0 pt-0 pb-4 flex flex-col gap-3">
                <div className="flex flex-row items-center justify-between w-full">
                    <CardTitle className="text-lg font-bold">주가 차트</CardTitle>
                    {/* 메인 탭 (1일/1주/1달/1년) */}
                    <Tabs value={period} onValueChange={(val) => setPeriod(val as PeriodType)} className="w-auto">
                        <TabsList className="grid w-full grid-cols-4 h-8 bg-gray-100">
                            <TabsTrigger value="1D" className="text-xs">1일</TabsTrigger>
                            <TabsTrigger value="1W" className="text-xs">1주</TabsTrigger>
                            <TabsTrigger value="1M" className="text-xs">1달</TabsTrigger>
                            <TabsTrigger value="1Y" className="text-xs">1년</TabsTrigger>
                        </TabsList>
                    </Tabs>
                </div>

                {/* 서브 탭: '1D'일 때만 보이는 분봉 탭 */}
                {period === '1D' && (
                    <div className="flex justify-end">
                        <Tabs value={String(minuteInterval)} onValueChange={(val) => setMinuteInterval(Number(val) as MinuteInterval)} className="w-auto">
                            <TabsList className="h-7 bg-blue-50">
                                <TabsTrigger value="1" className="text-[10px] px-2 data-[state=active]:bg-blue-500 data-[state=active]:text-white">1분</TabsTrigger>
                                <TabsTrigger value="3" className="text-[10px] px-2 data-[state=active]:bg-blue-500 data-[state=active]:text-white">3분</TabsTrigger>
                                <TabsTrigger value="5" className="text-[10px] px-2 data-[state=active]:bg-blue-500 data-[state=active]:text-white">5분</TabsTrigger>
                                <TabsTrigger value="10" className="text-[10px] px-2 data-[state=active]:bg-blue-500 data-[state=active]:text-white">10분</TabsTrigger>
                                <TabsTrigger value="30" className="text-[10px] px-2 data-[state=active]:bg-blue-500 data-[state=active]:text-white">30분</TabsTrigger>
                                <TabsTrigger value="60" className="text-[10px] px-2 data-[state=active]:bg-blue-500 data-[state=active]:text-white">60분</TabsTrigger>
                            </TabsList>
                        </Tabs>
                    </div>
                )}
            </CardHeader>

            <CardContent className="h-[350px] w-full pl-0 relative">
                {isLoading && (
                    <div className="absolute inset-0 z-10 flex items-center justify-center bg-white/80 backdrop-blur-[1px]">
                        <Loader2 className="h-8 w-8 animate-spin text-blue-500" />
                    </div>
                )}

                {chartData.length > 0 ? (
                    <ReactApexChart options={apexOptions} series={apexSeries} type="candlestick" height="100%" width="100%" />
                ) : (
                    !isLoading && (
                        <div className="h-full flex flex-col items-center justify-center text-gray-400 bg-gray-50 rounded-lg">
                            <p>표시할 데이터가 없습니다.</p>
                        </div>
                    )
                )}
            </CardContent>
        </Card>
    )
}