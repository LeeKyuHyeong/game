# Build stage
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:17-jre
WORKDIR /app

# Create uploads directory
RUN mkdir -p /app/uploads/songs

# Copy the built artifact
COPY --from=build /app/target/*.war app.war

# Expose port
EXPOSE 8082

# Run the application
ENTRYPOINT ["java", "-jar", "app.war"]
