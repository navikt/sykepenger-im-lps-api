FROM busybox:latest
ENV PORT=8080

ADD ./www/index.html /www/index.html
ADD ./www/hello-nais.png /www/hello-nais.png

HEALTHCHECK CMD nc -z localhost $PORT

# Create a basic webserver and run it until the container is stopped
CMD echo "httpd started" && trap "exit 0;" TERM INT; httpd -v -p $PORT -h /www -f & wait