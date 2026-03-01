# Counseling Platform

WebRTC 기반 화상 상담 플랫폼. 상담사와 고객이 실시간 영상 통화로 상담을 진행하고, 관리자가 상담 현황을 모니터링할 수 있는 웹 서비스.

## Tech Stack

### Backend

- **Kotlin 2.3** + **Spring Boot 4.0.2** + **Spring WebFlux** (Reactive)
- **JDK 25**, Gradle Kotlin DSL
- **CQRS**: Command(PostgreSQL) / Query(MongoDB)
- **Hexagonal Architecture**: domain → port → application → adapter
- **Multi-tenant**: DB per Tenant (동적 라우팅)

### Frontend

- **React** + **Next.js** + **TypeScript**
- **pnpm**, Tailwind CSS, Zustand, TanStack Query

### Infrastructure

- **PostgreSQL 16**: CQRS write-side (Meta DB + Tenant DB)
- **MongoDB 7**: CQRS read-side
- **Redis 7**: JWT 토큰 블랙리스트, 세션 관리
- **LiveKit**: WebRTC SFU (영상/음성, 녹화)

## Project Structure

```
counseling/
├── be/
│   ├── api/              # 공개 API (상담사/고객용) - port 8080
│   └── api-admin/        # 어드민 API - port 8081
├── fe/
│   ├── app-agent/        # 상담사 프론트엔드
│   ├── app-customer/     # 고객 프론트엔드
│   └── app-admin/        # 어드민 프론트엔드
├── docs/                 # PRD, 설계 문서, 목업
├── storage/              # 녹화, 파일, 캡처 (런타임 생성)
├── docker-compose.yml    # 로컬 개발 인프라
└── .env.example          # 환경 변수 템플릿
```

## Getting Started

### Prerequisites

- Docker & Docker Compose
- JDK 25+
- Node.js 20+ & pnpm

### 1. 환경 변수 설정

```bash
cp .env.example .env
```

### 2. 인프라 실행

```bash
# 전체 서비스 (DB + LiveKit)
docker compose --profile all up -d

# DB 서비스만 (PostgreSQL, MongoDB, Redis)
docker compose --profile db up -d

# LiveKit만
docker compose --profile livekit up -d
```

### 3. 상태 확인

```bash
docker compose ps
```

### 서비스 포트

| Service | Port | Description |
|---------|------|-------------|
| PostgreSQL (Meta DB) | 5432 | 테넌트/슈퍼유저 관리 |
| PostgreSQL (Tenant DB) | 5433 | 테넌트별 업무 데이터 |
| MongoDB | 27017 | CQRS read-side |
| Redis | 6379 | 토큰 블랙리스트/세션 |
| LiveKit | 7880 | WebRTC 시그널링 |
| API | 8080 | 공개 API |
| API Admin | 8081 | 어드민 API |

### 인프라 종료

```bash
# 컨테이너 종료 (데이터 유지)
docker compose down

# 컨테이너 + 볼륨 삭제 (데이터 초기화)
docker compose down -v
```
