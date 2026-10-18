FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-21@sha256:7426f17726e842c26d462908a780c655625a69d95b2b19642209c195f17d44b0

WORKDIR /app

ENV LANG='nb_NO.UTF-8' LANGUAGE='nb_NO:nb' LC_ALL='nb:NO.UTF-8' TZ="Europe/Oslo"

ARG JAR_FILE=target/*.jar
COPY --chown=65532:65532 --chmod=0444 ${JAR_FILE} app.jar

USER 65532:65532

ENTRYPOINT ["java", "-jar", "app.jar"]

