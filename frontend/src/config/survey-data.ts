export const SURVEY_QUESTIONS = [
    {
        id: 1,
        question: "고객님의 현재 연령대는 어떻게 되십니까?",
        options: [
            { text: "19세 이하", score : 0},
            { text: "20대 ~ 40대", score : 5},
            { text: "50대", score: 4},
            { text: "60대 이상", score : 2},
            ],
        },
    {
        id: 2,
        question: "투자금의 예상 운용 기간은 얼마나 되십니까?",
        options: [
            {text : "6개월 미만", score: 1 },
            {text : "6개월 이상 ~ 1년 미만", score : 2 },
            {text : "1년 이상 ~ 3년 미만", score : 3 },
            {text : "3년 이상", score: 5 },
            ],
        },
    {
        id: 3,
        question: "과거 금융상품 투자 경험은?",
        options: [
            {text: "은행 예적금만 경험", score: 1 },
            {text: "채권형 펀드 등", score: 3 },
            {text: "주식, ETF 등", score: 5 },
            {text: "파생상품, 선물옵션 등", score: 8 },
            ],
        },
    {
        id: 4,
        question: "금융상품에 대한 이해도는?",
        options: [
            {text: "지식 거의 없음", score: 1 },
            {text: "기본 개념을 알고 있음", score: 3 },
            {text: "구조와 위험에 대해 이해 하고 있음", score: 5 },
            {text: "전문적인 지식 보유", score: 7 },
            ],
        },
    {
        id: 5,
        question : "총 자산 대비 투자 비중은?",
        options: [
            {text : "10% 미만", score : 1 },
            {text: "10% 이상 30% 미만", score : 2},
            {text: "30% 이상 50% 미만", score : 3},
            {text: "50% 이상", score : 5},
            ],
        },
    {
        id: 6,
        question : "감내 가능한 손실 수준은?",
        options: [
            {text : "무조건 원금 보전", score : -100 },
            {text : "10% 미만 손실 ", score : 2 },
            {text : "20% 수준 손실", score : 5 },
            {text : "고수익을 위해 위험 감수", score : 10 },
            ],
        },
];