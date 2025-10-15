# 1) 빌드 스테이지
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app

# gradle wrapper 권한 + 캐시 최적화
COPY gradlew ./
COPY gradle ./gradle
RUN chmod +x gradlew

# 의존성 캐시를 위해 설정 파일 먼저 복사
COPY build.gradle settings.gradle ./
RUN ./gradlew dependencies --no-daemon || true

# 소스 복사 후 빌드
COPY src ./src
RUN ./gradlew clean bootJar -x test --no-daemon

# 2) 런타임 스테이지
FROM eclipse-temurin:17-jre
ENV TZ=Asia/Seoul
WORKDIR /app

# Non-root 유저 생성
RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser
USER appuser

# 빌드 결과물 복사
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080

# 실행
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-XX:+UseG1GC", "-jar", "app.jar"]