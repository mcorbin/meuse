FROM clojure:temurin-17-lein-focal as build-env

ADD . /app
WORKDIR /app

RUN lein uberjar

# -----------------------------------------------------------------------------

from eclipse-temurin:17-focal

RUN groupadd -r meuse && useradd -r -s /bin/false -g meuse meuse
RUN mkdir /app
COPY --from=build-env /app/target/*uberjar/meuse-*-standalone.jar /app/meuse.jar

RUN chown -R meuse:meuse /app

RUN apt-get update && apt-get -y upgrade && apt-get install -y git
user meuse

ENTRYPOINT ["java"]

CMD ["-jar", "/app/meuse.jar"]
