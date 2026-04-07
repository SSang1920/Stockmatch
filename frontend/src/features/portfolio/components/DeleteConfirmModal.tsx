import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { AlertTriangle, Loader2 } from "lucide-react";

interface DeleteConfirmModalProps {
    isOpen: boolean;
    onClose: () => void;
    onConfirm: () => void;
    isDeleting: boolean;
}

export function DeleteConfirmModal({ isOpen, onClose, onConfirm, isDeleting }: DeleteConfirmModalProps) {
    return (
        <Dialog open={isOpen} onOpenChange={onClose}>
            <DialogContent className="sm:max-w-[400px] bg-white">
                <DialogHeader className="flex flex-col items-center gap-2 pt-4">
                    <div className="p-3 bg-red-100 rounded-full">
                        <AlertTriangle className="w-6 h-6 text-red-600" />
                    </div>
                    <DialogTitle className="text-xl font-bold text-gray-900 text-center">
                        기록을 삭제하시겠습니까?
                    </DialogTitle>
                </DialogHeader>
                
                <div className="py-2 text-center text-sm text-gray-500">
                    삭제한 데이터는 복구할 수 없습니다.
                </div>

                <DialogFooter className="flex gap-2 mt-4 sm:justify-center">
                    <Button 
                        type="button" 
                        variant="outline" 
                        onClick={onClose}
                        disabled={isDeleting}
                        className="flex-1"
                    >
                        취소
                    </Button>
                    <Button 
                        type="button" 
                        onClick={onConfirm}
                        disabled={isDeleting}
                        className="flex-1 bg-red-600 hover:bg-red-700 text-white"
                    >
                        {isDeleting ? <Loader2 className="w-5 h-5 animate-spin mx-auto" /> : "삭제하기"}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}