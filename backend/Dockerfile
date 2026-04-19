FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /build

COPY backend/pom.xml .
RUN mvn dependency:go-offline -B -q

COPY backend/src ./src
RUN mvn package -DskipTests -B -q

FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

RUN addgroup -S pitstop && adduser -S pitstop -G pitstop

COPY --from=build /build/target/*.jar app.jar

RUN chown pitstop:pitstop app.jar
USER pitstop

EXPOSE 8080

ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
