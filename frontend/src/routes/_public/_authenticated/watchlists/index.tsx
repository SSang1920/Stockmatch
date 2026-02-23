import { Watchlist } from '@/features/watchlist/types';
import * as watchlistApi from '@/features/watchlist/api/watchlistApi';
import { createFileRoute, useNavigate ,redirect } from '@tanstack/react-router'
import { useEffect, useState } from 'react';
import { ChevronRight, Folder, Plus } from 'lucide-react';
import { WatchlistDialog, SortableWatchlistCard, WatchlistDetail } from '@/features/watchlist/components';
import { DndContext, DragEndEvent, KeyboardSensor, PointerSensor, closestCenter, useSensor, useSensors } from '@dnd-kit/core';
import { SortableContext, arrayMove, rectSortingStrategy, sortableKeyboardCoordinates } from '@dnd-kit/sortable';
import { Button } from '@/components/ui/button';
import { useUser } from '@/context/UserContext';


export const Route = createFileRoute('/_public/_authenticated/watchlists/')({
    component: WatchlistPage,
});

function WatchlistPage() {
    const navigate = useNavigate();
    const { user, isLoading: isUserLoading } = useUser();

    const [selectedWatchlistId, setSelectedWatchlistId] = useState<number | null>(null);
    const [watchlists, setWatchlists] = useState<Watchlist[]>([]);
    const [loading, setLoading] = useState(true);

    // 모달 상태 관리
    const [isCreateOpen, setIsCreateOpen] = useState(false)
    const [editTarget, setEditTarget] = useState<{ id: number, name: string } | null>(null)

    // 마우스, 터치, 키보드 지원
    const sensors = useSensors(
        useSensor(PointerSensor, { activationConstraint: { distance: 8 } }), // 8px 움직여야 드래그
        useSensor(KeyboardSensor, { coordinateGetter: sortableKeyboardCoordinates })
    );

    useEffect(() => {
            if (!isUserLoading && !user) {
                navigate({
                    to: '/sign-in',
                    search: { redirect: window.location.href }
                });
            }
        }, [user, isUserLoading, navigate]);

    // 데이터 로딩
        const loadWatchlists = async () => {
            if (!user) return;
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
            if (user) {
                loadWatchlists();
            }
        }, [user, selectedWatchlistId]);

    // [Handler] 폴더 생성
    const handleCreate = async (name: string) => {
        await watchlistApi.createWatchlist(name);
        await loadWatchlists();
    };

    // [Handler] 폴더 이름 수정
    const handleRename = async (name: string) => {
        if (!editTarget) return;
        await watchlistApi.updateWatchlist(editTarget.id, name);
        await loadWatchlists();
        setEditTarget(null);
    }

    // [Handler] 폴더 삭제
    const handleDelete = async (id: number) => {
        if (!confirm("정말 삭제하시겠습니까?")) return;
        try {
            await watchlistApi.deleteWatchlist(id);
            await loadWatchlists();
        } catch (error) { alert("삭제 실패"); }
    }

    // [Handler] 드래그 종료 시 (순서 변경)
    const handleDragEnd = async (event: DragEndEvent) => {
        const { active, over } = event

        if (over && active.id !== over.id) {
            // UI 업데이트
            const oldIndex = watchlists.findIndex((w) => w.id === active.id)
            const newIndex = watchlists.findIndex((w) => w.id === over.id)

            const newOrder = arrayMove(watchlists, oldIndex, newIndex)
            setWatchlists(newOrder)

            // API 호출
            try {
                const newOrderIds = newOrder.map(w => w.id);
                await watchlistApi.sortWatchlists(newOrderIds);
            } catch (error) {
                console.error("순서 변경 실패", error)
                loadWatchlists()
            }
        }
    }

    if (isUserLoading || !user) {
        return <div className="flex h-[50vh] items-center justify-center">로그인 확인 중...</div>;
    }

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
                <Button
                    onClick={() => setIsCreateOpen(true)}>
                    <Plus className="h-4 w-4 mr-2" />
                    새 폴더
                </Button>
            </div>

            {/* 폴더 리스트 */}
            <DndContext
                sensors={sensors}
                collisionDetection={closestCenter}
                onDragEnd={handleDragEnd}
            >
                <SortableContext
                    items={watchlists.map(w => w.id)}
                    strategy={rectSortingStrategy}
                >
                    {loading ? (
                        <div className="py-10 text-center text-muted-foreground">로딩 중...</div>
                    ) : watchlists.length === 0 ? (
                        <div className="flex flex-col items-center justify-center py-20 border-2 border-dashed rounded-xl bg-muted/10">
                            <Folder className="h-10 w-10 text-muted-foreground mb-4" />
                            <p className="text-muted-foreground mb-4">만들어진 폴더가 없습니다.</p>
                            <Button variant="link" onClick={() => setIsCreateOpen(true)}>
                                첫 번째 폴더 만들기
                            </Button>
                        </div>
                    ) : (
                        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
                            {watchlists.map((watchlist) => (
                                <SortableWatchlistCard
                                    key={watchlist.id}
                                    watchlist={watchlist}
                                    onClick={() => setSelectedWatchlistId(watchlist.id)}
                                    onEdit={(e) => {
                                        e.stopPropagation();
                                        setEditTarget({ id: watchlist.id, name: watchlist.name });
                                    }}
                                    onDelete={(e) => {
                                        e.stopPropagation();
                                        handleDelete(watchlist.id);
                                    }}
                                />
                            ))}
                        </div>
                    )}
                </SortableContext>
            </DndContext>

            {/* 생성 모달 */}
            <WatchlistDialog
                open={isCreateOpen}
                onOpenChange={setIsCreateOpen}
                mode="create"
                onSubmit={handleCreate}
            />

            {/* 수정 모달 */}
            <WatchlistDialog
                open={!!editTarget}
                onOpenChange={(open) => !open && setEditTarget(null)}
                mode="edit"
                onSubmit={handleRename}
            />
        </div>
    );
}
