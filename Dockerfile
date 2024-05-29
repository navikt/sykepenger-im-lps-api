FROM ghcr.io/navikt/baseimages/temurin:21
COPY build/libs/*-all.jar app.jar
#ENV JAVA_OPTS='-Dlogback.configurationFile=logback.xml'
ENV TZ="Europe/Oslo"
EXPOSE 8080
