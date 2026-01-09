import React, { useState, useEffect } from "react";
import { Link } from "@tanstack/react-router";
import axios from "axios";
import { LogOut, TrendingUp, User } from "lucide-react";
import { Button } from "../ui/button";

// 쿠키 포함 설정
axios.defaults.withCredentials = true;

export function Header() {
    // 유저 상태 관리
    const [user, setUser] = useState<any>(null);

    // 컴포넌트 마운트 시 유저 정보 확인
    useEffect(() => {
        const fetchUser = async () => {
            try {
                const response = await axios.get("http://localhost:8080/api/user/me");
                if (response.data && response.data.data) {
                    console.log("Header: 유저 정보 로딩 성공", response.data.data.name);
                    setUser(response.data.data);
                }
            } catch (error) {
                console.log("Header: 비로그인 상태");
                setUser(null);
            }
        };
        fetchUser();
    }, []);

    // 로그아웃 핸들러
    const handleLogout = async () => {
        try {
            await axios.post('http://localhost:8080/api/auth/logout');
            console.log("서버 로그아웃 성공");
        } catch (error) {
            console.error("로그아웃 요청 중 에러 발생 ", error);
        } finally {
            // 클라이언트 측 쿠키 삭제 및 상태 초기화
            document.cookie = 'accessToken=; Max-Age=0; path=/';
            document.cookie = 'refreshToken=; Max-Age=0; path=/';

            setUser(null);
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