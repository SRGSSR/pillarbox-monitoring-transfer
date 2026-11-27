# First stage: Build native image with GraalVM
FROM eclipse-temurin:27-jdk-alpine@sha256:3ce6707ba035480069ff65d12265a8806d74fe9fd761987d183dd86362def451 AS build

WORKDIR /app

COPY gradlew .
COPY gradle/ gradle/
COPY gradle.properties .
COPY build.gradle.kts .
COPY src ./src

RUN ./gradlew clean build -x test -x check

# Second stage: Slim runtime image with optimized settings
FROM eclipse-temurin:27-jre-alpine@sha256:51f686a5322c0b762dff27e8e94b4b6582d7e71321d3a23b1f3c53b4078aabc5
RUN addgroup -S -g 10001 pillarbox \
 && adduser -S -D -H -u 10001 -G pillarbox -h /nonexistent -s /sbin/nologin pillarbox
VOLUME /tmp

# Copy the built JAR file from the build stage
COPY --from=build /app/build/libs/app.jar app.jar

USER 10001:10001
EXPOSE 8081
# The health endpoint answers 200 while events arrive and 503 after a period without events.
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=3 \
  CMD ["wget", "-q", "-O", "/dev/null", "http://127.0.0.1:8081/health"]

ENTRYPOINT ["sh", "-c", "java -Dsun.net.inetaddr.ttl=5 -Dsun.net.inetaddr.negative.ttl=10 -XX:+UseContainerSupport -XX:-UseBASE64Intrinsics --add-opens java.base/java.math=ALL-UNNAMED -jar /app.jar"]
