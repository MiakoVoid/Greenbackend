FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app

COPY target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT java -XX:+UseContainerSupport -XX:+UseG1GC -XX:MaxRAMPercentage=70.0 -XX:InitialRAMPercentage=50.0 -XX:MaxGCPauseMillis=200 -XX:+HeapDumpOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom -jar app.jar