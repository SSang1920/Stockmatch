import { createFileRoute } from '@tanstack/react-router'
import { ComingSoon } from '@/_archive/components/coming-soon'

export const Route = createFileRoute('/_authenticated/help-center/')({
  component: ComingSoon,
})
