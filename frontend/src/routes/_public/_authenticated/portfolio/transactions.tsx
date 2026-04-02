import TransactionHistoryPage from '@/features/portfolio/TransactionHistoryPage'
import { createFileRoute } from '@tanstack/react-router'

export const Route = createFileRoute(
  '/_public/_authenticated/portfolio/transactions',
)({
  component: TransactionsIndex,
})

export default function TransactionsIndex() {
  return <TransactionHistoryPage />;
}
