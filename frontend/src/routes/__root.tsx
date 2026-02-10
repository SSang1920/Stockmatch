import { useState, useEffect } from 'react'
import { ensurePortfolio } from '@/api/user';
import { type QueryClient } from '@tanstack/react-query'
import { createRootRouteWithContext, Outlet } from '@tanstack/react-router'
import { ReactQueryDevtools } from '@tanstack/react-query-devtools'
import { TanStackRouterDevtools } from '@tanstack/react-router-devtools'
import { Toaster } from '@/components/ui/sonner'
import { NavigationProgress } from '@/_archive/components/navigation-progress'
import { GeneralError } from '@/features/errors/general-error'
import { NotFoundError } from '@/features/errors/not-found-error'
import { useUser } from '@/context/UserContext';

export const Route = createRootRouteWithContext<{
  queryClient: QueryClient
}>()({
  component: () => {
    const { user } = useUser();

    useEffect(() => {
          if (user) {
            ensurePortfolio()
              .then(() => console.log("Portfolio checked for:", user.name))
              .catch((err) => console.error("Portfolio check failed:", err));
          }
        }, [user]);

    return (
      <>
        <NavigationProgress />

        <Outlet context={{ user }} />
        <Toaster
          position="top-center"
          duration={3000}
          toastOptions={{
            style: {
              background: '#333333',
              color: '#ffffff',
              border: 'none',
              borderRadius: '16px',
              fontSize: '15px',
              boxShadow: '0 4px 12px rgba(0, 0, 0, 0.15)',
            },
          }}
        />
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