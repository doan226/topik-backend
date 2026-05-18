# Bước 1: Sử dụng Maven để build dự án thành file .jar
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Bước 2: Sử dụng Eclipse Temurin JDK 17 (hoặc 21) siêu nhẹ để chạy file .jar
FROM eclipse-temurin:17-jdk-slim
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]