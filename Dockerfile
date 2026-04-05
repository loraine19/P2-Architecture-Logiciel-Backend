FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests


FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# Copie le rapport JaCoCo généré en local (./mvnw verify avant docker build)
COPY target/site/jacoco/ ./target/site/jacoco/
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]