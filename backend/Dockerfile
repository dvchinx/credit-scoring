# Build con el Maven wrapper del repo; las dependencias se descargan en una capa propia (caché).
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN sh mvnw -q -B dependency:go-offline
COPY src src
RUN sh mvnw -q -B package -DskipTests

# Runtime: solo el JRE y el jar, con un usuario sin privilegios.
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 1001 app
COPY --from=build /app/target/backend-*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
