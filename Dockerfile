FROM eclipse-temurin:21-jdk AS build

WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY src src

RUN --mount=type=cache,target=/root/.m2 \
    sh ./mvnw -B -DskipTests package


FROM eclipse-temurin:21-jre

WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --no-create-home \
       --shell /usr/sbin/nologin spazacred

COPY --from=build /workspace/target/*.jar app.jar

USER spazacred

EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-Duser.timezone=Africa/Johannesburg", "-jar", "app.jar"]
