#!/bin/bash

# 현재 디렉토리 저장
CURRENT_DIR=$(pwd)

# 백엔드 시작
echo "Starting backend..."
cd /Users/hyemin/lipcoding/backend
./gradlew bootRun &
BACKEND_PID=$!

# 프론트엔드 시작
echo "Starting frontend..."
cd /Users/hyemin/lipcoding/frontend
npm start &
FRONTEND_PID=$!

# 종료 트랩 설정
trap "kill $BACKEND_PID $FRONTEND_PID; exit" INT TERM EXIT

# 앱이 시작될 때까지 대기
echo "Waiting for applications to start..."
sleep 10

# 테스트를 실행할 수 있는 환경이 되었음을 알림
echo "Applications started!"
echo "Backend PID: $BACKEND_PID"
echo "Frontend PID: $FRONTEND_PID"
echo "Press Ctrl+C to stop both applications"

# 터미널이 종료되지 않도록 유지
wait
