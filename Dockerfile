from openjdk:11

ARG MEUSE_VERSION=0.2.0

RUN groupadd -r meuse && useradd -r -s /bin/false -g meuse meuse
RUN mkdir /app
COPY target/uberjar/meuse-${MEUSE_VERSION}-standalone.jar /app/meuse.jar

RUN chown -R meuse:meuse /app

user meuse

ENTRYPOINT ["java"]

CMD ["-jar", "/app/meuse.jar"]
