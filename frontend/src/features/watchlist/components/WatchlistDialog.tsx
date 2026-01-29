import { Button } from "@/components/ui/button"
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { useEffect, useState } from "react"

interface WatchlistDialogProps {
    open: boolean
    onOpenChange: (open: boolean) => void
    mode: 'create' | 'edit'
    initialName?: string
    onSubmit: (name: string) => Promise<void>
}

export function WatchlistDialog({ open, onOpenChange, mode, initialName = '', onSubmit }: WatchlistDialogProps) {
    const [name, setName] = useState(initialName)
    const [loading, setLoading] = useState(false)

    useEffect(() => {
        if (open) setName(initialName)
    }, [open, initialName])

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault()
        if (!name.trim()) return

        try {
            setLoading(true)
            await onSubmit(name)
            onOpenChange(false)
            setName('')
        } catch (error) {
            console.error(error)
            alert('오류가 발생했습니다.')
        } finally {
            setLoading(false)
        }
    }

    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent className="sm:max-w-[425px]">
                <DialogHeader>
                    <DialogTitle>{mode === 'create' ? '새 폴더 만들기' : '폴더 이름 수정'}</DialogTitle>
                    <DialogDescription>폴더 이름을 입력해주세요.</DialogDescription>
                </DialogHeader>
                <form onSubmit={handleSubmit} className="space-y-4 py-4">
                    <Input
                        value={name}
                        onChange={(e) => setName(e.target.value)}
                        placeholder="예: 반도체, 내 연금"
                        autoFocus
                    />
                    <DialogFooter>
                        <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>취소</Button>
                        <Button type="submit" disabled={loading}>{loading ? '저장 중...' : '저장'}</Button>
                    </DialogFooter>
                </form>
            </DialogContent>
        </Dialog>
    )
}