# syntax=docker/dockerfile:1
#
# Independent build: only `core` + this service are copied and compiled here —
# no other service's source or Dockerfile is ever read. Two separate `mvn`
# invocations, not `-pl user-service -am` from the root: that flag still
# makes Maven parse the root pom's full <modules> list up front, which would
# fail here since the other 5 service folders were never copied in. Building
# `core` and `user-service` as two standalone poms avoids that entirely —
# core's own parent is spring-boot-starter-parent (external), not the root
# aggregator, so it needs nothing else on disk.
#
# Build context must still be the repo root, since `core` lives one level up
# from this file (see docker-compose.yml: context: ., dockerfile:
# user-service/Dockerfile).

FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace

COPY pom.xml mvnw ./
COPY .mvn .mvn
COPY core core
COPY user-service user-service
RUN chmod +x mvnw

RUN ./mvnw -f core/pom.xml clean install -DskipTests -B
RUN ./mvnw -f user-service/pom.xml clean package -DskipTests -B

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/user-service/target/user-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8086
ENTRYPOINT ["java", "-jar", "app.jar"]
