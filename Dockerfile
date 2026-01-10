# 실행 스테이지 (빌드 스테이지 제거)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 한국 시간(KST) 설정
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Seoul /etc/localtime && \
    echo "Asia/Seoul" > /etc/timezone && \
    apk del tzdata

# 업로드 디렉토리 생성
RUN mkdir -p /app/uploads/songs

# GitHub Actions에서 빌드된 WAR 파일을 복사
COPY target/*.war app.war

# 메모리 제한 설정
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.war"]
