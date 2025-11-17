# First stage: Build native image with GraalVM
FROM eclipse-temurin:23-jdk-alpine AS build

WORKDIR /app

COPY gradlew .
COPY gradle/ gradle/
COPY gradle.properties .
COPY build.gradle.kts .
COPY src ./src

RUN ./gradlew clean build -x test -x check

# Second stage: Slim runtime image with optimized settings
FROM eclipse-temurin:23-jre-alpine
VOLUME /tmp

# Copy the built JAR file from the build stage
COPY --from=build /app/build/libs/app.jar app.jar

# Extract layered JAR content for better image caching and performance
RUN java -Djarmode=layertools -jar app.jar extract

# JVM memory and garbage collection optimizations for containers
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dsun.net.inetaddr.ttl=60 -Dsun.net.inetaddr.negative.ttl=10 -XX:+UseContainerSupport --add-opens java.base/java.math=ALL-UNNAMED -jar /app.jar"]
