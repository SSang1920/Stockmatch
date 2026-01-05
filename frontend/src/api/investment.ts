import {useAuthStore} from '@/stores/auth-store';

interface InvestmentRequest {
    totalScore: number;
    rawAnswers: string;
}

export const submitInvestmentProfile = async (data: InvestmentRequest) => {

    const token = useAuthStore.getState().auth.accessToken;

    if (!token) {
        throw new Error("로그인이 필요합니다.");
        }

    const response = await fetch('api/user/me/investment-profile', {
        method: 'POST',
        headers: {
            'Content-Type' : 'application/json',
            'Authorization' : `Bearer ${token}`,
            },
        body: JSON.stringify(data),
        });

    if (!response.ok) {
        throw new Error('투자 성향 저장 실패');
        }

    return response.status;
};