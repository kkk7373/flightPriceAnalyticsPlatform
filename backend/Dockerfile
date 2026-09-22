FROM eclipse-temurin:21-jdk AS development

WORKDIR /workspace/backend

CMD ["/bin/bash"]

FROM development AS build

WORKDIR /workspace/backend/backend

COPY backend/.mvn/ .mvn/
COPY backend/mvnw backend/pom.xml ./
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline

COPY backend/src/ src/
RUN ./mvnw -B clean package -DskipTests

FROM eclipse-temurin:21-jre AS runtime

WORKDIR /app

COPY --from=build --chown=10001:10001 /workspace/backend/backend/target/*.jar app.jar

USER 10001:10001

ENTRYPOINT ["java", "-jar", "app.jar"]
