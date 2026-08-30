FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY back/statistic-service/target/statistic-service-0.0.1-SNAPSHOT-exec.jar app.jar

EXPOSE 8089

ENTRYPOINT ["java", "-jar", "app.jar"]
