# Empaqueta el artefacto producido por ./mvnw verify o scripts/verify-java21.sh.
FROM eclipse-temurin:21-jdk
RUN groupadd --system app && useradd --system --gid app --home-dir /app app
WORKDIR /app
COPY --chown=app:app target/backend-0.1.0-SNAPSHOT.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java","-XX:MaxRAMPercentage=75","-jar","/app/app.jar"]
