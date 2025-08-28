FROM gcr.io/distroless/java21
COPY build/install/*/lib /lib
ENV JAVA_OPTS='-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp'
ENV LANG='nb_NO.UTF-8' LANGUAGE='nb_NO:nb' LC_ALL='nb:NO.UTF-8' TZ="Europe/Oslo"
ENTRYPOINT ["java", "-cp", "/lib/*", "no.nav.helsearbeidsgiver.ApplicationKt"]
EXPOSE 8080
