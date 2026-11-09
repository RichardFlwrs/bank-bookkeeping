FROM eclipse-temurin:17-jdk AS build
WORKDIR /src
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline
COPY src src
RUN ./mvnw -B -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --uid 10001 app \
    && mkdir -p /data \
    && chown app:app /data
COPY --from=build /src/target/bank-0.0.1-SNAPSHOT.jar app.jar
USER app
EXPOSE 8086
ENV SPRING_DATASOURCE_URL=jdbc:sqlite:/data/bank_dev.db
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
