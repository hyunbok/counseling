# v1-2: Setup Backend API Module (be/api)

## 1. Overview

Kotlin 2.3 + Spring Boot 4.0.2 + WebFlux 기반 공개 API 모듈 초기 설정.
상담사/고객용 API 서버로, 헥사고널 아키텍처 + CQRS 패턴 적용.
독립적인 Gradle 빌드 환경 (프로젝트 루트에 빌드 설정 없음).

## 2. 구현 파일 목록

| File | Subtask | Description |
|------|---------|-------------|
| `be/api/settings.gradle.kts` | 1 | rootProject.name = "api" |
| `be/api/gradle.properties` | 1 | JVM args, kotlin.code.style |
| `be/api/build.gradle.kts` | 1 | 플러그인, 의존성 전체 정의 |
| `be/api/gradlew`, `gradlew.bat`, `gradle/wrapper/*` | 1 | Gradle wrapper |
| `be/api/.editorconfig` | 2 | ktlint_official 스타일 설정 |
| `be/api/src/main/kotlin/com/counseling/api/` 패키지들 | 3 | 헥사고널 패키지 구조 |
| `be/api/src/main/kotlin/.../ApiApplication.kt` | 4 | Spring Boot 엔트리포인트 |
| `be/api/src/main/kotlin/.../config/WebFluxConfig.kt` | 4 | CORS 설정 |
| `be/api/src/main/kotlin/.../config/SecurityConfig.kt` | 4 | WebFlux Security (permitAll) |
| `be/api/src/main/kotlin/.../config/JacksonConfig.kt` | 4 | Jackson Kotlin module |
| `be/api/src/main/resources/application.yml` | 4 | 기본 설정 (port 8080) |
| `be/api/src/main/resources/application-local.yml` | 4 | 로컬 개발 DB 연결 |
| `be/api/src/test/kotlin/.../ApiApplicationTests.kt` | 5 | Spring context load 테스트 |
| `be/api/src/test/kotlin/.../domain/SampleSpec.kt` | 5 | Kotest StringSpec 샘플 |

## 3. Gradle 빌드 설정

- **Plugins**: Kotlin 2.3.0, Spring Boot 4.0.2, dependency-management 1.1.7, ktlint 12.1.2
- **JDK**: toolchain 25
- **주요 의존성**:
  - spring-boot-starter-webflux, data-r2dbc, data-mongodb-reactive, data-redis-reactive
  - spring-boot-starter-security (permitAll, JWT는 이후 태스크)
  - jjwt-api:0.12.6 (impl/jackson은 runtimeOnly)
  - livekit-server-sdk:0.6.2
  - kotest-runner-junit5:5.9.1, kotest-assertions-core:5.9.1, mockk:1.13.13

## 4. 패키지 구조

```
com.counseling.api/
├── domain/              # 순수 도메인 (프레임워크 의존 없음)
├── port/
│   ├── inbound/         # Use case 인터페이스
│   └── outbound/        # Repository/외부 서비스 인터페이스
├── application/         # 서비스 구현 (port/inbound 구현체)
├── adapter/
│   ├── inbound/web/     # WebFlux 컨트롤러, DTO
│   └── outbound/
│       ├── persistence/ # R2DBC/MongoDB 구현체
│       └── external/    # LiveKit, Redis 등 외부 서비스
└── config/              # Spring @Configuration
```

## 5. 주요 결정사항

| Decision | Rationale |
|----------|-----------|
| dependency-management 1.1.7 | Spring Boot 4 BOM 지원 최소 버전 |
| LiveKit SDK 0.6.2 | 0.8.x는 Android 의존성 포함, JVM 서버 빌드 비호환 |
| SecurityConfig permitAll | JWT 설정은 이후 태스크, JwtDecoder 빈 없이 시작 가능 |
| exclude Mockito | MockK로 대체 (Kotlin idiom) |
| Kotest SpringExtension 미사용 | Spring Boot 4 비호환 (PRD 명시) |
| inbound/outbound 패키지명 | in/out은 Kotlin 예약어 |

## 6. Build Sequence

1. **Phase 1** (Subtask 1): Gradle 부트스트랩 — settings, properties, build.gradle.kts, wrapper
2. **Phase 2** (Subtask 2+3, 병렬 가능): .editorconfig + 패키지 구조 생성
3. **Phase 3** (Subtask 4): ApiApplication, Config 클래스, YAML 설정
4. **Phase 4** (Subtask 5): 테스트 인프라 — JUnit 5 + Kotest 샘플
5. **검증**: `./gradlew build` → 컴파일 + ktlintCheck + 테스트 통과
