# Meuse

A Rust registry written in Clojure.

WIP, more infos soon !

## Run PG

docker run -p 5432:5432 -e POSTGRES_DB=meuse -e POSTGRES_USER=meuse -e POSTGRES_PASSWORD=meuse postgres

psql -h localhost -p 5432 -U meuse
