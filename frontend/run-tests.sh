#!/bin/bash

# 현재 디렉토리 저장
CURRENT_DIR=$(pwd)

# 이미 실행 중인 프로세스 확인
BACKEND_RUNNING=$(lsof -i:8080 | grep LISTEN)
FRONTEND_RUNNING=$(lsof -i:3000 | grep LISTEN)

# 애플리케이션 실행 여부 확인
if [ -z "$BACKEND_RUNNING" ] || [ -z "$FRONTEND_RUNNING" ]; then
  echo "백엔드 또는 프론트엔드가 실행되고 있지 않습니다."
  echo "애플리케이션을 시작합니다..."
  
  # 백엔드 시작
  if [ -z "$BACKEND_RUNNING" ]; then
    echo "Starting backend..."
    cd /Users/hyemin/lipcoding/backend
    ./gradlew bootRun &
    BACKEND_PID=$!
    echo "Backend started with PID: $BACKEND_PID"
  else
    echo "Backend is already running."
  fi
  
  # 프론트엔드 시작
  if [ -z "$FRONTEND_RUNNING" ]; then
    echo "Starting frontend..."
    cd /Users/hyemin/lipcoding/frontend
    npm start &
    FRONTEND_PID=$!
    echo "Frontend started with PID: $FRONTEND_PID"
  else
    echo "Frontend is already running."
  fi
  
  # 애플리케이션이 시작될 때까지 대기
  echo "Waiting for applications to start..."
  sleep 15
  
  echo "Applications started and ready for testing!"
else
  echo "Applications are already running."
fi

# 원래 디렉토리로 돌아가기
cd $CURRENT_DIR

# Playwright 테스트 실행
echo "Running tests..."
npx playwright test

# 테스트 결과 표시
echo "Tests completed. Opening test report..."
npx playwright show-report
