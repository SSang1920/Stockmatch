# 1) 빌드 스테이지 (Node로 React 빌드)
FROM node:20-alpine AS build
WORKDIR /app

# 패키지 먼저 복사 (캐시)
COPY package*.json ./
RUN npm ci --no-audit --progress=false || npm install

# 소스 복사 후 빌드
COPY . .
RUN npm run build

# 2) 서빙 스테이지 (Nginx)
FROM nginx:1.27-alpine

# 빌드 산출물 복사
COPY --from=build /app/dist /usr/share/nginx/html

# 포트 노출
EXPOSE 80