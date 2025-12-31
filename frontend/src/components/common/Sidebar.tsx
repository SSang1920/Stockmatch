import { Link } from "@tanstack/react-router";
import { DollarSign, LayoutDashboard, LineChart, Search } from "lucide-react";

export function Sidebar() {
    return (
        <aside className="col-span-12 md:col-span-3 lg:col-span-2">
            <nav className="space-y-1 sticky top-20">
                <NavItem to="/" icon={<LineChart className="h-4 w-4" />}>
                    시장 요약
                </NavItem>
                <NavItem to="/market/search" icon={<Search className="h-4 w-4" />}>
                    종목 검색
                </NavItem>
                <NavItem to="/market/fx" icon={<DollarSign className="h-4 w-4" />}>
                    환율
                </NavItem>

                <div className="my-2 border-t" />

                <NavItem to="/dashboard" icon={<LayoutDashboard className="h-4 w-4" />}>
                    내 포트폴리오
                </NavItem>
            </nav>
        </aside>
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
            className="flex items-center gap-2 rounded-md px-3 py-2 text-sm hover:bg-muted"
        >
            <span className="text-muted-foreground">{props.icon}</span>
            <span>{props.children}</span>
        </Link>
    )
}