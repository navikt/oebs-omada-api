FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-21@sha256:9f0ba29ffe3427087f7c151de7388ef86796f76eba337574f43b0398679221c1

WORKDIR /app

ENV LANG='nb_NO.UTF-8' LANGUAGE='nb_NO:nb' LC_ALL='nb:NO.UTF-8' TZ="Europe/Oslo"

ARG JAR_FILE=target/*.jar
COPY --chown=65532:65532 --chmod=0444 ${JAR_FILE} app.jar

USER 65532:65532

ENTRYPOINT ["java", "-jar", "app.jar"]

