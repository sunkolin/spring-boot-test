#!/bin/bash
# 变量
export APP_NAME=spring-boot-test
export APP_PORT=6005
export APP_VERSION=1.0.0
export APP_ENV=dev

PID_FILE="${APP_NAME}.pid"
LOG_FILE="${APP_NAME}.log"
JAR_FILE="/app/*.jar"

echo "开始部署 ${APP_NAME}..."

# 1. 检查并停止正在运行的进程
echo "检查并停止正在运行的进程..."
if [ -f "$PID_FILE" ]; then
    OLD_PID=$(cat "$PID_FILE")
    if ps -p $OLD_PID > /dev/null 2>&1; then
        echo "检测到进程 (PID: $OLD_PID) 正在运行，正在停止..."
        kill $OLD_PID
        sleep 3
        if ps -p $OLD_PID > /dev/null 2>&1; then
            echo "正常停止失败，强制终止进程..."
            kill -9 $OLD_PID
        fi
        rm -f "$PID_FILE"
        echo "已停止旧进程"
    else
        echo "PID 文件存在但进程未运行"
        rm -f "$PID_FILE"
    fi
else
    # 尝试通过进程名查找
    PID=$(ps aux | grep "${JAR_FILE}" | grep -v grep | awk '{print $2}')
    if [ ! -z "$PID" ]; then
        echo "检测到进程 (PID: $PID) 正在运行，正在停止..."
        kill $PID
        sleep 3
        if ps -p $PID > /dev/null 2>&1; then
            kill -9 $PID
        fi
        echo "已停止旧进程"
    else
        echo "没有正在运行的进程"
    fi
fi

# 2. 启动新项目（后台执行）
echo "启动新项目..."
nohup java -Xms1G -Xmx2G -jar ${JAR_FILE} --spring.profiles.active=${APP_ENV} --server.port=${APP_PORT} > ${LOG_FILE} 2>&1 &
NEW_PID=$!
echo $NEW_PID > "$PID_FILE"

echo "项目已启动，PID: ${NEW_PID}"
echo "日志文件：${LOG_FILE}"
echo "部署完成！"