import { Link, useMatchRoute, useRouterState } from "@tanstack/react-router";
import { DollarSign, LayoutDashboard, LineChart, Star, BarChart3, ChevronDown, Receipt, ChevronRight } from "lucide-react";
import { useState } from "react";

export function Sidebar() {
    const routerState = useRouterState();
    const matchRoute = useMatchRoute();
    const isPortfolioActive = matchRoute({ to: '/portfolio', fuzzy: true });
    const isAnalysisActive = matchRoute({ to: '/analysis', fuzzy: true });

    // 메뉴 열림/닫힘 상태 관리
    const [isPortfolioOpen, setIsPortfolioOpen] = useState(!!isPortfolioActive);
    const [isAnalysisOpen, setIsAnalysisOpen] = useState(!!isAnalysisActive);

    // 새로고침
    const handleForcedReload = (path: string) => {
        if (routerState.location.pathname === path) {
            window.location.reload();
        }
    };

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

            <div className="space-y-1">
                <button
                    onClick={() => setIsPortfolioOpen(!isPortfolioOpen)}
                    className={`flex w-full items-center justify-between rounded-md px-3 py-2 text-sm transition-colors ${isPortfolioActive
                            ? "text-foreground font-medium"
                            : "text-muted-foreground hover:bg-muted hover:text-foreground"
                        }`}
                >
                    <div className="flex items-center gap-2">
                        <span className="flex h-4 w-4 items-center justify-center">
                            <LayoutDashboard className="h-4 w-4" />
                        </span>
                        <span>내 포트폴리오</span>
                    </div>
                    {isPortfolioOpen ? <ChevronDown className="h-4 w-4" /> : <ChevronRight className="h-4 w-4" />}
                </button>

                {/* 하위 탭 */}
                {isPortfolioOpen && (
                    <div className="ml-4 space-y-1 border-l pl-2 mt-1">
                        <NavItem to="/portfolio" exact icon={<LayoutDashboard className="h-4 w-4" />}>
                            자산 현황
                        </NavItem>
                        <NavItem to="/portfolio/transactions" icon={<Receipt className="h-4 w-4" />}>
                            거래 내역
                        </NavItem>
                    </div>
                )}
            </div>

            <div className="space-y-1">
                <button
                    onClick={() => setIsAnalysisOpen(!isAnalysisOpen)}
                    className={`flex w-full items-center justify-between rounded-md px-3 py-2 text-sm transition-colors ${isAnalysisActive
                            ? "text-foreground font-medium"
                            : "text-muted-foreground hover:bg-muted hover:text-foreground"
                        }`}
                >
                    <div className="flex items-center gap-2">
                        <span className="flex h-4 w-4 items-center justify-center">
                            <LayoutDashboard className="h-4 w-4" />
                        </span>
                        <span>AI 분석</span>
                    </div>
                    {isAnalysisOpen ? <ChevronDown className="h-4 w-4" /> : <ChevronRight className="h-4 w-4" />}
                </button>

                {/* 하위 탭 */}
                {isAnalysisOpen && (
                    <div className="ml-4 space-y-1 border-l pl-2 mt-1">
                        <NavItem to="/analysis/myPortfolio" icon={<Receipt className="h-4 w-4" />}
                        onClick={(e) => {
                            if (location.pathname === '/analysis/myPortfolio') {
                                window.location.reload();
                            }
                        }}>
                            내 포트폴리오 분석
                        </NavItem>
                        <NavItem to="/analysis/stock" icon={<Receipt className="h-4 w-4" />}
                        onClick={(e) => {
                            if (location.pathname === '/analysis/stock') {
                                window.location.reload();
                            }
                        }}>
                            포트폴리오 추가 적합 여부
                        </NavItem>
                        <NavItem to="/analysis/financialStatement" icon={<Receipt className="h-4 w-4" />}
                        onClick={(e) => {
                            if (location.pathname === '/analysis/financialStatement') {
                                window.location.reload();
                            }
                        }}>
                            제무재표 분석
                        </NavItem>
                    </div>
                )}
            </div>



        </nav>
    )
}

function NavItem(props: {
    to: string
    icon: React.ReactNode
    children: React.ReactNode
    exact?: boolean
    onClick?: React.MouseEventHandler
}) {
    return (
        <Link
            to={props.to}
            onClick={props.onClick}
            activeProps={{
                className: "bg-muted font-medium text-primary"
            }}
            activeOptions={{ exact: props.exact }}
            className="flex items-center gap-2 rounded-md px-3 py-2 text-sm text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
        >
            <span className="flex h-4 w-4 items-center justify-center">{props.icon}</span>
            <span>{props.children}</span>
        </Link>
    )
}