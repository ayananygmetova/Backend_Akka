FROM java:8-jre-alpine
WORKDIR /opt/docker
ADD --chown=daemon:daemon opt /opt
EXPOSE 8000
USER daemon
ENTRYPOINT ["/opt/docker/bin/cluster"]
CMD []
