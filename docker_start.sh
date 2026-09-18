#!/bin/bash

# 环境变量
export APP_NAME=spring-boot-test
export APP_PORT=6005
export APP_VERSION=1.0.0
export APP_ENV=dev

# 打包
# 如果是dev环境，先打包
# 如果是prod环境，先从远程拉取包
if [ APP_ENV = "dev" ]; then
    source docker_build.sh
elif [ APP_ENV = "prod" ]; then
    echo "从远程拉取包"
    docker pull sunkolin/${APP_NAME}:${APP_VERSION}
fi

# 停止并删除容器，再启动
sudo docker stop ${APP_NAME}
sudo docker rm -fv ${APP_NAME}
sudo docker run -d --name ${APP_NAME} -p ${APP_PORT}:${APP_PORT} -e APP_ENV=${APP_ENV} -e APP_PORT=${APP_PORT} sunkolin/${APP_NAME}:${APP_VERSION}