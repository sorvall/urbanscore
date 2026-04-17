# syntax=docker/dockerfile:1

FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

RUN apk add --no-cache maven

COPY pom.xml ./
RUN mkdir -p src
COPY src ./src

RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jdk-alpine AS runtime
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
USER spring:spring

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
