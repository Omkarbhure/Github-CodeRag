# Stage 1: Build JAR using Maven and Temurin JDK 17
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder
WORKDIR /build

# Copy project definition and source
COPY pom.xml .
COPY src ./src

# Package application without executing tests in container build
RUN mvn clean package -DskipTests -B

# Stage 2: Minimal runtime image
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Install curl for health check probing
RUN apk add --no-cache curl

# Run as non-root user for security
RUN addgroup -S coderag && adduser -S coderag -G coderag

# Create directory for on-disk repository extracts
RUN mkdir -p /app/data/repos && chown -R coderag:coderag /app

# Copy executable jar from builder stage
COPY --from=builder --chown=coderag:coderag /build/target/coderag-*.jar app.jar

USER coderag

EXPOSE 8081
ENV PORT=8081 \
    REPO_STORAGE_PATH=/app/data/repos

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:${PORT}/api/health || exit 1

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
