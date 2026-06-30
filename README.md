# Stockmatch

> 생성형 AI와 실시간 금융 API를 결합한 **개인 맞춤형 자산 진단 및 리스크 관리** 웹 애플리케이션

![Java](https://img.shields.io/badge/Java-17-007396?logo=openjdk&logoColor=white)  ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F?logo=springboot&logoColor=white) ![Spring Security](https://img.shields.io/badge/Spring%20Security-6.x-6DB33F?logo=springsecurity&logoColor=white) ![JWT](https://img.shields.io/badge/JWT-000000?logo=jsonwebLogos&logoColor=white)  ![Redis](https://img.shields.io/badge/Redis-7.x-DC382D?logo=redis&logoColor=white) ![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?logo=mysql&logoColor=white)  ![Docker](https://img.shields.io/badge/Docker-2496ED?logo=docker&logoColor=white)  ![Nginx](https://img.shields.io/badge/Nginx-009639?logo=nginx&logoColor=white) ![GitHub Actions](https://img.shields.io/badge/GitHub%20Actions-CI/CD-2088FF?logo=githubactions&logoColor=white) ![OpenAI](https://img.shields.io/badge/OpenAI-GPT--4o-412991?logo=openai&logoColor=white)

---

## 배포 주소
[https://stockmatch.kro.kr](https://stockmatch.kro.kr) (2026-04-30 ~ 서비스 중)

---


## 팀원 소개
| 이름 | Github |
| ----- | ----- |
| 김상준 | https://github.com/Saangjun00 |
| 용상윤 | https://github.com/imnerf242 |

---


## 목차
1. [프로젝트 소개](#프로젝트-소개)  
2. [목표](#목표)  
3. [주요 기능](#주요-기능)  
4. [구현 화면](#구현-화면)  
5. [아키텍처](#아키텍처)  
6. [기술 스택 상세](#기술-스택-상세)  
7. [프로젝트 실행 방법](#프로젝트-실행-방법)  
8. [데이터베이스 설계 (ERD)](#데이터베이스-설계-erd)

---

##  프로젝트 소개
**StockMatch**는 생성형 AI와 실시간 금융 API를 활용하여 개인 맞춤형 자산 진단 및 리스크 관리를 제공하는 웹 애플리케이션입니다.
사용자가 직접 등록한 포트폴리오를 기반으로 자산의 안정성을 진단하고 합리적인 투자 판단을 내릴 수 있도록 백엔드 시스템을 구축했습니다.

- **환각 최소화 정형 리포트**: OpenAI **Strict JSON Schema** 통제를 통해 환각 현상(Hallucination) 없는 재무제표 요약 생성
- **비용 통제 및 트래픽 제어**: Redis 캐시 전략과 속도 제한 가드로 무분별한 외부 API 호출 및 트래픽 폭주 원천 차단
- **온디맨드 실시간 수집**: 한국투자증권(KIS) 금융망과 연동하여 미등록 종목 진입 시 실시간 마스터 데이터를 자동 적재하는 파이프라인 컴포넌트 설계 완료

---


## 목표
- 투자 스타일과 실제 자산 비중 대조를 통한 맞춤형 리스크 피드백 제공
- 엄격한 스키마 구조 설계를 통해 생성형 AI의 금융 데이터 해석 신뢰성 확보
- 다중 외부 금융 API와 환율 레이어를 결합한 무결성 데이터 파이프라인 구축

---


## 주요 기능
- **OAuth2 소셜 로그인 & JWT 인증/인가**(Access / Refresh), Spring Security 보안 계층
- **한국투자증권(KIS) API 연동 및 데이터 통합**: 국내·외 상품기본조회 및 실시간 환율 데이터를 연동하여 자산 간 가치 및 보유 비중 정합성 보정
- **내 포트폴리오 분석**: 투자 성향과 자산 비중 대조 및 자산 쏠림 위험 분류
- **포트폴리오 추가 적합 여부 분석**: 신규 종목 편입 시 기존 자산과의 상관관계를 분석하여 포트폴리오 다각화 효과 및 리스크 변동성 진단
- **재무제표 요약 및 AI 리포트 생성**: Strict JSON Schema 기반 금융 분석 및 토큰 비용 최적화 패키징 구축


### JWT 기반 인증/인가
<details>
  <summary>자세히 보기</summary>

  - **소셜 로그인 연동**: OAuth2 프로토콜 기반 다중 소셜 가입(Google, Kakao, Naver) 및 단일화된 인증 인프라 구축
  - **쿠키(Cookie) 기반 토큰 검증**: 클라이언트 쿠키에서 `accessToken`을 직접 추출·파싱하는 `OncePerRequestFilter` 보안 필터 계층 구현
  - **경량 토큰 및 위조 방지**: 페이로드 크기 최적화 및 `HMAC-SHA` 알고리즘 기반의 `SecretKey` 캐싱 적용, 커스텀 폴백 예외 핸들링
  - **효율적인 세션 관리**: 일반 요청은 쿠키 기반 JWT로 검증하되, 재발급 경로(`/api/auth/refresh`)는 필터를 스킵하도록 최적화
</details>

### 한국투자증권(KIS) API 연동 및 환율 보정
<details>
  <summary>자세히 보기</summary>

  - **국내외 금융 인프라 단일화**: 한국투자증권(KIS) Open API 망으로 데이터 채널을 집중하여 주식 정보 마스터 수집 규격 추상화
  - **원/달러 환율 실시간 보정**: KIS API로부터 실시간 환율 데이터를 직접 파싱 및 연동하여 원화/외화 자산 비중을 정교하게 실시간 가치 보정
  - **Redis Cache-Aside 캐시 최적화**: 조회 주기가 일정한 외부 금융 API 및 환율 데이터에 캐싱 전략을 적용하여 중복 네트워크 호출을 차단하고 트래픽 비용 대폭 절감
</details>

### 내 포트폴리오 분석
<details>
  <summary>자세히 보기</summary>

  - **성향-자산 괴리율 진단**: 사용자의 주관적인 투자 성향(`investmentType`)과 실제 보유 자산의 종목별 비중(`weightPct`) 데이터를 대조하여 리스크 일치 여부를 심층 진단합니다.
  - **자산 쏠림 리스크 탐지**: 단일 종목의 비중이 과도하게 높은 상태를 감지하여 포트폴리오 상태에 따라 `WELL_BALANCED(60% 미만)`, `CONCENTRATED(60%~80%)`, `HIGH_RISK(80% 이상 또는 성향 불일치)` 단계로 안전성 코드를 자동 분류합니다.
  - **사용자 질의 맞춤 피드백**: 유저 추가 요청 사항(`userComment`) 유무에 따른 동적 분기 로직 설계 및 대체 자산군 거시경제 관점 제안
  - **Structured Outputs**: OpenAI API 요청 시 Strict JSON Schema를 강제하여 환각 현상(Hallucination) 없는 정형 데이터 응답 보장
  - **AiRequestGuard**: 유저별 AI 분석 호출 건수를 통제하는 인터셉터 계층을 적용하여 인프라 비용 폭주 원천 차단
</details>

### 포트폴리오 추가 적합 여부 분석
<details>
  <summary>자세히 보기</summary>

  - **다각화 및 상관관계 시뮬레이션**: 관심 종목 편입 시 기존 포트폴리오(`currentHoldings`)의 분산 효과와 기대 성과(수익성/성장/현금흐름)에 미치는 영향력을 정밀 시뮬레이션합니다.
  - **자산 구성별 유연한 전개**: 보유 종목 수(단일 자산 보유 여부 등)와 투자 성향 지표를 분석의 전제로 바인딩하여 맞춤형 자산 결합 리포트의 전개 방식을 다르게 제어합니다.
  - **시너지 판단 결론 도출**: 대상 기업의 비즈니스 퍼포먼스와 자산 데이터의 득실을 종합적으로 평가하여 `보완(COMPLEMENTARY)`, `중립(NEUTRAL)`, `부담(BURDEN)` 중 최종 적합성 결론 코드를 산출합니다.
  - **Structured Outputs**: OpenAI API 요청 시 Strict JSON Schema를 강제하여 환각 현상(Hallucination) 없는 정형 데이터 응답 보장
  - **AiRequestGuard**: 유저별 AI 분석 호출 건수에 제한을 두는 가드 계층을 적용하여 무분별한 API 오남용 및 인프라 비용 폭주 원천 차단
</details>

### 재무제표 분석
<details>
  <summary>자세히 보기</summary>

  - **토큰 비용 최적화**: 기업 재무 분석 시 유저 정보와 포트폴리오 데이터를 배제한 경량 패키지(`financialPackage`)를 재구성하여 외부 API 호출 토큰 소비를 최소화
  - **데이터 기반 재무 진단**: 금융 API로 수집한 매출액, 영업이익률, 부채비율, ROE, FCF(잉여현금흐름) 등 실제 재무 수치만을 근거로 분석하며 자본잠식이나 과도한 부채 등 치명적 리스크를 최우선 강조
  - **금융 전문 용어 한글 병기**: PER(주가수익비율), ROE(자기자본이익률) 등 영문 약어 언급 시 한글 설명을 필수 병기하여 가독성을 높이고 종합 상태에 따라 최종 건전성 코드(`COMPLEMENTARY`, `NEUTRAL`, `BURDEN`)를 부여
  - **Structured Outputs**: OpenAI API 요청 시 Strict JSON Schema를 강제하여 환각 현상(Hallucination) 없는 정형 데이터 응답 보장
  - **AiRequestGuard**: 유저별 AI 분석 호출 건수에 제한을 두는 가드 계층을 적용하여 무분별한 API 오남용 및 인프라 비용 폭주 원천 차단
</details>

---

### 구현화면

---



## 아키텍처
- **Spring Boot + MySQL + JPA(QueryDSL)** 기반 백엔드  
- **React** 기반 프론트엔드  
- **OAuth2 + JWT** 쿠키 기반 인증/인가 (Access Token + Refresh Token)
- **OpenAI API + Structured Outputs (Strict Schema)** 기반 AI 정형 데이터 제어
- **AiRequestGuard** 인터셉터 계층을 통한 AI 호출 트래픽 및 비용 통제
- **Redis (Cache-Aside)** 전략 기반 외부 금융 API 데이터 캐시 최적화
- **국내·외 금융 API 및 환율 연동**: 한국은행(BOK), DART, Alpha Vantage 연동 데이터 통합
- **Docker + Docker Compose** 로 배포 환경 구성  
- **Nginx** 를 통한 Reverse Proxy

```mermaid
graph TD
    %% 클라이언트 레이어
    Client[Client Browser / React] -->|HTTPS / HttpOnly Cookies| Nginx[Reverse Proxy / Nginx]

    %% 인프라 프록시 및 WAS 레이어
    Nginx --> WAS[Core WAS / Spring Boot 3.5]

    %% 내부 컴포넌트 격리 구역
    subgraph SpringBoot_Internal [Spring Boot Core 인프라]
        WAS --> Security[Spring Security / OAuth2]
        WAS --> Interceptor[AiRequestGuard Interceptor]
        WAS --> CacheLayer[Spring Cache Layer]
        WAS --> JPALayer[Spring Data JPA / QueryDSL]
    end

    %% 보안 및 저장소 레이어 매핑
    Security -->|Cookie Extraction| InMem[OncePerRequestFilter 인증 계층]
    CacheLayer -->|Cache-Aside 전략| Redis[(Redis Memory Cache)]
    JPALayer -->|ORM Mapping| MySQL[(MySQL 8.0 Main RDB)]

    %% 외부 금융 및 AI 파이프라인 데이터 게이트웨이
    WAS -->|Structured Outputs| OpenAI[OpenAI GPT-4o API]
    WAS -->|Master Data & 환율 연동| KIS[한국투자증권 Open API 망]
    WAS -->|국내 재무 수치| DART[국가전자공시시스템 DART]
    WAS -->|해외 마스터 프로필| AlphaVantage[Alpha Vantage API]

    %% 스타일 정의
    style Client fill:#2088FF,stroke:#fff,stroke-width:2px,color:#fff
    style Nginx fill:#009639,stroke:#fff,stroke-width:2px,color:#fff
    style WAS fill:#6DB33F,stroke:#fff,stroke-width:2px,color:#fff
    style Redis fill:#DC382D,stroke:#fff,stroke-width:2px,color:#fff
    style MySQL fill:#4479A1,stroke:#fff,stroke-width:2px,color:#fff
    style OpenAI fill:#412991,stroke:#fff,stroke-width:2px,color:#fff
    style KIS fill:#ff9900,stroke:#fff,stroke-width:2px,color:#fff

---

## 기술 스택 상세

### Frontend
- **React / TypeScript**: 컴포넌트 기반 UI 아키텍처 및 상태 제어
- **TanStack Router & Query**: 캐시 제어 및 주식 상세 페이지 무효화(`invalidateQueries`) 시스템 탑재
- **Tailwind CSS & Shadcn UI**: 반응형 레이아웃 및 디자인 시스템 인프라 구축

### Backend
- **Java 17 / Spring Boot 3.5**
- **Spring Data JPA / QueryDSL**: ORM 및 복합 인덱스 매칭 동적 쿼리 최적화
- **Spring Security / OAuth2 Client**: 다중 소셜 로그인 인증 처리  
- **JJWT (0.12.6)**: 이중 JWT(Access / Refresh Token) 인증/인가
- **Spring Data Redis / Spring Cache**: 외부 금융 API 호출 최적화 및 캐싱
- **MySQL 8.0**: 메인 관계형 데이터베이스(RDB)

### External APIs
- **한국투자증권 (KIS API)**: 국내/해외 주식 마스터 데이터 온디맨드 실시간 수집 연동, 환율 수
- **OpenAI API**: 데이터 기반 자산 진단 및 정형 AI 분석 리포트 생성
- **국가전자공시시스템 DART API**: 국내 주식 고유번호 파싱 및 기업 재무 데이터 수집
- **Alpha Vantage API**: 글로벌(해외) 주식 기업 개요 및 다차원 재무 수치 연동
  
### Infra & DevOps
- **Oracle Cloud (OCI)**: 클라우드 컴퓨팅 인스턴스 서버 및 데이터베이스 배포
- **Docker & Docker Compose**: 가상 환경 인프라 컨테이너 기반 배포
- **Nginx**: Reverse Proxy 구동 및 정적 리소스 웹 서버 가단성 확보
- **GitHub Actions (CI/CD)**: 코드 푸시 시 자동 빌드, 패키징 및 운영 서버 무중단 배포 파이프라인 구축

  
---

## 프로젝트 실행 방법

---

## 데이터베이스 설계도 (ERD)
