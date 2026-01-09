import {useAuthStore} from '@/stores/auth-store';

interface InvestmentRequest {
    totalScore: number;
    rawAnswers: string;
}

export const submitInvestmentProfile = async (data: InvestmentRequest) => {

    const response = await fetch('/api/user/me/investment-profile', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(data),
            //  쿠키를 백엔드로 보내기 위해 추가해야 함
            credentials: 'include',
        });

        if (response.status === 401) {
            throw new Error("로그인 세션이 만료되었습니다. 다시 로그인해주세요.");
        }

        if (!response.ok) {
            throw new Error('투자 성향 저장 실패');
        }

        return response.status;
};