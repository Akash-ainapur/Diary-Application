FROM maven:3-eclipse-temurin-17-alpine AS builder
WORKDIR /app

COPY pom.xml .
COPY mvnw ./
COPY mvnw.cmd ./
COPY src ./src

# Fix line endings for the wrapper and prefer system mvn if wrapper fails
RUN if [ -f "./mvnw" ]; then sed -i 's/\r$//' mvnw && chmod +x mvnw; fi
RUN mvn package -DskipTests || ./mvnw package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]