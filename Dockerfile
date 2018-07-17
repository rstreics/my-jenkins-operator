FROM openjdk:9-jdk-slim as jenkins-operator-cache
WORKDIR /workspace
COPY . /workspace
RUN ./gradlew downloadJars

FROM openjdk:9-jdk-slim as jdk
WORKDIR /workspace
COPY --from=jenkins-operator-cache /root/.gradle /root/.gradle
COPY . /workspace
RUN ./gradlew clean compileGroovy assembl -x test

FROM openjdk:9-jre-slim
COPY --from=jdk /workspace/build/libs/jenkins-operator.jar /jenkins-operator.jar
ENTRYPOINT java -jar /jenkins-operator.jar
