import { useQuery } from '@tanstack/react-query';
import { fetchDashboardStats } from '../api';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Users, UserPlus, UserMinus, Activity, AlertTriangle } from 'lucide-react';

export function AdminDashboard() {
    const { data, isLoading, isError } = useQuery({
        queryKey: ['admin-stats'],
        queryFn: fetchDashboardStats,

    });

    if (isLoading) {
        return <div className="flex h-60 items-center justify-center">데이터를 불러오는 중...</div>;
    }

    if (isError) {
        return <div className="p-4 text-red-500">대시보드 정보를 가져오는데 실패했습니다.</div>;
    }

    return (
        <div className="space-y-6 animate-in fade-in duration-500">
            <div>
                <h2 className="text-3xl font-bold tracking-tight">대시보드</h2>
                <p className="text-muted-foreground">
                    StockMatch 서비스의 현재 현황
                </p>
            </div>

            {/* 2. 통계 카드 그리드 배치 */}
            <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">

                {/* 일일 활성 유저 (DAU) */}
                <StatCard
                    title="오늘 활성 유저"
                    value={data?.dailyActiveUsers}
                    icon={Users}
                    description="오늘 로그인한 유저 수"
                />

                {/* 신규 가입 */}
                <StatCard
                    title="신규 가입"
                    value={data?.newJoinCount}
                    icon={UserPlus}
                    className="text-blue-600"
                />

                {/* 탈퇴 유저 */}
                <StatCard
                    title="탈퇴 유저"
                    value={data?.withdrawnCount}
                    icon={UserMinus}
                    description="오늘 탈퇴한 유저 수"
                    className="text-red-600"
                />

                {/* 활성 포트폴리오 */}
                <StatCard
                    title="활성 포트폴리오"
                    value={data?.dailyActivePortfolios}
                    icon={Activity}
                    description="오늘 수정된 포트폴리오"
                />
            </div>

            {/* 3. 에러 모니터링 섹션 (에러가 있을 때만 강조) */}
            <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
                 <StatCard
                    title="KIS API 에러"
                    value={data?.kisApiErrorCount}
                    icon={AlertTriangle}
                    // 에러가 1건이라도 있으면 빨간색 경고 스타일 적용
                    isWarning={data?.kisApiErrorCount ? data.kisApiErrorCount > 0 : false}
                    description="오늘 발생한 API 호출 에러"
                />
            </div>
        </div>
    );
}

interface StatCardProps {
    title: string;
    value?: number;
    icon: any;
    description?: string;
    isWarning?: boolean; // 경고 상태일 때 스타일 변경용
    className?: string;
}

function StatCard({ title, value, icon: Icon, description, isWarning, className }: StatCardProps) {
    return (
        <Card className={isWarning ? "border-red-500 bg-red-50/50" : ""}>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-sm font-medium">
                    {title}
                </CardTitle>
                <Icon className={`h-4 w-4 ${isWarning ? "text-red-500" : "text-muted-foreground"}`} />
            </CardHeader>
            <CardContent>
                <div className={`text-2xl font-bold ${isWarning ? "text-red-600" : ""} ${className}`}>
                    {/* 데이터가 없으면 0으로 표시 */}
                    {value?.toLocaleString() ?? 0}
                </div>
                {description && (
                    <p className="text-xs text-muted-foreground mt-1">
                        {description}
                    </p>
                )}
            </CardContent>
        </Card>
    );
}