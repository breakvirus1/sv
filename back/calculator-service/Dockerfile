FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY back/calculator-service/target/calculator-service-0.0.1-SNAPSHOT-exec.jar app.jar
EXPOSE 8086
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
