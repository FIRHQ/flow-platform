#!/usr/bin/env bash

# Pull and run required services via docker

# pull mysql docker image
docker pull mysql:5.6

# pull rabbitmq 3.6.10 docker image
docker pull rabbitmq:3

docker run -d -e MYSQL_ALLOW_EMPTY_PASSWORD=yes -p 3306:3306 mysql:5.6
docker run -d --hostname flow-rabbit -p 5672:5672 rabbitmq