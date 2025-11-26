FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY build/libs/simulator-0.0.1-SNAPSHOT.jar simulator.jar
ENTRYPOINT ["java", "-jar", "simulator.jar"]