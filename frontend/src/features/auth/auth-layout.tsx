import { TrendingUp } from 'lucide-react'
import { Link } from '@tanstack/react-router'

type AuthLayoutProps = {
  children: React.ReactNode
}

export function AuthLayout({ children }: AuthLayoutProps) {
  return (
    <div className='container grid h-svh max-w-none items-center justify-center'>
      <div className='mx-auto flex w-full flex-col justify-center space-y-2 py-8 sm:w-[480px] sm:p-8'>
        <Link
          to="/"
          className='mb-4 flex items-center justify-center transition-opacity hover:opacity-80'
        >
          <TrendingUp className='me-2 text-primary' />
          <h1 className='text-xl font-medium'>StockMatch</h1>
        </Link>
        
        {children}
      </div>
    </div>
  )
}
