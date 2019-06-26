#!/bin/bash

docker run -p 5432:5432 -e POSTGRES_DB=meuse -e POSTGRES_USER=meuse -e POSTGRES_PASSWORD=meuse -v $(pwd)/dev/resources/sql/schema.sql:/docker-entrypoint-initdb.d/schema.sql postgres:11.4
