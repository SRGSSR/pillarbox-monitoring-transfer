# First stage: Build native image with GraalVM
FROM eclipse-temurin:24-jdk-alpine AS build

WORKDIR /app

COPY gradlew .
COPY gradle/ gradle/
COPY gradle.properties .
COPY build.gradle.kts .
COPY src ./src

RUN ./gradlew clean build -x test -x check

# Second stage: Slim runtime image with optimized settings
FROM eclipse-temurin:24-jre-alpine
VOLUME /tmp

# Copy the built JAR file from the build stage
COPY --from=build /app/build/libs/app.jar app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
