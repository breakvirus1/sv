FROM debian:bookworm-slim
RUN apt-get update && apt-get install -y openjdk-17-jre-headless && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY target/order-service-0.0.1-SNAPSHOT-exec.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
