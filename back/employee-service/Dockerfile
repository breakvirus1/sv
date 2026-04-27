FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests -pl back/employee-service -am

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/back/employee-service/target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]