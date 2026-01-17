import { Watchlist } from '@/features/watchlist/types';
import * as watchlistApi from '@/features/watchlist/api/watchlistApi';
import { createFileRoute, redirect } from '@tanstack/react-router'
import { useEffect, useState } from 'react';
import { ChevronRight, Folder, Plus } from 'lucide-react';
import axios from 'axios';

// 인증 체크 함수
const checkAuth = async () => {
    try {
        await axios.get('/api/auth/check');
        return true;
    } catch (error) {
        return false;
    }
}

export const Route = createFileRoute('/_public/watchlists/')({
    // 페이지 로드 전 검사
    beforeLoad: async ({ location }) => {
        const isLoggedIn = await checkAuth();

        if (!isLoggedIn) {
            throw redirect({
                to: '/login',
                search: {
                    redirect: location.href,
                },
            });
        }
    },
    component: WatchlistPage,
});

function WatchlistPage() {
    const [selectedWatchlistId, setSelectedWatchlistId] = useState<number | null>(null);
    const [watchlists, setWatchlists] = useState<Watchlist[]>([]);
    const [loading, setLoading] = useState(true);

    // 데이터 로딩
    const loadWatchlists = async () => {
        try {
            setLoading(true);
            const data = await watchlistApi.getWatchlists();
            setWatchlists(data);
        } catch (error) {
            console.error(error);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadWatchlists();
    }, [selectedWatchlistId]);

    // 관심종목 폴더 생성 핸들러
    const handleCreate = async () => {
        const name = prompt('새 폴더 이름을 입력하세요.');
        if (!name) return;
        try {
            await watchlistApi.createWatchlist(name);
            loadWatchlists();
        } catch (e: any) {
            alert(e.message);
        }
    };

    // 상세 화면 렌더링
    if (selectedWatchlistId !== null) {
        return (
            <WatchlistDetail
                watchlistId={selectedWatchlistId}
                onBack={() => setSelectedWatchlistId(null)}
            />
        );
    }

    // 목록 화면 렌더링
    return (
        <div className="space-y-6 animate-in fade-in duration-300">
            {/* 헤더 */}
            <div className="flex items-center justify-between">
                <div>
                    <h1 className="text-2xl font-bold">관심종목</h1>
                    <p className="text-muted-foreground mt-1">나만의 투자 리스트를 관리해보세요.</p>
                </div>
                <button
                    onClick={handleCreate}
                    className="flex items-center gap-2 bg-primary text-primary-foreground px-4 py-2 rounded-md hover:bg-primary/90 transition-colors"
                >
                    <Plus className="h-4 w-4" />
                    새 폴더
                </button>
            </div>

            {/* 폴더 리스트 그리드 */}
            {loading ? (
                <div className="py-10 text-center text-muted-foreground">로딩 중...</div>
            ) : watchlists.length === 0 ? (
                <div className="flex flex-col items-center justify-center py-20 border-2 border-dashed rounded-xl bg-muted/10">
                    <Folder className="h-10 w-10 text-muted-foreground mb-4" />
                    <p className="text-muted-foreground mb-4">만들어진 폴더가 없습니다.</p>
                    <button onClick={handleCreate} className="text-primary font-medium hover:underline">
                        첫 번째 폴더 만들기
                    </button>
                </div>
            ) : (
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
                    {watchlists.map((watchlist) => (
                        <div
                            key={watchlist.id}
                            onClick={() => setSelectedWatchlistId(watchlist.id)} // 클릭 시 상세 보기 상태로 변경
                            className="group cursor-pointer relative flex flex-col justify-between p-6 border rounded-xl bg-card shadow-sm hover:shadow-md hover:border-primary/50 transition-all"
                        >
                            <div className="flex justify-between items-start mb-4">
                                <div className="p-3 bg-primary/10 rounded-lg text-primary group-hover:bg-primary group-hover:text-white transition-colors">
                                    <Folder className="h-6 w-6" />
                                </div>
                                <ChevronRight className="h-5 w-5 text-muted-foreground opacity-0 group-hover:opacity-100 transition-opacity" />
                            </div>

                            <div>
                                <h3 className="font-bold text-lg mb-1">{watchlist.name}</h3>
                                <p className="text-sm text-muted-foreground">
                                    {watchlist.items.length}개 종목 포함
                                </p>
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
}
