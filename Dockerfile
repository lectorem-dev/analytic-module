FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY build/libs/analytic-0.0.1-SNAPSHOT.jar AnalyticsService.jar
ENTRYPOINT ["java", "-jar", "AnalyticsService.jar"]