import { IconGoogle, IconKakao, IconNaver } from '@/assets/brand-icons'

export const apps = [
  {
    name: 'Google',
    logo: <IconGoogle />,
    provider: 'GOOGLE',
    connected: false,
    desc: '구글 계정과 연동되어 있습니다.',
  },
  {
    name: 'Kakao',
    logo: <IconKakao />,
    provider: 'KAKAO',
    connected: false,
    desc: '카카오 계정과 연동되어 있습니다.',
  },
  {
    name: 'Naver',
    logo: <IconNaver />,
    provider: 'NAVER',
    connected: false,
    desc: '네이버 계정과 연동되어 있습니다.',
  },

]
