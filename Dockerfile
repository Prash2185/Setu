# Stage 1: Build the Application
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
# Bina test run kiye direct .jar file banayenge
RUN mvn clean package -DskipTests 

# Stage 2: Run the Application (Lightweight Alpine Image)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# Build stage se .jar file uthao
COPY --from=build /app/target/cloudbilling-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
# App start karne ka command
ENTRYPOINT ["java","-jar","app.jar"]