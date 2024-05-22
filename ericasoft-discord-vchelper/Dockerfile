ARG app_jar
FROM registry.hub.docker.com/library/eclipse-temurin:22-jre

RUN apt-get --assume-yes update --fix-missing && \
    apt-get --assume-yes upgrade --fix-missing && \
    apt-get --assume-yes install curl && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/* /var/cache/apt/archives/*

ARG USER=docker
ARG APP_HOME=/srv

RUN adduser --home ${APP_HOME} --shell /bin/bash --disabled-password --gecos "" --uid 1001 ${USER}

COPY /target/${app_jar} ${APP_HOME}/${app_jar}
COPY /target/classes/entrypoint.sh ${APP_HOME}/entrypoint.sh

RUN chmod 555 ${APP_HOME}/entrypoint.sh
RUN chown -R ${USER}:${USER} ${APP_HOME}

USER ${USER}
WORKDIR ${APP_HOME}
EXPOSE 42069

ENTRYPOINT ["./entrypoint.sh","${app_jar}"]