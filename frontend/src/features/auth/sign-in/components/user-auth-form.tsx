import { useState } from 'react'
import { Loader2 } from 'lucide-react'
import { cn } from '@/lib/utils'
import { Button } from '@/components/ui/button'
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/components/ui/form'
import { Input } from '@/components/ui/input'
import { PasswordInput } from '@/_archive/components/password-input'


interface UserAuthFormProps extends React.HTMLAttributes<HTMLDivElement> {
}

export function UserAuthForm({className, ...props}: UserAuthFormProps) {
  const [isLoading, setIsLoading] = useState<string | null>(null)

  const handleSocialLogin = (provider: string) => {
      setIsLoading(provider)

      const BACKEND_CALLBACK_URL = 'http://localhost:8080/api/auth/callback';

        // 1. 카카오 로그인
      if (provider === 'kakao') {
            const CLIENT_ID = import.meta.env.VITE_KAKAO_CLIENT_ID;
            const REDIRECT_URI = `${BACKEND_CALLBACK_URL}/kakao`;

            window.location.href = `https://kauth.kakao.com/oauth/authorize?client_id=${CLIENT_ID}&redirect_uri=${REDIRECT_URI}&response_type=code`;
          }

          // 2. 네이버 로그인
          else if (provider === 'naver') {
            const CLIENT_ID = import.meta.env.VITE_NAVER_CLIENT_ID;
            const REDIRECT_URI = `${BACKEND_CALLBACK_URL}/naver`;
            const STATE = 'false'; // 보안용 랜덤 문자열

            window.location.href = `https://nid.naver.com/oauth2.0/authorize?client_id=${CLIENT_ID}&redirect_uri=${REDIRECT_URI}&response_type=code&state=${STATE}`;
          }

          // 3. 구글 로그인
          else if (provider === 'google') {
            const CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID;
            const REDIRECT_URI = `${BACKEND_CALLBACK_URL}/google`;
            const SCOPE = 'email profile';

            window.location.href = `https://accounts.google.com/o/oauth2/v2/auth?client_id=${CLIENT_ID}&redirect_uri=${REDIRECT_URI}&response_type=code&scope=${SCOPE}`;
          }
     }

 return (
     <div className={cn('grid gap-3', className)} {...props}>

       <Button
         variant='outline'
         type='button'
         disabled={isLoading !== null}
         onClick={() => handleSocialLogin('kakao')}
         className='w-full bg-[#FEE500] hover:bg-[#FEE500]/90 text-black/85 border-none h-11 relative rounded-[12px]'
       >
         {isLoading === 'kakao' ? (
           <Loader2 className='h-4 w-4 animate-spin text-black' />
         ) : (
           <>
             <div className="absolute left-4 top-1/2 -translate-y-1/2">
              <KakaoIcon className="!h-6 !w-6 text-black" />
             </div>
             <span className="font-semibold text-[15px]">카카오 로그인</span>
           </>
         )}
       </Button>

       <Button
         variant='outline'
         type='button'
         disabled={isLoading !== null}
         onClick={() => handleSocialLogin('naver')}
         className='w-full bg-[#03C75A] hover:bg-[#03C75A]/90 text-white border-none h-11 relative rounded-md'
       >
         {isLoading === 'naver' ? (
           <Loader2 className='h-4 w-4 animate-spin text-white' />
         ) : (
           <>
             <div className="absolute left-4 top-1/2 -translate-y-1/2">
               <NaverIcon className="!h-5 !w-5 fill-white" />
             </div>
             <span className="font-semibold text-[15px]">네이버 로그인</span>
           </>
         )}
       </Button>


       <Button
         variant='outline'
         type='button'
         disabled={isLoading !== null}
         onClick={() => handleSocialLogin('google')}
         className='w-full bg-white hover:bg-gray-50 text-[#1F1F1F] border border-[#747775] h-11 relative rounded-md'
       >
         {isLoading === 'google' ? (
           <Loader2 className='h-4 w-4 animate-spin' />
         ) : (
           <>
             <div className="absolute left-4 top-1/2 -translate-y-1/2">
               <GoogleIcon className="!h-6 !w-6" />
             </div>
             <span className="font-medium text-[15px]">Google 계정으로 로그인</span>
           </>
         )}
       </Button>
     </div>
   )
 }

function KakaoIcon({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 24 24" className={className} xmlns="http://www.w3.org/2000/svg" fill="currentColor">
      <path d="M12 3C7.58 3 4 5.79 4 9.24c0 2.16 1.41 4.06 3.55 5.14-.14.51-.53 1.86-.6 2.16-.09.39.14.38.29.28.2.14 2.2-1.48 3.08-2.07.56.08 1.13.12 1.68.12 4.42 0 8-2.79 8-6.24S16.42 3 12 3z"/>
    </svg>
  )
}

function NaverIcon({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 24 24" className={className} xmlns="http://www.w3.org/2000/svg" fill="currentColor">
       <path d="M16.273 12.845L7.376 0H0v24h7.726V11.156L16.624 24H24V0h-7.727v12.845z"/>
    </svg>
  )
}

function GoogleIcon({ className }: { className?: string }) {
  return (
      <svg viewBox="0 0 24 24" className={className} xmlns="http://www.w3.org/2000/svg">
         <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" fill="#4285F4" />
         <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853" />
         <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.26.81-.58z" fill="#FBBC05" />
         <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" fill="#EA4335" />
      </svg>
  )
}