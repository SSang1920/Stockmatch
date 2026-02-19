import { useCallback, useEffect, useMemo, useState, useTransition } from "react";
import { StockChartItem } from "../../types";
import { ApexOptions } from "apexcharts";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import ReactApexChart from "react-apexcharts";
import * as stockApi from '@/features/market/api'
import { Loader2 } from "lucide-react";
import dayjs from "@/lib/dayjs";

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
    const [aggCache, setAggCache] = useState<Record<number, StockChartItem[]>>({});
    const [isLoading, setIsLoading] = useState(false);
    const [isPending, startTransition] = useTransition();

    // 티커 변경 시 캐시 및 상태 초기화
    useEffect(() => {
        setAggCache({});
        setRawMinuteData([]);
        setChartData([]);
        setPeriod('1D');
        setMinuteInterval(1);
    }, [ticker]);

    // 날짜 파싱 헬퍼
    const parseDateToTs = useCallback((dateInput: string | Date | number) => {
        if (!dateInput) return 0;
        let dateStr = String(dateInput);

        if (dateStr.length === 14 && !isNaN(Number(dateStr))) {
            // 20260214120000 -> 2026-02-14T12:00:00
            dateStr = `${dateStr.slice(0, 4)}-${dateStr.slice(4, 6)}-${dateStr.slice(6, 8)}T${dateStr.slice(8, 10)}:${dateStr.slice(10, 12)}:${dateStr.slice(12, 14)}`;
        }

        // 한국 주식이면 서울 시간대, 미국이면 뉴욕 시간대
        const tz = currency === 'KRW' ? "Asia/Seoul" : "America/New_York";

        const dayjsDate = dayjs.tz(dateStr, tz);

        return dayjsDate.isValid() ? dayjsDate.valueOf() : 0;
    }, [currency]);

    // 고속 캔들 생성 함수
    const makeCandleFast = useCallback((chunk: StockChartItem[]): StockChartItem | null => {
        if (!chunk || chunk.length === 0) return null;

        // 첫 번째 유효한 데이터를 찾아 초기값으로 설정
        let firstValidIndex = -1;
        for (let i = 0; i < chunk.length; i++) {
            const c = chunk[i];
            if (c.high != null && !isNaN(c.high) && c.low != null && !isNaN(c.low)) {
                firstValidIndex = i;
                break;
            }
        }

        // 유효한 데이터가 하나도 없으면 null 반환
        if (firstValidIndex === -1) return null;

        const first = chunk[0];
        const last = chunk[chunk.length - 1];

        let high = chunk[firstValidIndex].high;
        let low = chunk[firstValidIndex].low;
        let volume = 0;
        const len = chunk.length;

        for (let i = 0; i < len; i++) {
            const c = chunk[i];
            if (c.high == null || isNaN(c.high) || c.low == null || isNaN(c.low)) continue;

            if (c.high > high) high = c.high;
            if (c.low < low) low = c.low;
            volume += (Number(c.volume) || 0);
        }

        return {
            date: first.date,
            open: Number(first.open),
            high: high,
            low: low,
            close: Number(last.close),
            volume: volume,
            change: 0,
            changeRate: 0
        };
    }, []);

    // 캔들 합치기 로직
    const aggregateCandles = useCallback((data: StockChartItem[], interval: number): StockChartItem[] => {
        if (interval === 1) return data;
        if (!data || data.length === 0) return [];

        const aggregated: StockChartItem[] = [];
        let currentChunk: StockChartItem[] = [];
        let currentBucketTime: number | null = null;

        for (const item of data) {
            const timestamp = parseDateToTs(item.date);
            if (timestamp === 0) continue;

            const itemDate = new Date(timestamp);
            const minutes = itemDate.getMinutes();

            const bucketDate = new Date(timestamp);
            const roundedMinutes = Math.floor(minutes / interval) * interval;
            bucketDate.setMinutes(roundedMinutes);
            bucketDate.getSeconds(0);
            bucketDate.setMilliseconds(0);

            const bucketTime = bucketDate.getTime();

            if (currentBucketTime !== null && bucketTime !== currentBucketTime) {
                if (currentChunk.length > 0) {
                    const candle = makeCandleFast(currentChunk);
                    if (candle) aggregated.push(candle);
                }
                currentChunk = [];
            }

            currentBucketTime = bucketTime;
            currentChunk.push(item);
        }

        // 마지막 남은 캔들 처리
        if (currentChunk.length > 0) {
            const candle = makeCandleFast(currentChunk);
            if (candle) aggregated.push(candle);
        }

        return aggregated;
    }, [makeCandleFast]);

    // 기간 필터링 로직
    useEffect(() => {
        const loadData = async () => {
            if (period === '1D') {
                // 이미 원본 데이터가 있으면 로딩 안함
                if (rawMinuteData.length > 0) return;

                setIsLoading(true);
                try {
                    const response = await stockApi.getStockMinuteChart(ticker);
                    const mappedData: StockChartItem[] = response
                        .filter(item => 
                            item.dateTime != null && 
                            item.open != null && !isNaN(Number(item.open)) &&
                            item.close != null && !isNaN(Number(item.close))
                        )
                        .map(item => ({
                            date: item.dateTime, 
                            open: Number(item.open),
                            high: Number(item.high),
                            low: Number(item.low),
                            close: Number(item.close),
                            volume: Number(item.volume),
                            change: 0,
                            changeRate: 0
                        }));

                    setRawMinuteData(mappedData);
                    setChartData(mappedData);
                    setAggCache(prev => ({ ...prev, 1: mappedData }));
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

                const filtered = originalData.filter(item => {
                    const ts = parseDateToTs(item.date);
                    const d = new Date(ts);
                    return d >= cutoffDate && !isNaN(d.getTime()) && Number(item.open) > 0;
                })
                .sort((a, b) => parseDateToTs(a.date) - parseDateToTs(b.date));

                setChartData(filtered);
            }
        };

        loadData();
    }, [period, originalData, ticker, rawMinuteData.length]);

    // 분봉 변경 시 캐시 활용
    useEffect(() => {
        if (period === '1D' && rawMinuteData.length > 0) {
            // 캐시에 있으면 캐시 사용
            if (aggCache[minuteInterval]) {
                startTransition(() => {
                    setChartData(aggCache[minuteInterval]);
                });
                return;
            }

            // 캐시에 없으면 계산 후 저장
            const aggregated = aggregateCandles(rawMinuteData, minuteInterval);
            setAggCache(prev => ({ ...prev, [minuteInterval]: aggregated }));

            startTransition(() => {
                setChartData(aggregated);
            });
        }
    }, [minuteInterval, rawMinuteData, period, aggCache, aggregateCandles]);

    // 유효 데이터만 추출
    const validData = useMemo(() => {
        return chartData.filter(item =>
            item.open != null && !isNaN(item.open) && item.open > 0 &&
            item.close != null && !isNaN(item.close)
        );
    }, [chartData]);

    // 시리즈 데이터 생성
    const apexSeries = useMemo(() => {
        return [{
            name: 'Price',
            data: validData.map((item, index) => ({
                x: period === '1D' ? parseDateToTs(item.date) : index,
                y: [item.open * rate, item.high * rate, item.low * rate, item.close * rate]
            }))
        }];
    }, [validData, rate]);

    const apexOptions: ApexOptions = useMemo(() => {
        const isIntraday = period === '1D';

        // 날짜 포맷 함수
        const formatDateTime = (ts: number) => {
            const d = dayjs(ts);
            if (isIntraday) return d.format('MM-DD HH:mm');
            return d.format('YY.MM.DD');
        };

        // 가격 포맷 함수
        const formatPrice = (val: number) => {
            if (val === null || isNaN(val)) return '-';
            return new Intl.NumberFormat(currency === 'KRW' ? 'ko-KR' : 'en-US', {
                style: currency === 'USD' ? 'currency' : 'decimal',
                currency: 'USD',
                minimumFractionDigits: currency === 'KRW' ? 0 : 2,
                maximumFractionDigits: currency === 'KRW' ? 0 : 2
            }).format(val) + (currency === 'KRW' ? '원' : '');
        }

        // 초기 화면 범위를 "마지막 데이터가 있는 날의 개장 시간"부터로 설정
        let xaxisMin: number | undefined = undefined;
        let xaxisMax: number | undefined = undefined;

        if (isIntraday && validData.length > 0) {
            // 가장 최신 데이터의 시간을 가져옴 (오늘 장중이면 오늘, 새벽이면 어제)
            const lastTs = parseDateToTs(validData[validData.length - 1].date);

            // 그 날짜의 "장 시작 시간"을 계산
            const baseDate = dayjs(lastTs);

            if (currency === 'KRW') {
                // 한국: 09:00 ~ 15:30
                xaxisMin = baseDate.hour(9).minute(0).second(0).valueOf();
                xaxisMax = baseDate.hour(15).minute(30).second(0).valueOf();
            } else {
                // 미국: 04:00 ~ 20:00 (현지시간)
                xaxisMin = baseDate.hour(4).minute(0).second(0).valueOf();
                xaxisMax = baseDate.hour(20).minute(0).second(0).valueOf();
            }
        }

        return {
            chart: {
                type: 'candlestick',
                height: 350,
                toolbar: { show: false },
                zoom: { enabled: true, type: 'x', autoScaleYaxis: true },
                animations: { enabled: false },
                fontFamily: 'inherit',
                selection: { enabled: true, xaxis: { min: xaxisMin, max: xaxisMax } }
            },
            title: {
                text: '',
                align: 'left',
                style: { fontSize: '16px', fontWeight: 'bold', color: '#374151' }
            },
            xaxis: {
                type: isIntraday ? 'datetime' : 'numeric',
                min: xaxisMin,
                max: xaxisMax,
                tickAmount: 6,
                labels: {
                    formatter: (val) => {
                        if (isIntraday) {
                            return dayjs(val).format('MM-DD HH:mm');
                        } else {
                            const index = Math.floor(Number(val));
                            const item = validData[index];
                            return item ? formatDateTime(parseDateToTs(item.date)) : '';
                        }
                    },
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
                custom: function({ seriesIndex, dataPointIndex, w }) {
                    // 데이터 가져오기
                    const o = w.globals.seriesCandleO[seriesIndex][dataPointIndex];
                    const h = w.globals.seriesCandleH[seriesIndex][dataPointIndex];
                    const l = w.globals.seriesCandleL[seriesIndex][dataPointIndex];
                    const c = w.globals.seriesCandleC[seriesIndex][dataPointIndex];

                    // 날짜 가져오기
                    let dateStr = '-';
                    if (isIntraday) {
                        const ts = w.globals.seriesX[seriesIndex][dataPointIndex];
                        dateStr = dayjs(ts).format('MM-DD HH:mm');
                    } else {
                        const item = validData[dataPointIndex];
                        dateStr = item ? dayjs(parseDateToTs(item.date)).format('YY.MM.DD') : '-';
                    }

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
                },
                x: {
                    formatter: (val) => {
                        const index = Math.floor(Number(val));
                        const item = validData[index];
                        return item ? formatDateTime(parseDateToTs(item.date)) : '';
                    }
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
    }, [period, rate, currency, validData, parseDateToTs]);

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