FROM ghcr.io/navikt/baseimages/temurin:21
COPY build/libs/*-all.jar app.jar
#ENV JAVA_OPTS='-Dlogback.configurationFile=logback.xml'
ENV JDK_JAVA_OPTIONS='-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp'
ENV JAVA_OPTS='-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp -Xms512m -Xmx2048m'
ENV TZ="Europe/Oslo"
EXPOSE 8080