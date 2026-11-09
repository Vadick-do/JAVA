FROM maven:3.10.0-eclipse-temurin-21-noble AS build
WORKDIR /app
COPY pom.xml ./
COPY .mvn/ .mvn/
COPY src/ src/
RUN mvn -B -ntp verify

FROM eclipse-temurin:21.0.13_9-jre-noble AS runtime
WORKDIR /app
COPY --from=build /app/target/booking-backend-java-*.jar /app/booking.jar
COPY --from=build /app/target/dependency/ /app/dependency/
ENTRYPOINT ["java", "-jar", "/app/booking.jar"]
