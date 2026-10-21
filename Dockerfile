FROM ubuntu:24.04

LABEL maintainer="sunkolin <sunkolin@qq.com>"

# 禁止交互式提示,防止构建卡住
ENV DEBIAN_FRONTEND=noninteractive

# 替换阿里云源
RUN sed -i 's|archive.ubuntu.com|mirrors.aliyun.com|g' /etc/apt/sources.list

# 设置时区
ENV TZ=Asia/Shanghai
RUN apt-get update && apt-get install -y --no-install-recommends tzdata
RUN ln -snf /usr/share/zoneinfo/${TZ} /etc/localtime && echo ${TZ} > /etc/timezone

# 设置别名
RUN echo "alias ll='ls -al'" >> /etc/profile
ENV ENV="/etc/profile"

# curl,wget,telnet,net-tools,java
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    wget \
    telnet \
    procps \
    iproute2 \
    net-tools \
    less \
    openjdk-17-jre \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# java
ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
ENV PATH=$PATH:$JAVA_HOME/bin

# 部署应用
WORKDIR /app
COPY ./target/*.jar /app/

# 启动
CMD ["bash", "-c", "java -Xms1g -Xmx2g -jar /app/*.jar --spring.profiles.active=${APP_ENV:-dev} --server.port=${APP_PORT:-8080}"]