import { createFileRoute } from '@tanstack/react-router'
import { FinancialDashboard } from '@/features/analysis/components/financial-statement/FinancialDashboard';

export const Route = createFileRoute(
  '/_public/_authenticated/analysis/financialStatement',
)({
  component: FinancialAnalysisPage,
})

function FinancialAnalysisPage() {
  return (
    <div className="p-4 max-w-7xl mx-auto">
        <FinancialDashboard />
    </div>
  );
}
