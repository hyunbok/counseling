# v1-3: Setup Backend Admin API Module (be/api-admin)

## 1. 개요

`be/api` 모듈을 미러링하여 관리자 전용 API 모듈(`be/api-admin`)을 구성한다.
포트 8081, 패키지 `com.counseling.admin`, CORS 경로 `/api-adm/**`.
Gradle standalone 프로젝트로 `be/api`와 동일한 의존성 및 빌드 체인을 사용한다.

## 2. 핵심 차이점 (vs be/api)

| 항목 | be/api | be/api-admin |
|---|---|---|
| rootProject.name | api | api-admin |
| server.port | 8080 | 8081 |
| spring.application.name | counseling-api | counseling-api-admin |
| 패키지 | com.counseling.api | com.counseling.admin |
| CORS 경로 | /api/** | /api-adm/** |
| Security | permitAll() | Role-based (SUPER_ADMIN, COMPANY_ADMIN, GROUP_ADMIN) |

## 3. 파일 목록

```
be/api-admin/
├── .editorconfig                              # api와 동일
├── build.gradle.kts                           # api와 동일
├── gradle.properties                          # api와 동일
├── settings.gradle.kts                        # rootProject.name = "api-admin"
├── gradlew / gradlew.bat                      # api에서 복사
├── gradle/wrapper/                            # api에서 복사
├── src/main/kotlin/com/counseling/admin/
│   ├── AdminApplication.kt                    # 진입점
│   └── config/
│       ├── SecurityConfig.kt                  # Role-based 보안
│       └── WebFluxConfig.kt                   # CORS /api-adm/**
├── src/main/resources/
│   ├── application.yml                        # 8081, admin 설정
│   └── application-local.yml                  # DB/Redis/LiveKit 연결
├── src/test/kotlin/com/counseling/admin/
│   ├── AdminApplicationTests.kt               # 컨텍스트 로드 테스트
│   └── domain/SampleSpec.kt                   # Kotest 검증
└── src/test/resources/
    └── application-test.yml                   # autoconfigure 제외
```

## 4. SecurityConfig 설계

```
/actuator/health, /actuator/info  → permitAll()
/api-adm/super/**                → hasRole("SUPER_ADMIN")
/api-adm/company/**              → hasAnyRole("SUPER_ADMIN", "COMPANY_ADMIN")
/api-adm/group/**                → hasAnyRole("SUPER_ADMIN", "COMPANY_ADMIN", "GROUP_ADMIN")
/api-adm/**                      → authenticated()
anyExchange                      → denyAll()
```

CSRF 비활성, HTTP Basic/Form Login 비활성. JWT 필터 통합은 인증 태스크에서 구현.

## 5. 서브태스크별 구현 파일

### ST-1: Gradle 빌드 설정
- `be/api-admin/build.gradle.kts` (api 복사)
- `be/api-admin/settings.gradle.kts` (api-admin)
- `be/api-admin/gradle.properties` (api 복사)
- `be/api-admin/.editorconfig` (api 복사)
- `be/api-admin/gradlew`, `gradlew.bat`, `gradle/wrapper/*` (api 복사)

### ST-2: ktlint 설정
- `.editorconfig`에 이미 `ktlint_code_style = ktlint_official` 포함
- `build.gradle.kts`에 ktlint 플러그인 이미 포함
- ST-1에서 함께 처리 (별도 작업 불필요)

### ST-3: 헥사고날 패키지 구조
- `AdminApplication.kt` 생성 시 패키지 자동 생성
- `config/SecurityConfig.kt`, `config/WebFluxConfig.kt`
- 빈 패키지(domain, port, application, adapter)는 .gitkeep 없이 실제 구현 시 생성

### ST-4: Application 진입점 + 설정
- `AdminApplication.kt`
- `config/SecurityConfig.kt` (Role-based)
- `config/WebFluxConfig.kt` (CORS /api-adm/**)
- `application.yml`, `application-local.yml`

### ST-5: 테스트 인프라
- `application-test.yml` (autoconfigure 제외)
- `AdminApplicationTests.kt` (@SpringBootTest)
- `domain/SampleSpec.kt` (Kotest StringSpec)

## 6. 빌드 시퀀스

**Phase 1** (ST-1, ST-2): Gradle 스캐폴드 → `./gradlew tasks` 검증
**Phase 2** (ST-3, ST-4): 소스 코드 + 설정 → `./gradlew compileKotlin` 검증
**Phase 3** (ST-5): 테스트 인프라 → `./gradlew build` 전체 빌드 검증

ST-1/ST-2는 병합 가능, ST-3/ST-4도 병합 가능 → 실질적으로 2 Phase 구현.

## 7. 주요 결정사항

1. **Standalone Gradle 프로젝트**: be/api 패턴 유지, multi-module 불채택
2. **패키지 분리**: `com.counseling.admin` — 향후 공유 도메인은 `be/common` 모듈로 추출
3. **동일 데이터소스**: 같은 PostgreSQL/MongoDB/Redis 인스턴스 접근 (local 프로파일)
4. **.gitkeep 미사용**: 빈 패키지 디렉토리는 생성하지 않음
5. **Security 스켈레톤**: Role 구조만 선언, JWT 필터는 인증 태스크에서 구현
