# be/api — Counseling API

고객/상담사용 백엔드 API 서버.

## Tech Stack

- Kotlin 2.3 + Spring Boot 4.0 + WebFlux
- Hexagonal Architecture + CQRS
- PostgreSQL (R2DBC) + MongoDB (Reactive) + Redis (Reactive)
- LiveKit Server SDK

## 실행

```bash
# 로컬 인프라 (Docker Compose)
cd ../../infra && docker compose up -d

# 빌드 및 실행
./gradlew build
./gradlew bootRun --args='--spring.profiles.active=local'
```

## 환경

| 항목 | 값 |
|---|---|
| 포트 | 8080 |
| 프로파일 | local, test |
| CORS | `/api/**` |
| API 경로 | `/api/**` |

## 패키지 구조

```
com.counseling.api/
├── domain/              # 도메인 모델
├── port/inbound/        # Use case 인터페이스
├── port/outbound/       # Repository/외부 시스템 인터페이스
├── application/         # 서비스 구현
├── adapter/inbound/web/ # WebFlux 컨트롤러
├── adapter/outbound/    # 영속성/외부 시스템 어댑터
└── config/              # Spring 설정
```

## 테스트

```bash
./gradlew test
```
