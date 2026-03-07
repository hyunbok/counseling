# be/api — Counseling API

화상 상담 플랫폼 백엔드 API 서버.

## Tech Stack

| 분류 | 기술 |
|---|---|
| Language | Kotlin 2.3, JDK 25 |
| Framework | Spring Boot 4.0.2 + WebFlux (Reactive) |
| Architecture | Hexagonal + CQRS |
| RDB | PostgreSQL (R2DBC) |
| DB Migration | Flyway (멀티테넌트 — meta + tenant별) |
| Document DB | MongoDB (Reactive) |
| Cache / PubSub | Redis (Reactive, Lettuce) |
| Auth | JWT (jjwt 0.12.x) + Spring Security |
| Video | LiveKit Server SDK 0.6.2 |
| JSON | Jackson 3.x (`tools.jackson.*`) |
| Build | Gradle 9.4.0, ktlint 1.5.0 |
| Test | Kotest 5.9 + MockK 1.13 + Reactor Test |

## 선행 조건

```bash
# 로컬 인프라 (PostgreSQL, MongoDB, Redis, LiveKit)
cd ../../infra && docker compose up -d
```

## 빌드 및 실행

```bash
./gradlew build                                        # 빌드
./gradlew bootRun --args='--spring.profiles.active=local'  # 실행
./gradlew test                                         # 테스트
./gradlew ktlintCheck                                  # 코드 스타일 검사
```

## 환경 설정

| 항목 | 기본값 |
|---|---|
| 서버 포트 | `8080` |
| 프로파일 | `local`, `test` |
| CORS origins | `localhost:3000, 3001, 3002, 3100` |
| API 경로 | `/api/**` |
| Actuator | `/actuator/health`, `/actuator/info` |

주요 환경변수 (`application.yml` / `application-local.yml`):

| 환경변수 | 설명 | 기본값 (local) |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | 활성 프로파일 | `local` |
| `JWT_SECRET` | JWT 서명 키 | 개발용 키 |
| `LIVEKIT_URL` | LiveKit 서버 URL | `http://localhost:7880` |
| `LIVEKIT_API_KEY` / `API_SECRET` | LiveKit 인증 | `devkey` / `secret` |
| `MAIL_HOST` / `MAIL_PORT` | 메일 서버 | `localhost:1025` |

## 패키지 구조

```
com.counseling.api/
├── domain/                  # 도메인 모델, 이벤트
├── port/
│   ├── inbound/             # Use case 인터페이스 (Query / Command)
│   └── outbound/            # Repository / 외부 시스템 포트
├── application/             # 서비스 구현 (Use case 구현체)
├── adapter/
│   ├── inbound/web/         # WebFlux 컨트롤러, DTO, 필터
│   └── outbound/
│       ├── persistence/     # R2DBC, MongoDB 어댑터
│       └── external/        # LiveKit, 메일 등 외부 연동
└── config/                  # Spring 설정 (Security, R2DBC, Redis, CORS 등)
```

## API 엔드포인트

| 도메인 | 컨트롤러 | 주요 경로 |
|---|---|---|
| 인증 | `AuthController` | `/api/auth/**` (login, refresh, password, name) |
| 상담사 | `AgentController` | `/api/agents/**` (상태 관리) |
| 대기열 | `QueueController` | `/api/queue/**` (SSE 스트림 포함) |
| 상담 채널 | `ChannelController` | `/api/channels/**` |
| 채팅 | `ChatController` | `/api/channels/{id}/chat/**` |
| 이력 | `HistoryController` | `/api/history/**` |
| 녹화 | `RecordingController` | `/api/channels/{id}/recording/**` |
| 피드백 | `FeedbackController` | `/api/feedback/**` |
| 화면 공유 | `CoBrowsingController` | `/api/channels/{id}/cobrowsing/**` |
| 화면 캡처 | `ScreenCaptureController` | `/api/channels/{id}/captures/**` |
| 파일 공유 | `SharedFileController` | `/api/channels/{id}/files/**` |
| 알림 | `NotificationController` | `/api/notifications/**` |
| LiveKit 웹훅 | `LiveKitWebhookController` | `/api/livekit/webhook` |

## DB 마이그레이션

Flyway로 멀티테넌트 마이그레이션을 관리합니다.

- **Meta DB**: `db/migration/` — 테넌트, 슈퍼 관리자 테이블
- **Tenant DB**: `db/tenant-migration/` — 상담사, 채널 등 업무 테이블

마이그레이션은 애플리케이션 시작 시 `FlywayMigrationConfig`에서 자동 실행됩니다.
