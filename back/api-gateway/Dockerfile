FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY back/api-gateway/target/api-gateway-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
