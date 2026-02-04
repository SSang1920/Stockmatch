import { Watchlist } from "@/features/watchlist/types";
import { useState } from "react";
import * as watchlistApi from '@/features/watchlist/api';
import { toast } from "sonner";
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Check, Folder, Plus } from "lucide-react";
import { Button } from "@/components/ui/button";
import { CreateWatchlistDialog } from "./CreateWatchlistDialog";

interface AddToWatchlistModalProps {
    ticker: string;
    name: string;
    isOpen: boolean;
    onOpenChange: (open: boolean) => void;
    watchlists: Watchlist[];
    onWatchlistUpdate: () => void;
}

export function AddToWatchlistModal({ ticker, name, isOpen, onOpenChange, watchlists, onWatchlistUpdate }: AddToWatchlistModalProps) {
    const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);

    // 토글 로직: 이미 있으면 삭제, 없으면 추가
    const handleToggleFolder = async (watchlist: Watchlist) => {
        const existingItem = watchlist.items?.find(item => item.ticker === ticker);

        try {
            if (existingItem) {
                await watchlistApi.deleteWatchlistItem(watchlist.id, existingItem.id);
                toast.success("관심종목에서 삭제했습니다.", { duration: 1500 });
            } else {
                await watchlistApi.addWatchlistItem(watchlist.id, { ticker });
                toast.success(`${watchlist.name}에 추가했습니다.`, { duration: 1500 });
            }

            onWatchlistUpdate();
        } catch (e: any) {
            const msg = e.response?.data?.message || '오류가 발생했습니다.';
            toast.error(msg, { duration: 2000 });
        }
    };

    // 폴더 생성 후 바로 추가
    const handleCreateSuccess = async (newId: number, folderName: string) => {
        try {
            await watchlistApi.addWatchlistItem(newId, { ticker });
            toast.success(`${folderName}에 추가했습니다.`, { duration: 1500 });
            onWatchlistUpdate();
        } catch (e) {
            toast.error("폴더는 생성되었으나 추가에 실패했습니다.");
        }
    };

    return (
        <>
            <Dialog open={isOpen} onOpenChange={onOpenChange}>
                <DialogContent className="sm:max-w-md">
                    <DialogHeader>
                        <DialogTitle>관심종목 설정</DialogTitle>
                        <DialogDescription className="sr-only">
                            종목을 추가하거나 뺄 폴더를 선택하세요.
                        </DialogDescription>
                    </DialogHeader>

                    <div className="space-y-2 py-2">
                        {/* 폴더 리스트 */}
                        <div className="max-h-[300px] overflow-y-auto pr-1 space-y-2">
                            {watchlists.length > 0 ? (
                                watchlists.map((wl) => {
                                    const isAdded = wl.items?.some(item => item.ticker === ticker);

                                    return (
                                        <Button
                                            key={wl.id}
                                            variant="ghost"
                                            className={`w-full justify-start h-14 text-base font-normal px-3 transition-all duration-200
                        ${isAdded
                                                    ? 'bg-blue-50 text-blue-600 hover:bg-blue-100 hover:text-blue-700'
                                                    : 'hover:bg-secondary/50 text-foreground'
                                                }
                      `}
                                            onClick={() => handleToggleFolder(wl)}
                                        >
                                            <div className={`p-2 rounded-lg mr-3 ${isAdded ? 'bg-white/20' : 'bg-secondary'}`}>
                                                {isAdded ? <Check className="h-5 w-5" /> : <Folder className="h-5 w-5 text-muted-foreground" />}
                                            </div>
                                            <span className="flex-1 text-left">{wl.name}</span>
                                        </Button>
                                    );
                                })
                            ) : (
                                <div className="text-center py-8 text-muted-foreground bg-muted/20 rounded-xl">
                                    <p>생성된 폴더가 없습니다.</p>
                                </div>
                            )}
                        </div>

                        {/* 새 폴더 만들기 버튼 */}
                        <div className="pt-2 mt-2 border-t">
                            <Button
                                variant="ghost"
                                className="w-full justify-start h-12 text-primary hover:bg-primary/5"
                                onClick={() => setIsCreateModalOpen(true)}
                            >
                                <div className="bg-primary/10 p-2 rounded-lg mr-3">
                                    <Plus className="h-5 w-5 text-primary" />
                                </div>
                                새 폴더 만들기
                            </Button>
                        </div>
                    </div>
                </DialogContent>
            </Dialog>

            <CreateWatchlistDialog
                isOpen={isCreateModalOpen}
                onOpenChange={setIsCreateModalOpen}
                onSuccess={handleCreateSuccess}
            />
        </>
    )
}