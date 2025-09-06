# ---- Build Stage ----
FROM gradle:8.9-jdk17-alpine AS build
WORKDIR /app

# Copy everything and build the fat jar
COPY . .
RUN gradle buildFatJar --no-daemon

# ---- Run Stage ----
FROM eclipse-temurin:17-jre-jammy AS run
WORKDIR /app

# Copy only the fat jar from the build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Expose Ktor's default port
EXPOSE 5000

# Run the application
CMD ["java", "-jar", "app.jar"]
