import { Link } from "@tanstack/react-router";
import { DollarSign, LayoutDashboard, LineChart, Star } from "lucide-react";

export function Sidebar() {
    return (
        <nav className="h-full w-full overflow-y-auto space-y-1 pr-3">
            <NavItem to="/" icon={<LineChart className="h-4 w-4" />}>
                시장 요약
            </NavItem>
            <NavItem to="/market/fx" icon={<DollarSign className="h-4 w-4" />}>
                환율
            </NavItem>

            <div className="my-2 border-t" />

            <NavItem to="/watchlists" icon={<Star className="h-4 w-4" />}>
                관심 종목
            </NavItem>
            <NavItem to="/dashboard" icon={<LayoutDashboard className="h-4 w-4" />}>
                내 포트폴리오
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
            activeProps={{
                className: "bg-muted font-medium text-primary"
            }}
            className="flex items-center gap-2 rounded-md px-3 py-2 text-sm text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
        >
            <span className="flex h-4 w-4 items-center justify-center">{props.icon}</span>
            <span>{props.children}</span>
        </Link>
    )
}