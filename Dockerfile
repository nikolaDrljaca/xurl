# ---- Build Stage ----
FROM gradle:8.9-jdk17-alpine AS build
WORKDIR /app

# Copy everything and build the fat jar
COPY . .
RUN gradle shadowJar --no-daemon

# ---- Run Stage ----
FROM amazoncorretto:17-alpine AS run
WORKDIR /app

# Copy only the fat jar from the build stage
COPY --from=build /app/build/libs/*-all.jar app.jar

# Expose Ktor's default port
EXPOSE 5000

# Run the application
CMD ["java", "-jar", "app.jar"]
