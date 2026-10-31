FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-21@sha256:f3e4694f62a8d085a42d7347a6ff96e776363d7f5cb49fa36f4d1cfb90b3a5f1

WORKDIR /app

ENV LANG='nb_NO.UTF-8' LANGUAGE='nb_NO:nb' LC_ALL='nb:NO.UTF-8' TZ="Europe/Oslo"

ARG JAR_FILE=target/*.jar
COPY --chown=65532:65532 --chmod=0444 ${JAR_FILE} app.jar

USER 65532:65532

ENTRYPOINT ["java", "-jar", "app.jar"]

