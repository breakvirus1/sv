FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY back/material-service/target/material-service-0.0.1-SNAPSHOT-exec.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "/app/app.jar"]