# be/api-admin — Counseling Admin API

관리자용 백엔드 API 서버.

## Tech Stack

| 분류 | 기술 |
|---|---|
| Language | Kotlin 2.3, JDK 25 |
| Framework | Spring Boot 4.0.2 + WebFlux (Reactive) |
| Architecture | Hexagonal + CQRS |
| RDB | PostgreSQL (R2DBC) |
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
| 서버 포트 | `8081` |
| 프로파일 | `local`, `test` |
| CORS origins | `localhost:3000, 3001, 3002, 3200` |
| API 경로 | `/api-adm/**` |
| Actuator | `/actuator/health`, `/actuator/info` |

주요 환경변수 (`application.yml` / `application-local.yml`):

| 환경변수 | 설명 | 기본값 (local) |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | 활성 프로파일 | `local` |
| `JWT_SECRET` | JWT 서명 키 | 개발용 키 |
| `LIVEKIT_URL` | LiveKit 서버 URL | `ws://localhost:7880` |
| `LIVEKIT_API_KEY` / `API_SECRET` | LiveKit 인증 | `devkey` / `secret` |

## Security (Role-based Access)

```
/actuator/health, /actuator/info  → permitAll()
/api-adm/super/**                 → SUPER_ADMIN
/api-adm/company/**               → SUPER_ADMIN, COMPANY_ADMIN
/api-adm/group/**                 → SUPER_ADMIN, COMPANY_ADMIN, GROUP_ADMIN
/api-adm/**                       → authenticated()
```

## 패키지 구조

```
com.counseling.admin/
├── domain/                  # 도메인 모델, 이벤트
├── port/
│   ├── inbound/             # Use case 인터페이스 (Query / Command)
│   └── outbound/            # Repository / 외부 시스템 포트
├── application/             # 서비스 구현 (Use case 구현체)
├── adapter/
│   ├── inbound/web/         # WebFlux 컨트롤러, DTO, 필터
│   └── outbound/
│       ├── persistence/     # R2DBC, MongoDB 어댑터
│       └── external/        # LiveKit 등 외부 연동
└── config/                  # Spring 설정 (Security, R2DBC, Redis, CORS 등)
```

## API 엔드포인트

| 도메인 | 컨트롤러 | 주요 경로 | 권한 |
|---|---|---|---|
| 인증 | `AdminAuthController` | `/api-adm/auth/**` | public |
| 테넌트 관리 | `TenantController` | `/api-adm/super/tenants/**` | SUPER_ADMIN |
| 회사 관리 | `CompanyController` | `/api-adm/company/**` | COMPANY_ADMIN+ |
| 그룹 관리 | `GroupController` | `/api-adm/group/**` | GROUP_ADMIN+ |
| 상담사 관리 | `AgentController` | `/api-adm/company/agents/**` | COMPANY_ADMIN+ |
| 모니터링 | `MonitoringController` | `/api-adm/company/monitoring/**` | COMPANY_ADMIN+ |
| 통계 | `StatsController` | `/api-adm/company/stats/**` | COMPANY_ADMIN+ |
| 피드백 조회 | `FeedbackController` | `/api-adm/company/feedback/**` | COMPANY_ADMIN+ |
