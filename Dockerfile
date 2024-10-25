FROM ghcr.io/navikt/baseimages/temurin:21
COPY build/libs/*-all.jar app.jar
ENV JAVA_OPTS='-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp'
ENV TZ="Europe/Oslo"
EXPOSE 8080