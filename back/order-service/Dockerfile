FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Копируем весь проект в контейнер
COPY . .

# Собираем только order-service и его зависимости
RUN mvn clean package -DskipTests -pl back/order-service -am

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/back/order-service/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
