FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests -pl back/api-gateway -am

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/back/api-gateway/target/*.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]
