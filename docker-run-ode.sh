#!/bin/bash
docker run -it -v /var/run/docker.sock:/var/run/docker.sock -p '8080:8080' -e DOCKER_HOST_IP=$DOCKER_HOST_IP jpoode_ode:latest $1