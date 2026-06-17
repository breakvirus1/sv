FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY back/generate-data-service/target/generate-data-service-0.0.1-SNAPSHOT-exec.jar app.jar
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
