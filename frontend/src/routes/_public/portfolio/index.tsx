import PortfolioPage from "@/features/portfolio/PortfolioPage";
import { createFileRoute } from "@tanstack/react-router";

export const Route = createFileRoute('/_public/portfolio/')({
    component: PortfolioIndex,
})

export default function PortfolioIndex() {
    return <PortfolioPage />;
}