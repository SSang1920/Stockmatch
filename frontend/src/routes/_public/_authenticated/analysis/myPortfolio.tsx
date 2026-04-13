import { createFileRoute } from '@tanstack/react-router'

import { MyPortfolioDashboard } from '@/features/analysis/components/my-portfolio/MyPortfolioDashboard';

export const Route = createFileRoute(
  '/_public/_authenticated/analysis/myPortfolio',)({
  component: PortfolioAnalysisPage,
})

function PortfolioAnalysisPage() {
  return (
     <div className="p-4 max-w-7xl mx-auto">
       <MyPortfolioDashboard />
     </div>
   );
}
