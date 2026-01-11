import { useState } from 'react';
import { createFileRoute, useNavigate, Link } from '@tanstack/react-router';
import { useMutation } from '@tanstack/react-query';
import { toast } from 'sonner';
import { Header } from '@/components/common/Header';
import { SURVEY_QUESTIONS } from '@/config/survey-data';
import { submitInvestmentProfile } from '@/api/investment';

export const Route = createFileRoute('/survey')({
    component: SurveyPage,
    });


function SurveyPage() {
    const navigate = useNavigate();
    const [answers, setAnswers] = useState<Record<number,number>>({});

    //API 요청 관리
    const mutation = useMutation({
        mutationFn: submitInvestmentProfile,
        onSuccess: () => {
            toast.success("투자 성향 분석이 완료되었습니다!");
            navigate({ to: '/' }); // 완료 후 메인 페이지 이동
            },
        onError: (error) => {
            console.error(error);
            toast.error(error.message || "저장 중 오류가 발생했습니다.");
            }
        });

    // 답변 선택 핸들러
    const handleSelect = (questionId: number, score: number) => {
        setAnswers((prev) => ({
            ...prev,
            [questionId]: score,
            }));
        };

    // 제출 핸들러
    const handleSubmit = () => {
        // 유효성 검사 (답변 누락 검사)
        if (Object.keys(answers).length < SURVEY_QUESTIONS.length) {
            toast.error("모든 질문에 답변해주세요.");
            return;
            }

        // 총점 계산
        const totalScore = Object.values(answers).reduce((acc, curr) => acc + curr, 0);

        //데이터 전송
        mutation.mutate({
            totalScore: totalScore,
            rawAnswers: JSON.stringify(answers),
            });
        };

    return (
        <div className="min-h-screen bg-gray-50">
                    <Header />

        <div className="max-w-3xl mx-auto p-6 py-10">
          <h1 className="text-3xl font-bold mb-8 text-center text-gray-800">
            투자 성향 분석
          </h1>

          <div className="space-y-8">
            {SURVEY_QUESTIONS.map((q) => (
              <div key={q.id} className="p-6 border border-gray-200 rounded-xl shadow-sm bg-white">
                <h3 className="text-lg font-bold mb-4 text-gray-700">
                  Q{q.id}. {q.question}
                </h3>

                <div className="grid gap-3">
                  {q.options.map((opt, idx) => (
                    <button
                      key={idx}
                      onClick={() => handleSelect(q.id, opt.score)}
                      className={`p-4 text-left rounded-lg transition-all border
                        ${answers[q.id] === opt.score
                          ? 'bg-blue-600 text-white border-blue-600 shadow-md ring-2 ring-blue-300'
                          : 'hover:bg-gray-50 border-gray-200 text-gray-600'
                        }`}
                    >
                      {opt.text}
                    </button>
                  ))}
                </div>
              </div>
            ))}
          </div>

          <div className="mt-10 flex justify-center">
            <button
              onClick={handleSubmit}
              disabled={mutation.isPending}
              className="px-10 py-4 bg-blue-600 text-white text-lg font-bold rounded-xl hover:bg-blue-700 disabled:opacity-50 transition-colors shadow-lg"
            >
              {mutation.isPending ? "분석 중..." : "결과 제출하기"}
            </button>
          </div>
        </div>
       </div>
     );
}