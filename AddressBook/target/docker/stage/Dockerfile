FROM openjdk:8-jre-alpine
WORKDIR /opt/docker
ADD --chown=daemon:daemon opt /opt
USER daemon
ENTRYPOINT ["/opt/docker/bin/address-book"]
CMD ["/opt/docker/bin/address-book"]
