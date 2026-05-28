# 1) 빌드 스테이지 (Node.js 환경)
FROM node:20-alpine AS builder
WORKDIR /app

# 캐시 최적화를 위해 패키지 파일 먼저 복사 후 설치
COPY package*.json ./
RUN npm ci

# 전체 소스 복사 후 빌드 진행
COPY . .

ARG VITE_API_BASE_URL
ARG VITE_KAKAO_CLIENT_ID
ARG VITE_NAVER_CLIENT_ID
ARG VITE_GOOGLE_CLIENT_ID

RUN npm run build

# 2) 런타임 스테이지 (Nginx 환경)
FROM nginx:alpine
ENV TZ=Asia/Seoul

# React 빌드 결과물(dist 또는 build)을 Nginx 웹 루트 폴더로 복사
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80

# Nginx 실행
CMD ["nginx", "-g", "daemon off;"]