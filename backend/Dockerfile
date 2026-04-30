FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

RUN apk add --no-cache sqlite-libs

COPY target/*.jar app.jar

RUN mkdir -p /data/storage /data/logs

EXPOSE 8080

ENV APP_PORT=8080
ENV APP_HOST=0.0.0.0
ENV STORAGE_PATH=/data/storage
ENV DB_PATH=/data/db/material.db
ENV LOG_PATH=/data/logs/application.log
ENV LOG_MAX_SIZE=100MB
ENV LOG_MAX_HISTORY=30
ENV MAX_FILE_SIZE=500MB

ENTRYPOINT ["sh", "-c", "java -jar app.jar \
  --server.port=${APP_PORT} \
  --server.address=${APP_HOST} \
  --spring.datasource.url=jdbc:sqlite:${DB_PATH} \
  --storage.local.base-path=${STORAGE_PATH} \
  --logging.file.name=${LOG_PATH} \
  --logging.file.max-size=${LOG_MAX_SIZE} \
  --logging.file.max-history=${LOG_MAX_HISTORY} \
  --spring.servlet.multipart.max-file-size=${MAX_FILE_SIZE} \
  --spring.servlet.multipart.max-request-size=${MAX_FILE_SIZE}"]
