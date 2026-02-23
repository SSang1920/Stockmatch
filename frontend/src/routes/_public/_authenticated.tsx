import { createFileRoute, Outlet, redirect } from '@tanstack/react-router'
import axios from '@/lib/axios'
import { Loader2 } from 'lucide-react';

export const Route = createFileRoute('/_public/_authenticated')({
    beforeLoad: async ({ location }) => {
        try {
            await axios.get('/auth/check');
        } catch (error) {
            throw redirect({
                to: '/sign-in',
                search: {
                    redirect: location.href,
                },
            });
        }
    },

    // 인증 확인 중일 때 보여줄 로딩 화면 
    pendingComponent: () => (
        <div className="flex items-center justify-center h-screen">
            <Loader2 className="h-8 w-8 animate-spin text-primary" />
        </div>
    ),
    component: () => <Outlet />,
});