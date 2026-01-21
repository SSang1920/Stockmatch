import { createFileRoute } from '@tanstack/react-router';

import { AnalysisDashboard } from '@/features/analysis/components/AnalysisDashboard';


export const Route = createFileRoute('/_public/analysis/')({
  component: AnalysisPage,
});

function AnalysisPage() {
  return (
    <div className="p-4 max-w-7xl mx-auto">
      <AnalysisDashboard />
    </div>
  );
}