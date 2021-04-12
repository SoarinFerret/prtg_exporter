FROM maven:3.6.3-jdk-8
ADD . /
RUN mvn package -DskipTests


FROM openjdk:8-jdk-slim
WORKDIR /prtg/
COPY --from=0 /prtg-exporter-app/target/*.jar app.jar
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","./app.jar"]