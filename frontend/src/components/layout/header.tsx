import React, { useState, useEffect } from 'react'
import axios from 'axios' // axios 임포트 필수
import { cn } from '@/lib/utils'
import { Separator } from '@/components/ui/separator'
import { SidebarTrigger } from '@/components/ui/sidebar'
import { Button } from '@/components/ui/button'
import { Link } from '@tanstack/react-router'

axios.defaults.withCredentials = true; // 쿠키 포함 설정

type HeaderProps = React.HTMLAttributes<HTMLElement> & {
  fixed?: boolean
  right?: React.ReactNode
}

export function Header({ className, fixed, right, children, ...props }: HeaderProps) {
  const [offset, setOffset] = useState(0)

  const [user, setUser] = useState<any>(null);

  useEffect(() => {
    const fetchUser = async () => {
      try {
        const response = await axios.get('http://localhost:8080/api/user/me');
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

  useEffect(() => {
    const onScroll = () => {
      setOffset(document.body.scrollTop || document.documentElement.scrollTop)
    }
    document.addEventListener('scroll', onScroll, { passive: true })
    return () => document.removeEventListener('scroll', onScroll)
  }, [])

  const handleLogin = () => {
    window.location.href = '/sign-in';
  }

  const handleLogout = async () => {
      try {
        await axios.post('http://localhost:8080/api/auth/logout');
        console.log("서버 로그아웃 성공");
      } catch (error) {
        console.error("로그아웃 요청 중 에러 발생 ", error);
      } finally {
          document.cookie = 'accessToken=; Max-Age=0; path=/';
          document.cookie = 'refreshToken=; Max-Age=0; path=/';

          setUser(null);
          //  메인 페이지로 이동하면서 새로고침
          window.location.href = '/';
      }
    }

  return (
    <header
      className={cn(
        'z-50 h-16 w-full border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60',
                fixed && 'sticky top-0',
                offset > 10 && fixed ? 'shadow' : 'shadow-none',
                className
              )}
      {...props}
    >
      <div className="mx-auto flex h-full max-w-7xl items-center justify-between px-6">

        <div className="flex items-center gap-2">
          {children}
        </div>

        <div className="flex items-center gap-2">
          {right}
          {/* 유저 정보가 있으면 이름 표시, 없으면 로그인 버튼 */}
          {user ? (
            <div className='flex items-center gap-3'>
              <span className='text-sm font-semibold text-slate-700 hidden sm:block'>
                {user.name}님 환영합니다
              </span>
              <Button variant='outline' size='sm' onClick={handleLogout}>
                로그아웃
              </Button>
            </div>
          ) : (
              <div className="flex gap-2">
            <Button onClick={handleLogin} variant='default' size='sm'>
              로그인
            </Button>
            </div>
          )}
        </div>
      </div>
    </header>
  )
}