import { useState } from "react";
import * as watchlistApi from '@/features/watchlist/api';
import { toast } from "sonner";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Loader2 } from "lucide-react";

interface CreateWatchlistDialogProps {
    isOpen: boolean;
    onOpenChange: (open: boolean) => void;
    onSuccess: (newWatchlistId: number, folderName: string) => void;
}

export function CreateWatchlistDialog({ isOpen, onOpenChange, onSuccess }: CreateWatchlistDialogProps) {
    const [folderName, setFolderName] = useState('');
    const [isSubmitting, setIsSubmitting] = useState(false);

    const handleSubmit = async () => {
        if (!folderName.trim()) return;
        setIsSubmitting(true);
        try {
            const newId = await watchlistApi.createWatchlist(folderName);
            onSuccess(newId, folderName);
            setFolderName('');
            onOpenChange(false);
        } catch (e) {
            console.error(e);
            toast.error('폴더 생성에 실패했습니다.');
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <Dialog open={isOpen} onOpenChange={onOpenChange}>
            <DialogContent className="sm:max-w-[400px]">
                <DialogHeader>
                    <DialogTitle>새 폴더 만들기</DialogTitle>
                    <DialogDescription>
                        관심종목을 담을 새로운 폴더 이름을 입력해주세요.
                    </DialogDescription>
                </DialogHeader>

                <div className="py-4">
                    <Input
                        placeholder="폴더 이름 (예: 미국 테크주)"
                        value={folderName}
                        onChange={(e) => setFolderName(e.target.value)}
                        onKeyDown={(e) => e.key === 'Enter' && handleSubmit()}
                        autoFocus
                    />
                </div>

                <DialogFooter>
                    <Button variant="outline" onClick={() => onOpenChange(false)}>
                        취소
                    </Button>
                    <Button onClick={handleSubmit} disabled={!folderName.trim() || isSubmitting}>
                        {isSubmitting ? <Loader2 className="h-4 w-4 animate-spin" /> : '만들기'}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}