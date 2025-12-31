import { Link } from "@tanstack/react-router";
import { TrendingUp } from "lucide-react";
import { Button } from "../ui/button";

export function Header() {
    return (
        <header className="border-b bg-background">
            <div className="mx-auto flex max-w-7xl items-center justify-between p-4">
                {/* 로고 영역 */}
                <Link to="/" className="flex items-center gap-2">
                    <div className="flex h-9 w-9 items-center justify-center rounded-lg border bg-background">
                        <TrendingUp className="h-5 w-5" />
                    </div>
                    <div className="leading-tight">
                        <div className="text-sm font-semibold">StockMatch</div>
                        <div className="text-xs text-muted-foreground">
                            Market · Portfolio
                        </div>
                    </div>
                </Link>

                {/* 우측 버튼 영역 */}
                <div className="flex gap-2">
                    <Button asChild variant="outline">
                        <Link to="/sign-in">로그인</Link>
                    </Button>
                    <Button asChild>
                        <Link to="/dashboard">대시보드</Link>
                    </Button>
                </div>
            </div>
        </header>
    )
}