#!/bin/bash

docker run -p 5432:5432 -e POSTGRES_DB=meuse -e POSTGRES_USER=meuse \
       -e POSTGRES_PASSWORD=meuse \
       -v $(pwd)/dev/resources/sql/schema.sql:/docker-entrypoint-initdb.d/schema.sql \
       postgres:11.4

# to test ssl:

#       -v /home/mathieu/Documents/meuse/ssl/server.cer:/var/lib/postgresql/server.crt \
#       -v /home/mathieu/Documents/meuse/ssl/server.key:/var/lib/postgresql/server.key:Z \
#       postgres:11.4 \
#       -c ssl=on \
#       -c ssl_cert_file=/var/lib/postgresql/server.crt \
#       -c ssl_key_file=/var/lib/postgresql/server.key

