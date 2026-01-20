import { useState, useEffect } from 'react'
import axios from 'axios'
import { ensurePortfolio } from '@/api/user';
import { type QueryClient } from '@tanstack/react-query'
import { createRootRouteWithContext, Outlet } from '@tanstack/react-router'
import { ReactQueryDevtools } from '@tanstack/react-query-devtools'
import { TanStackRouterDevtools } from '@tanstack/react-router-devtools'
import { Toaster } from '@/components/ui/sonner'
import { NavigationProgress } from '@/_archive/components/navigation-progress'
import { GeneralError } from '@/features/errors/general-error'
import { NotFoundError } from '@/features/errors/not-found-error'

axios.defaults.withCredentials = true;
export const Route = createRootRouteWithContext<{
  queryClient: QueryClient
}>()({
  component: () => {
    // 1. 유저 상태 관리 추가
    const [user, setUser] = useState<any>(null);

    useEffect(() => {
      const checkLogin = async () => {
        try {
          const response = await axios.get('http://localhost:8080/api/user/me');

          if (response.data && response.data.data) {
            setUser(response.data.data); // 유저 정보 저장

            try {
              await ensurePortfolio();
              console.log("Portfolio ensured successfully for:", userData.name);
            } catch (portfolioError) {
              console.error("Portfolio initialization failed:", portfolioError);
            }
          }
        } catch (error) {
          setUser(null);
        }
      };
      checkLogin();
    }, []);

    return (
      <>
        <NavigationProgress />

        <Outlet context={{ user }} />
        <Toaster duration={5000} />
        {import.meta.env.MODE === 'development' && (
          <>
            <ReactQueryDevtools buttonPosition='bottom-left' />
            <TanStackRouterDevtools position='bottom-right' />
          </>
        )}
      </>
    )
  },
  notFoundComponent: NotFoundError,
  errorComponent: GeneralError,
})