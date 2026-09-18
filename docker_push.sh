#!/bin/bash

# 变量
export APP_NAME=spring-boot-test
export APP_VERSION=1.0.0

# 打包
mvn clean install -Dmaven.test.skip=true

# 打镜像
sudo docker build -t sunkolin/${APP_NAME}:${APP_VERSION} -f Dockerfile .
sudo docker tag sunkolin/${APP_NAME}:${APP_VERSION} sunkolin/${APP_NAME}:latest

# 推送
sudo docker login --username=sunkolin@qq.com --password='xxx'
sudo docker push registry.cn-hangzhou.aliyuncs.com/sunkolin/${APP_NAME}:${APP_VERSION}
sudo docker push registry.cn-hangzhou.aliyuncs.com/sunkolin/${APP_NAME}:latest