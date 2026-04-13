import React, { useState, useEffect } from "react";
import { Link } from "@tanstack/react-router";
import { LogOut, TrendingUp, User, ShieldCheck } from "lucide-react";
import { Button } from "../ui/button";
import { getUserInfo, logoutApi } from "@/api/user";
import { useUser } from "../../context/UserContext";

export function Header() {

    const { user } = useUser();
    // 로그아웃 핸들러
    const handleLogout = async () => {
        try {
            await logoutApi();
            console.log("서버 로그아웃 성공");
        } catch (error) {
            console.error("로그아웃 요청 중 에러 발생 ", error);
        } finally {
            // 로그인 여부 플래그 삭제
            localStorage.removeItem('isLoggedIn');

            // 메인 페이지로 이동하면서 새로고침 (상태 완전 초기화)
            window.location.href = '/';
        }
    };

    return (
        <header className="sticky top-0 z-50 w-full border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
            <div className="container mx-auto flex h-16 items-center justify-between px-4">
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
                <div className="flex items-center gap-2">
                    {user ? (
                        // 로그인 상태일 때
                        <>
                            {/* 사용자 환영 문구 */}
                            <span className="mr-2 hidden text-sm font-medium text-slate-700 sm:block">
                                {user.name}님
                            </span>
                            {user.role === 'ADMIN' && (
                                <Button variant="ghost" asChild className="text-black">
                                    <Link to="/admin" className="flex items-center gap-2">
                                        <ShieldCheck className="h-4 w-4" />
                                        <span className="hidden sm:inline">관리자 페이지</span>
                                    </Link>
                                </Button>
                            )}
                            <Button variant="ghost" asChild>
                                <Link to="/profile" className="flex items-center gap-2">
                                    <User className="h-4 w-4" />
                                    <span className="hidden sm:inline">내 프로필</span>
                                </Link>
                            </Button>
                            
                            <Button 
                                variant="outline" 
                                className="gap-2" 
                                onClick={handleLogout}
                            >
                                <LogOut className="h-4 w-4" />
                                <span>로그아웃</span>
                            </Button>
                        </>
                    ) : (
                        // 비로그인 상태일 때
                        <Button asChild>
                            <Link to="/sign-in">로그인</Link>
                        </Button>
                    )}
                </div>
            </div>
        </header>
    );
}