# Multi-stage build for the QE Harness reference SUT (Task 14).
#
# Stage 1 compiles the Spring Boot fat jar with Maven against a full JDK.
# Stage 2 runs it on a slim JRE-only Alpine base -- no build tooling, no
# Maven, no source, in the image that actually ships. Tests are skipped
# here (-DskipTests): this SUT's own test suite (Tasks 6-13) already runs
# outside Docker via `mvn test` and needs a live Testcontainers Docker
# socket that a `docker build` sandbox does not reliably provide (see
# qe-harness/README.md's Testcontainers section) -- re-running it as part
# of the image build would make the build itself flaky for a reason
# unrelated to the image's own correctness.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Dependency layer cached separately from source so an application-code-only
# change doesn't force a full re-download of the Maven Central tree above.
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

COPY src ./src
RUN mvn -q -B -DskipTests package

# ---- runtime image ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Runs as a non-root user; alpine's busybox addgroup/adduser cover this
# without pulling in a separate base image.
RUN addgroup -S sut && adduser -S sut -G sut
COPY --from=build /build/target/reference-sut-*.jar /app/app.jar
USER sut

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
