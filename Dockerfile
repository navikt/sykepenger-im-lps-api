FROM gcr.io/distroless/java21
COPY build/libs/*-all.jar app.jar
ENV JAVA_OPTS='-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp'
ENV LANG='nb_NO.UTF-8' LANGUAGE='nb_NO:nb' LC_ALL='nb:NO.UTF-8' TZ="Europe/Oslo"
ENTRYPOINT ["java", "-jar", "/app.jar"]
EXPOSE 8080
