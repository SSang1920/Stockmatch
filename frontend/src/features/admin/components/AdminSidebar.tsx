import { Link } from "@tanstack/react-router";
import { LayoutDashboard, Users, FileText, Settings, ShieldAlert } from "lucide-react";

export function AdminSidebar() {
    return (
        <nav className="h-full w-full overflow-y-auto space-y-1 pr-3">
            {/* 1. 메인 대시보드 */}
            <NavItem to="/admin" icon={<LayoutDashboard className="h-4 w-4" />}>
                대시보드
            </NavItem>

            <div className="my-2 border-t" />

            {/* 2. 유저 관리 (추후 개발) */}
            <NavItem to="/admin/users" icon={<Users className="h-4 w-4" />}>
                유저 관리
            </NavItem>

            {/* 3. 포트폴리오 관리 (추후 개발) */}
            <NavItem to="/admin/portfolios" icon={<FileText className="h-4 w-4" />}>
                포트폴리오 관리
            </NavItem>

            {/* 4. 시스템 설정 (추후 개발) */}
            <NavItem to="/admin/settings" icon={<Settings className="h-4 w-4" />}>
                 시스템 설정
            </NavItem>
        </nav>
    )
}

function NavItem(props: {
    to: string
    icon: React.ReactNode
    children: React.ReactNode
}) {
    return (
        <Link
            to={props.to}
            // 현재 페이지일 때 강조 스타일
            activeProps={{
                className: "bg-muted font-medium text-primary"
            }}
            // 기본 스타일 (회색) -> 마우스 올리면 진해짐
            className="flex items-center gap-2 rounded-md px-3 py-2 text-sm text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
        >
            <span className="flex h-4 w-4 items-center justify-center">{props.icon}</span>
            <span>{props.children}</span>
        </Link>
    )
}