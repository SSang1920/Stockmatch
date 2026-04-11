import { createFileRoute } from '@tanstack/react-router';

import { AnalysisDashboard } from '@/features/analysis/components/stock-suitability/AnalysisDashboard';

export const Route = createFileRoute('/_public/_authenticated/analysis/stock')({
  component: StockAnalysisPage,
});

function StockAnalysisPage() {
  return (
    <div className="p-4 max-w-7xl mx-auto">
      <AnalysisDashboard />
    </div>
  );
}