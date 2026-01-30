import { useSortable } from '@dnd-kit/sortable'
import { CSS } from '@dnd-kit/utilities'
import { Watchlist } from "../types";
import { Folder, MoreVertical, Pencil, Trash2 } from 'lucide-react';
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from '@/components/ui/dropdown-menu';

interface SortableCardProps {
    watchlist: Watchlist
    onClick: () => void
    onEdit: (e: React.MouseEvent) => void
    onDelete: (e: React.MouseEvent) => void
}

export function SortableWatchlistCard({ watchlist, onClick, onEdit, onDelete }: SortableCardProps) {
    const {
        attributes,
        listeners,
        setNodeRef,
        transform,
        transition,
        isDragging
    } = useSortable({ id: watchlist.id })

    const style = {
        transform: CSS.Transform.toString(transform),
        transition,
        zIndex: isDragging ? 50 : 'auto',
        opacity: isDragging ? 0.5 : 1,
    }

    return (
        <div
            ref={setNodeRef}
            style={style}
            className="group relative flex flex-col justify-between p-6 border rounded-xl bg-card shadow-sm hover:shadow-md hover:border-primary/50 transition-all bg-white cursor-pointer"
            onClick={onClick}
        >
            <div className="flex justify-between items-start mb-4">
                {/* 드래그 핸들 */}
                <div
                    className="p-3 bg-primary/10 rounded-lg text-primary cursor-grab active:cursor-grabbing"
                    {...attributes}
                    {...listeners}
                    onClick={(e) => e.stopPropagation()} // 드래그 핸들 클릭 시 상세 이동 방지
                >
                    <Folder className="h-6 w-6" />
                </div>

                {/* 메뉴 버튼 */}
                <div onClick={(e) => e.stopPropagation()}>
                    <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                            <button className="p-1 hover:bg-muted rounded-full text-muted-foreground">
                                <MoreVertical className="h-4 w-4" />
                            </button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="end">
                            <DropdownMenuItem onClick={onEdit}>
                                <Pencil className="mr-2 h-4 w-4" /> 이름 수정
                            </DropdownMenuItem>
                            <DropdownMenuItem onClick={onDelete} className="text-red-600 focus:text-red-600">
                                <Trash2 className="mr-2 h-4 w-4" /> 삭제
                            </DropdownMenuItem>
                        </DropdownMenuContent>
                    </DropdownMenu>
                </div>
            </div>

            <div>
                <h3 className="font-bold text-lg mb-1">{watchlist.name}</h3>
                <p className="text-sm text-muted-foreground">{watchlist.items.length}개 종목 포함</p>
            </div>
        </div>
    )
}