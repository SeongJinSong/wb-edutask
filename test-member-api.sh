#!/bin/bash

# WB Education Task Management System - 회원가입 API 테스트 스크립트
# 작성자: WB Development Team
# 버전: 1.0.0

echo "=========================================="
echo "WB Education Task Management System"
echo "회원가입 API 테스트"
echo "=========================================="

# 서버 URL
BASE_URL="http://localhost:8080/api"

# 테스트 데이터
echo ""
echo "1. 수강생 회원 가입 테스트"
echo "------------------------"

curl -X POST "${BASE_URL}/members/register" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "홍길동",
    "email": "hong@weolbu.com",
    "phoneNumber": "010-1234-5678",
    "password": "Test123",
    "memberType": "STUDENT"
  }' \
  -w "\nHTTP Status: %{http_code}\n" \
  -s

echo ""
echo "2. 강사 회원 가입 테스트"
echo "------------------------"

curl -X POST "${BASE_URL}/members/register" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "너나위",
    "email": "neona@weolbu.com",
    "phoneNumber": "010-9876-5432",
    "password": "Instructor123",
    "memberType": "INSTRUCTOR"
  }' \
  -w "\nHTTP Status: %{http_code}\n" \
  -s

echo ""
echo "3. 중복 이메일 가입 시도 테스트"
echo "------------------------"

curl -X POST "${BASE_URL}/members/register" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "홍길동2",
    "email": "hong@weolbu.com",
    "phoneNumber": "010-1111-2222",
    "password": "Test456",
    "memberType": "STUDENT"
  }' \
  -w "\nHTTP Status: %{http_code}\n" \
  -s

echo ""
echo "4. 유효성 검증 실패 테스트 (잘못된 비밀번호)"
echo "------------------------"

curl -X POST "${BASE_URL}/members/register" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "김철수",
    "email": "kim@weolbu.com",
    "phoneNumber": "010-3333-4444",
    "password": "123",
    "memberType": "STUDENT"
  }' \
  -w "\nHTTP Status: %{http_code}\n" \
  -s

echo ""
echo "5. 회원 조회 테스트 (이메일로)"
echo "------------------------"

curl -X GET "${BASE_URL}/members/email/hong@weolbu.com" \
  -H "Content-Type: application/json" \
  -w "\nHTTP Status: %{http_code}\n" \
  -s

echo ""
echo "6. 회원 조회 테스트 (ID로)"
echo "------------------------"

curl -X GET "${BASE_URL}/members/1" \
  -H "Content-Type: application/json" \
  -w "\nHTTP Status: %{http_code}\n" \
  -s

echo ""
echo "=========================================="
echo "테스트 완료!"
echo "=========================================="
