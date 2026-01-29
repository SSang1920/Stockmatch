import { useEffect, useState } from "react"
import { Button } from "@/components/ui/button"
import { ArrowLeft, Check, GripVertical, Pencil, Plus, Search, Trash2, X } from "lucide-react"
import { Link } from "@tanstack/react-router"
import { SortableContext, arrayMove, sortableKeyboardCoordinates, useSortable, verticalListSortingStrategy } from "@dnd-kit/sortable"
import { DndContext, DragEndEvent, KeyboardSensor, PointerSensor, closestCenter, useSensor, useSensors } from "@dnd-kit/core"
import { CSS } from "@dnd-kit/utilities"
import { getErrorMessage } from "@/lib/utils"

// API & Types
import * as watchlistApi from '../api'
import { Watchlist, WatchlistItem } from "../types"
import { StockSearchModal } from "."
import { Input } from "@/components/ui/input"

interface SortableItemProps {
    item: WatchlistItem;
    onDelete: (id: number) => void;
    onUpdateMemo: (id: number, memo: string) => void;
}

function SortableWatchlistItem({ item, onDelete, onUpdateMemo }: SortableItemProps) {
    const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({ id: item.id });

    const style = {
        transform: CSS.Transform.toString(transform),
        transition,
        opacity: isDragging ? 0.5 : 1,
        zIndex: isDragging ? 50 : 'auto',
        position: 'relative' as const,
    };

    // 메모 수정 모드 상태
    const [isEditing, setIsEditing] = useState(false);
    const [memoText, setMemoText] = useState(item.memo || "");

    const handleSaveMemo = (e: React.MouseEvent | React.KeyboardEvent) => {
        e.stopPropagation();
        onUpdateMemo(item.id, memoText);
        setIsEditing(false);
    };

    const handleCancelMemo = (e: React.MouseEvent) => {
        e.stopPropagation();
        setMemoText(item.memo || "");
        setIsEditing(false);
    }

    return (
        <div
            ref={setNodeRef}
            style={style}
            className="bg-white border rounded-lg p-3 mb-2 shadow-sm flex items-center gap-3 group"
        >
            {/* 드래그 핸들 */}
            <div
                {...attributes}
                {...listeners}
                className="cursor-grab text-gray-400 hover:text-gray-600 touch-none p-1"
            >
                <GripVertical className="h-5 w-5" />
            </div>

            {/* 종목 정보 */}
            <Link to={`/stocks/${item.market}/${item.ticker}`} className="flex-1 min-w-0">
                <div className="flex items-center gap-2">
                    <span className="font-bold text-lg">{item.ticker}</span>
                    <span className="text-[10px] bg-secondary px-1.5 py-0.5 rounded text-secondary-foreground">
                        {item.market}
                    </span>
                </div>
                <div className="text-sm text-muted-foreground truncate">
                    {item.securityName}
                </div>
            </Link>

            {/* 메모 영역 */}
            <div 
                className="flex flex-col items-end gap-1 min-w-[180px]" 
                onPointerDown={(e) => e.stopPropagation()}
                onClick={(e) => e.stopPropagation()}
            >
                {isEditing ? (
                    <div className="flex items-center gap-1">
                        <Input
                            value={memoText}
                            onChange={(e) => setMemoText(e.target.value)}
                            className="h-8 text-xs w-[120px]"
                            placeholder="메모 입력"
                            autoFocus
                            onKeyDown={(e) => {
                                if (e.key === 'Enter') handleSaveMemo(e);
                                if (e.key === 'Escape') setIsEditing(false);
                            }}
                        />
                        <Button size="icon" variant="ghost" className="h-8 w-8" onClick={handleSaveMemo}>
                            <Check className="h-4 w-4 text-green-600" />
                        </Button>
                        <Button size="icon" variant="ghost" className="h-8 w-8" onClick={handleCancelMemo}>
                            <X className="h-4 w-4 text-red-500" />
                        </Button>
                    </div>
                ) : (
                    <div className="flex items-center gap-2 group/memo">
                        {item.memo ? (
                            <span
                                className="text-xs bg-yellow-50 text-yellow-800 px-2 py-1 rounded max-w-[150px] truncate cursor-pointer hover:bg-yellow-100"
                                title={item.memo}
                                onClick={() => setIsEditing(true)}
                            >
                                {item.memo}
                            </span>
                        ) : (
                            <span
                                className="text-xs text-muted-foreground opacity-0 group-hover/memo:opacity-100 cursor-pointer hover:text-primary"
                                onClick={() => setIsEditing(true)}
                            >
                                메모 추가
                            </span>
                        )}
                        <Button
                            size="icon"
                            variant="ghost"
                            className="h-7 w-7 opacity-0 group-hover:opacity-100 transition-opacity"
                            onClick={() => setIsEditing(true)}
                        >
                            <Pencil className="h-3 w-3" />
                        </Button>
                    </div>
                )}
            </div>

            {/* 삭제 버튼 */}
            <Button
                size="icon"
                variant="ghost"
                className="text-muted-foreground hover:text-red-600 hover:bg-red-50"
                onClick={(e) => {
                    e.stopPropagation();
                    onDelete(item.id);
                }}
            >
                <Trash2 className="h-4 w-4" />
            </Button>
        </div>
    )
}

interface DetailProps {
    watchlistId: number
    onBack: () => void
}

export function WatchlistDetail({ watchlistId, onBack }: DetailProps) {
    const [watchlist, setWatchlist] = useState<Watchlist | null>(null);
    const [loading, setLoading] = useState(true);
    const [isSearchOpen, setIsSearchOpen] = useState(false);

    // DnD 센서
    const sensors = useSensors(
        useSensor(PointerSensor, { activationConstraint: { distance: 8 } }),
        useSensor(KeyboardSensor, { coordinateGetter: sortableKeyboardCoordinates })
    );

    const loadData = async () => {
        try {
            setLoading(true);
            const data = await watchlistApi.getWatchlistDetail(watchlistId);
            if (data && !data.items) {
                data.items = [];
            }
            setWatchlist(data);
        } catch (error) {
            alert('정보를 불러오지 못했습니다.');
            onBack();
        } finally {
            setLoading(false);
        }
    }

    useEffect(() => { loadData() }, [watchlistId]);

    // 종목 추가 핸들러
    const handleAddStock = async (ticker: string) => {
        try {
            await watchlistApi.addWatchlistItem(watchlistId, ticker);
            await loadData();
            setIsSearchOpen(false);
        } catch (error) {
            console.error(error);
            alert(getErrorMessage(error));
        }
    };

    // 종목 삭제 핸들러
    const handleDelete = async (itemId: number) => {
        if (!confirm("정말 삭제하시겠습니까?")) return;
        try {
            await watchlistApi.deleteWatchlistItem(watchlistId, itemId);
            setWatchlist(prev => prev ? { ...prev, items: prev.items.filter(i => i.id !== itemId) } : null);
        } catch (error) {
            alert(getErrorMessage(error));
            loadData();
        }
    };

    // 메모 수정 핸들러
    const handleUpdateMemo = async (itemId: number, memo: string) => {
        try {
            await watchlistApi.updateWatchlistItem(watchlistId, itemId, memo);
            setWatchlist(prev => prev ? { ...prev, items: prev.items.map(i => i.id === itemId ? { ...i, memo } : i) } : null);
        } catch (error) {
            alert(getErrorMessage(error))
        }
    };

    // 순서 변경 핸들러
    const handleDragEnd = async (event: DragEndEvent) => {
        const { active, over } = event;

        if (!watchlist || !over || active.id === over.id) return;

        const activeId = active.id;
        const overId = over.id;

        const oldIndex = watchlist.items.findIndex(i => i.id === activeId);
        const newIndex = watchlist.items.findIndex(i => i.id === overId);

        if (oldIndex === -1 || newIndex === -1) return;

        // UI 순서 즉시 변경
        const newItems = arrayMove(watchlist.items, oldIndex, newIndex);
        setWatchlist({ ...watchlist, items: newItems });

        // 서버에 순서 저장
        try {
            const itemIds = newItems.map(i => i.id);
            await watchlistApi.sortWatchlistItems(watchlistId, itemIds);
        } catch (error) {
            console.error("순서 저장 실패하였습니다.", error);
            alert(getErrorMessage(error));
            loadData();
        }
    }

    if (loading) return <div className="p-10 text-center">로딩 중...</div>;
    if (!watchlist) return null;

    return (
        <div className="space-y-6 animate-in slide-in-from-right-4 duration-300">
            {/* 상단 헤더 */}
            <div className="flex items-center justify-between pb-4 border-b">
                <div className="flex items-center gap-3">
                    <Button variant="ghost" size="icon" onClick={onBack} className="rounded-full">
                        <ArrowLeft className="h-5 w-5" />
                    </Button>
                    <div>
                        <h1 className="text-2xl font-bold">{watchlist.name}</h1>
                        <p className="text-sm text-muted-foreground">총 {watchlist.items.length}개 종목</p>
                    </div>
                </div>
                <Button onClick={() => setIsSearchOpen(true)}>
                    <Plus className="mr-2 h-4 w-4" /> 종목 추가
                </Button>
            </div>

            {/* 목록 영역 (DnD Context) */}
            <div className="bg-muted/30 border rounded-xl min-h-[400px] p-4">
                {watchlist.items.length === 0 ? (
                    <div className="flex flex-col items-center justify-center h-64 text-muted-foreground">
                        <Search className="h-10 w-10 mb-2 opacity-20" />
                        <p>아직 추가된 종목이 없습니다.</p>
                    </div>
                ) : (
                    <DndContext sensors={sensors} collisionDetection={closestCenter} onDragEnd={handleDragEnd}>
                        <SortableContext items={watchlist.items.map(i => i.id)} strategy={verticalListSortingStrategy}>
                            {watchlist.items.map((item) => (
                                <SortableWatchlistItem
                                    key={item.id}
                                    item={item}
                                    onDelete={handleDelete}
                                    onUpdateMemo={handleUpdateMemo}
                                />
                            ))}
                        </SortableContext>
                    </DndContext>
                )}
            </div>

            {/* 종목 검색 모달 */}
            <StockSearchModal
                open={isSearchOpen}
                onOpenChange={setIsSearchOpen}
                onAdd={handleAddStock}
            />
        </div>
    )
}