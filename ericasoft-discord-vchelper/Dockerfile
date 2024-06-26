FROM registry.hub.docker.com/library/eclipse-temurin:22-jre
ARG app_jar
RUN apt-get --assume-yes update --fix-missing && \
    apt-get --assume-yes upgrade --fix-missing && \
    apt-get --assume-yes install curl jq && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/* /var/cache/apt/archives/*

ARG USER=docker
ARG APP_HOME=/srv

RUN adduser --home ${APP_HOME} --shell /bin/bash --disabled-password --gecos "" --uid 1001 ${USER}

COPY /target/${app_jar} ${APP_HOME}/application.jar
COPY /target/classes/entrypoint.sh ${APP_HOME}/entrypoint.sh
COPY /target/classes/healthcheck.sh ${APP_HOME}/healthcheck.sh

RUN chmod 555 ${APP_HOME}/entrypoint.sh
RUN chmod 555 ${APP_HOME}/healthcheck.sh
RUN chown -R ${USER}:${USER} ${APP_HOME}

USER ${USER}
WORKDIR ${APP_HOME}
EXPOSE 42069

HEALTHCHECK --interval=10s --timeout=2s --retries=5 --start-period=30s \
  CMD sh ./healthcheck.sh

ENTRYPOINT ["/srv/entrypoint.sh", "application.jar"]