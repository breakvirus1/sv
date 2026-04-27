FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests -pl back/discovery-server -am

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/back/discovery-server/target/*.jar app.jar
EXPOSE 8761
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
