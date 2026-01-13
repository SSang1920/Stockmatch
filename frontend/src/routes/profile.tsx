import { useState, useEffect } from 'react';
import { createFileRoute, useNavigate } from '@tanstack/react-router';
import { useMutation } from '@tanstack/react-query';
import { toast } from 'sonner';
import { Header } from '@/components/common/Header';
import { getUserInfo, updateApiKey, fetchDecryptedApiKey, deleteUser } from '@/api/user';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';

export const Route = createFileRoute('/profile')({
  component: RouteComponent,
})

function RouteComponent() {
    const navigate = useNavigate();
    const [user, setUser] = useState<any>(null);
    const [apiKey, setApiKey] = useState('');
    const [hasSavedKey, setHasSavedKey] = useState(false);

    const [isEditing, setIsEditing] = useState(false);
    const [isVisible, setIsVisible] = useState(false);

    useEffect(() => {
        const fetchUser = async () => {
            try {
                const response = await getUserInfo();
                const userData = response.data;

                setUser(userData);

                if(userData.apiKey) {
                    setHasSavedKey(true);
                    setApiKey('');
                }
                else{
                    setHasSavedKey(false);
                }
            } catch (error) {
                console.error("프로필 로딩 실패: ", error);
                toast.error("로그인 정보가 없습니다.");
                }
            };
        fetchUser();
        }, []);

    const mutation = useMutation({
        mutationFn : updateApiKey,
        onSuccess: () => {
            toast.success("Alpha Vantage Key가 저장되었습니다.");
            setHasSavedKey(true);
            setApiKey('');
            },
        onError: () => {
            toast.error("키 저장에 실패했습니다.");
            }
        });

    const deleteMutation = useMutation({
            mutationFn: deleteUser,
            onSuccess: () => {
                toast.success("회원 탈퇴가 완료되었습니다.");

                document.cookie = "accessToken=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;";
                document.cookie = "refreshToken=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;"
                localStorage.clear();

                window.location.href = '/';
            },
            onError: (error :any) => {
                const errorData = error.response?.data;

                const errorCode = errorData?.error?.code || errorData?.code;
                const isGoogleUser = user?.authprovider === 'GOOGLE';

                if (errorCode === 'A008' && isGoogleUser) {
                    if (window.confirm("구글 계정 연동 해제를 위해 보안 재인증이 필요합니다.\n확인을 누르면 구글 로그인 화면으로 이동합니다.")) {
                        const CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID;
                        const REDIRECT_URI = 'http://localhost:8080/api/auth/callback/google';
                        const SCOPE = 'email profile';

                        // prompt=consent와 access_type=offline을 강제로 붙여서 보냄
                        const reauthUrl = `https://accounts.google.com/o/oauth2/v2/auth?client_id=${CLIENT_ID}&redirect_uri=${REDIRECT_URI}&response_type=code&scope=${SCOPE}&prompt=consent&access_type=offline`;

                        window.location.href = reauthUrl;
                    }
                    return;
                }

                console.error("회원 탈퇴 에러:", error);
                toast.error(errorData?.message || "회원 탈퇴에 실패했습니다. 잠시 후 다시 시도해주세요.");
            }
        });

    const handleToggleView = async () => {
            if (isVisible) {
                // 숨기기 클릭 시
                setIsVisible(false);

                if (!isEditing) {
                                setApiKey('');
                            }
            } else {
                // 보기 클릭 시
                try {
                    if (!apiKey && hasSavedKey) {
                        const realKey = await fetchDecryptedApiKey();
                        setApiKey(realKey);
                        toast.info("저장된 키를 불러왔습니다.");
                    }
                    setIsVisible(true);
                } catch (error) {
                    console.error(error);
                    toast.error("키를 불러오는데 실패했습니다.");
                }
            }
        };


    const handleEditStart = () => {
        setIsEditing(true);
        setApiKey('');
        setIsVisible(false);
    };

    const handleCancelEdit = () => {
        setIsEditing(false);
        setApiKey('');
        setIsVisible(false);
    };

    const handleSaveKey = () => {
        if (!apiKey.trim()) {
            toast.error("API 키를 입력해주세요.");
            return;
            }
        mutation.mutate(apiKey);
        };

    const handleDeleteAccount = () => {
            if (window.confirm("정말로 탈퇴하시겠습니까?\n이 작업은 되돌릴 수 없으며, 모든 포트폴리오와 설정 정보가 삭제됩니다.")) {
                deleteMutation.mutate();
            }
        };

    if (!user) {
        return (
            <>
            <Header />
            <div className="flex h-[80vh] items-center justify-center">
                            <p>로딩 중...</p>
                        </div>
            </>
            );
        }

return (
    <div className="min-h-screen bg-slate-50">
      <Header />

      <main className="container mx-auto max-w-2xl px-4 py-10">
        <h1 className="mb-8 text-3xl font-bold text-slate-800">내 프로필</h1>


        <div className="mb-6 rounded-xl border bg-white p-6 shadow-sm">
            <h2 className="mb-4 text-lg font-semibold text-slate-700 border-b pb-2">기본 정보</h2>
            <div className="space-y-4">
                <div className="flex justify-between">
                    <span className="text-slate-500">이름</span>
                    <span className="font-medium">{user.name}</span>
                </div>
                <div className="flex justify-between">
                    <span className="text-slate-500">연동된 소셜 계정</span>
                    <span className="font-medium uppercase text-blue-600">
                        {user.authprovider || "Local"}
                    </span>
                </div>
                <div className="flex justify-between items-center">
                    <span className="text-slate-500">나의 투자 성향</span>

                    <div className="flex items-center gap-1">
                         <span className="rounded-full bg-green-100 px-3 py-1 text-sm font-semibold text-green-700">
                             {user.investmentType || "분석되지 않음"}
                         </span>
                         <Button
                             variant="outline"
                             size="sm"
                             className="h-7 text-xs px-2 text-slate-600 border-slate-200"
                             onClick={() => navigate({ to: '/survey' })}
                         >
                             분석하러 가기
                         </Button>
                     </div>
                </div>
            </div>
        </div>

        <div className="rounded-xl border bg-white p-6 shadow-sm">
            <h2 className="mb-4 text-lg font-semibold text-slate-700 border-b pb-2">서비스 설정</h2>

            <div className="mb-4">
                <label className="mb-2 block text-sm font-medium text-slate-700">
                    Alpha Vantage API Key
                </label>
                <p className="mb-3 text-xs text-slate-500">
                    주식 데이터를 받아오기 위해 필요한 개인 키입니다.
                    <a href="https://www.alphavantage.co/support/#api-key" target="_blank" className="ml-1 text-blue-500 underline">
                        여기서 발급
                    </a> 받을 수 있습니다.
                </p>
                <div className="flex gap-2">
                    {/* 인풋 창: 보기 모드에 따라 type 변경, 수정 모드 아니면 비활성화 */}
                    <Input
                        type={isVisible ? "text" : "password"}
                        placeholder={hasSavedKey && !isEditing ? "****************" : "API Key를 입력하세요"}
                        value={apiKey}
                        onChange={(e) => setApiKey(e.target.value)}
                        disabled={hasSavedKey && !isEditing}
                        className={`flex-1 ${!isEditing && hasSavedKey ? "bg-slate-100 text-slate-500 cursor-not-allowed" : ""}`}
                    />

                    {/* 1. 보기/숨기기 버튼: 키가 저장되어 있을 때만 표시 */}
                    {hasSavedKey && (
                        <Button
                            onClick={handleToggleView} //
                            variant="outline"
                            type="button"
                            className="w-20"
                        >
                           {isVisible ? "숨기기" : "보기"}
                        </Button>
                    )}

                    {hasSavedKey && !isEditing ? (
                        <Button
                            onClick={handleEditStart}
                            variant="secondary"
                            type="button"
                        >
                             수정
                        </Button>
                    ) : (
                        <>
                            {hasSavedKey && isEditing && (
                                <Button
                                    onClick={handleCancelEdit}
                                    variant="ghost"
                                    type="button"
                                >
                                    취소
                                </Button>
                            )}

                            <Button
                                onClick={handleSaveKey}
                                disabled={mutation.isPending}
                            >
                                {mutation.isPending ? "저장 중..." : "저장"}
                            </Button>
                        </>
                        )}
                </div>
            </div>
        </div>

        <div className="mt-6 rounded-xl border border-red-100 bg-red-50 p-6 shadow-sm">
            <h2 className="mb-4 text-lg font-semibold text-red-700 border-b border-red-200 pb-2">회원 탈퇴</h2>
            <div className="flex items-center justify-between">
                <div className="text-sm text-red-600">
                    <p>StockMatch 서비스를 더 이상 이용하지 않으시려면 회원 탈퇴를 진행해주세요.</p>
                    <p className="mt-1 font-medium">※ 탈퇴 시 모든 포트폴리오 및 설정 정보가 즉시 삭제됩니다.</p>
                </div>
                <Button
                    variant="destructive"
                    onClick={handleDeleteAccount}
                    disabled={deleteMutation.isPending}
                >
                    {deleteMutation.isPending ? "처리 중..." : "회원 탈퇴"}
                </Button>
            </div>
        </div>

      </main>
    </div>
  )
}
