import { useEffect } from 'react'
import { ensurePortfolio } from '@/api/user';
import { type QueryClient } from '@tanstack/react-query'
import { createRootRouteWithContext, Outlet } from '@tanstack/react-router'
import { ReactQueryDevtools } from '@tanstack/react-query-devtools'
import { TanStackRouterDevtools } from '@tanstack/react-router-devtools'
import { Toaster } from '@/components/ui/sonner'
import { GeneralError } from '@/features/errors/general-error'
import { NotFoundError } from '@/features/errors/not-found-error'
import { User, useUser } from '@/context/UserContext';

export const Route = createRootRouteWithContext<{
  queryClient: QueryClient
  user: User | null
  isLoading: boolean
}>()({
  component: () => {
    const { user, isLoading } = useUser();
    const AnyOutlet = Outlet as any;

    useEffect(() => {
          if (user) {
            ensurePortfolio()
              .then(() => console.log("Portfolio checked for:", user.name))
              .catch((err) => console.error("Portfolio check failed:", err));
          }
        }, [user]);

    return (
      <>
        <AnyOutlet context={{ user, isLoading }} />
        <Toaster
          richColors={true}
          position="top-center"
          duration={3000}
          toastOptions={{
            style: {
              borderRadius: '16px',
              fontSize: '15px',
              boxShadow: '0 4px 12px rgba(0, 0, 0, 0.15)',
              border: 'none',
            },
            success: {
              style: {
                background: '#ECFDF5',
                color: '#059669',
                border: '1px solid #10B981',
              },
            },
            error: {
              style: {
                background: '#FEF2F2',
                color: '#DC2626',
                border: '1px solid #EF4444',
              },
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