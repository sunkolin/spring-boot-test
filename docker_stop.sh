#!/bin/bash

export APP_NAME=spring-boot-test

echo "docker stop running."

sudo docker stop ${APP_NAME}

sudo docker rm ${APP_NAME}

echo "docker stop complete."
