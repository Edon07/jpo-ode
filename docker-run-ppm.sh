#!/bin/bash
docker run -it -v ${DOCKER_SHARED_VOLUME}:/ppm_data -e DOCKER_HOST_IP=${DOCKER_HOST_IP} jpoode_ppm:latest $1
