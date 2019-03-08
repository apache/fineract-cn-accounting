FROM openjdk:8-jdk-alpine

ARG accounting_port=2025

ENV server.max-http-header-size=16384 \
    cassandra.clusterName="Test Cluster" \
    server.port=$accounting_port

WORKDIR /tmp
COPY accounting-service-boot-0.1.0-BUILD-SNAPSHOT.jar .

CMD ["java", "-jar", "accounting-service-boot-0.1.0-BUILD-SNAPSHOT.jar"]
