FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY back/file-service/target/file-service-0.0.1-SNAPSHOT-exec.jar app.jar
EXPOSE 8087
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
