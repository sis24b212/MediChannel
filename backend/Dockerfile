# Multi-stage Dockerfile for deploying MediChannel Spring Boot API on Render
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY settings.xml /root/.m2/settings.xml
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
ENV TZ=Asia/Colombo
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Duser.timezone=Asia/Colombo", "-jar", "app.jar"]
