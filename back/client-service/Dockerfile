FROM eclipse-temurin:17-jre-alpine
# postgresql-client нужен для pg_dump (бэкап баз данных компаний)
RUN apk add --no-cache postgresql-client
WORKDIR /app
COPY back/client-service/target/client-service-0.0.1-SNAPSHOT-exec.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
