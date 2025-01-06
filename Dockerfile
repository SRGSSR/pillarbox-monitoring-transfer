# First stage: Build native image with GraalVM
FROM gradle:8.10-jdk22-alpine AS build

WORKDIR /app
COPY gradle.properties .
COPY build.gradle.kts .
COPY src ./src
RUN gradle clean build -x test -x check

# Second stage: Slim runtime image with optimized settings
FROM eclipse-temurin:22-jre-alpine
VOLUME /tmp

# Copy the built JAR file from the build stage
COPY --from=build /app/build/libs/app.jar app.jar

# Extract layered JAR content for better image caching and performance
RUN java -Djarmode=layertools -jar app.jar extract

# JVM memory and garbage collection optimizations for containers
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dsun.net.inetaddr.ttl=60 -Dsun.net.inetaddr.negative.ttl=10 -XX:+UseContainerSupport --add-opens java.base/java.math=ALL-UNNAMED -jar /app.jar"]
