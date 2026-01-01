import { Link } from "@tanstack/react-router";
import { LogOut, TrendingUp, User } from "lucide-react";
import { Button } from "../ui/button";

export function Header() {
    const isLoggedIn = false;

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
                    {isLoggedIn ? (
                        // 로그인 상태일 때: 내 프로필, 로그아웃
                        <>
                            <Button variant="ghost" asChild>
                                <Link to="/profile" className="flex items-center gap-2">
                                    <User className="h-4 w-4" />
                                    <span>내 프로필</span>
                                </Link>
                            </Button>
                            <Button variant="outline" className="gap-2">
                                <LogOut className="h-4 w-4" />
                                <span>로그아웃</span>
                            </Button>
                        </>
                    ) : (
                        // 비로그인 상태일 때: 로그인 버튼
                        <Button asChild>
                            <Link to="/sign-in">로그인</Link>
                        </Button>
                    )}
                </div>
            </div>
        </header>
    )
}