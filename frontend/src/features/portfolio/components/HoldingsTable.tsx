import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { HoldingItem } from "../types/portfolio";
import { useDeleteHolding } from "../hooks/usePortfolio";
import { Button } from "@/components/ui/button";
import { Edit2, Loader2, Trash2 } from "lucide-react";
import { useState } from "react";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Link } from "@tanstack/react-router";

interface HoldingsTableProps {
    holdings: HoldingItem[];
    usdToKrwRate: number;
    onEdit: (holding: HoldingItem) => void;
}

export function HoldingsTable({ holdings, usdToKrwRate, onEdit }: HoldingsTableProps) {
    const { mutate: deleteHolding, isPending } = useDeleteHolding();

    // 삭제 모달 상태 관리
    const [deleteModalOpen, setDeleteModalOpen] = useState(false);
    const [selectedToDelete, setSelectedToDelete] = useState<HoldingItem | null>(null);

    const openDeleteModal = (item: HoldingItem) => {
        setSelectedToDelete(item);
        setDeleteModalOpen(true);
    };

    const confirmDelete = () => {
        if (selectedToDelete?.holdingId) {
            deleteHolding(selectedToDelete.holdingId, {
                onSuccess: () => setDeleteModalOpen(false)
            });
        }
    };

    return (
        <>
            <Card className="shadow-sm border-none bg-white h-full">
                <CardHeader className="border-b pb-4">
                    <div className="flex items-center justify-between">
                        <CardTitle className="text-lg">보유 종목</CardTitle>
                        <span className="text-xs text-gray-400">기준 환율: {(usdToKrwRate || 0).toLocaleString()}원/$</span>
                    </div>
                </CardHeader>
                <CardContent className="p-0">
                    <div className="overflow-x-auto">
                        <table className="w-full text-sm text-left">
                            <thead className="bg-gray-50 text-gray-500">
                                <tr>
                                    <th className="px-4 py-3 font-medium">종목명</th>
                                    <th className="px-4 py-3 font-medium text-right">보유수량</th>
                                    <th className="px-4 py-3 font-medium text-right">현재가</th>
                                    <th className="px-4 py-3 font-medium text-right">평가금액</th>
                                    <th className="px-4 py-3 font-medium text-right">평가손익</th>
                                    <th className="px-4 py-3 font-medium text-right">수익률</th>
                                    <th className="px-4 py-3 font-medium text-center">관리</th>
                                </tr>
                            </thead>
                            <tbody className="divide-y">
                                {holdings.map((item) => {
                                    const isPlus = (item.pnlAmount || 0) >= 0;
                                    const colorClass = isPlus ? "text-red-500" : "text-blue-500";
                                    const marketParam = item.currency === 'USD' ? 'US' : 'KR';

                                    return (
                                        <tr key={item.ticker} className="hover:bg-gray-50 transition-colors">
                                            <td className="px-4 py-3">
                                                <Link
                                                    to="/stocks/$market/$ticker"
                                                    params={{
                                                        market: marketParam,
                                                        ticker: item.ticker
                                                    }}
                                                    className="Group"
                                                >
                                                    <div className="font-bold text-gray-800 group-hover:text-blue-600 transition-colors">
                                                        {item.krName || item.name}
                                                    </div>
                                                    <div className="text-xs text-gray-500 group-hover:text-blue-400 transition-colors">
                                                        {item.ticker}
                                                    </div>
                                                </Link>

                                            </td>
                                            <td className="px-4 py-3 text-right">{(item.quantity || 0).toLocaleString()}주</td>

                                            <td className="px-4 py-3 text-right font-medium">
                                                {item.currency === "USD" ? "$" : ""}
                                                {(item.currentPrice || 0).toLocaleString()}
                                                {item.currency === "KRW" ? "원" : ""}
                                            </td>

                                            <td className="px-4 py-3 text-right font-medium">
                                                <div>{Math.floor(item.value || 0).toLocaleString()}원</div>
                                                {item.currency === "USD" && (
                                                    <div className="text-xs text-gray-400">
                                                        ${((item.currentPrice || 0) * (item.quantity || 0)).toFixed(2)}
                                                    </div>
                                                )}
                                            </td>
                                            <td className={`px-4 py-3 text-right font-medium ${colorClass}`}>
                                                {isPlus ? "+" : ""}{Math.floor(item.pnlAmount || 0).toLocaleString()}원
                                            </td>
                                            <td className={`px-4 py-3 text-right font-bold ${colorClass}`}>
                                                {(item.pnlRate || 0) >= 0 ? "+" : ""}{((item.pnlRate || 0) * 100).toFixed(2)}%
                                            </td>
                                            <td className="px-4 py-3 text-center space-x-1">
                                                {/* 수정 버튼 */}
                                                <Button variant="ghost" size="icon" className="h-8 w-8 text-gray-400 hover:text-blue-500" onClick={() => onEdit(item)}>
                                                    <Edit2 className="h-4 w-4" />
                                                </Button>
                                                {/* 삭제 버튼 */}
                                                <Button variant="ghost" size="icon" className="h-8 w-8 text-gray-400 hover:text-red-500" onClick={() => openDeleteModal(item)}>
                                                    <Trash2 className="h-4 w-4" />
                                                </Button>
                                            </td>
                                        </tr>
                                    );
                                })}
                            </tbody>
                        </table>
                    </div>
                </CardContent>
            </Card>

            {/* 커스텀 삭제 확인 모달 */}
            <Dialog open={deleteModalOpen} onOpenChange={setDeleteModalOpen}>
                <DialogContent className="sm:max-w-[400px] bg-white">
                    <DialogHeader>
                        <DialogTitle>종목 삭제</DialogTitle>
                        <DialogDescription className="mt-2">
                            정말로 <strong className="text-gray-900">
                                {selectedToDelete?.currency === 'USD'
                                    ? `${selectedToDelete?.krName || selectedToDelete?.name}(${selectedToDelete?.ticker})`
                                    : `${selectedToDelete?.name}(${selectedToDelete?.ticker})`}
                            </strong> 종목을 삭제하시겠습니까? <br />이 작업은 되돌릴 수 없습니다.
                        </DialogDescription>
                    </DialogHeader>
                    <DialogFooter className="mt-4">
                        <Button variant="outline" onClick={() => setDeleteModalOpen(false)}>취소</Button>
                        <Button variant="destructive" onClick={confirmDelete} disabled={isPending}>
                            {isPending ? <Loader2 className="h-4 w-4 animate-spin mr-2" /> : null}
                            삭제하기
                        </Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>
        </>
    )
}