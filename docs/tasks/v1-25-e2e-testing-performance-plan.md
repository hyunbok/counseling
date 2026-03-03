# v1-25: End-to-End Testing and Performance Optimization

## 1. Overview

E2E 테스트 인프라 구축 및 프로덕션 준비를 위한 성능 최적화. Playwright 기반 다중 브라우저 테스트로 고객-상담사 전체 흐름을 검증하고, FE/BE 성능을 개선한다.

**의존성**: v1-13(고객 영상통화), v1-15(상담사 대시보드), v1-17(관리자), v1-18(알림), v1-19(피드백), v1-20(히스토리) — 모두 완료

## 2. E2E Test Infrastructure

### 2-1. 프로젝트 구조
```
e2e/
├── package.json          # @playwright/test
├── tsconfig.json
├── playwright.config.ts  # 멀티 프로젝트: customer(3000), agent(3100), api(8080)
├── global-setup.ts       # Docker 헬스체크, 테스트 에이전트 시딩
├── fixtures/
│   └── test-helpers.ts   # loginAgent(), joinAsCustomer(), waitForVideoCall()
└── tests/
    ├── customer-flow.spec.ts
    ├── agent-flow.spec.ts
    ├── full-session.spec.ts
    └── chat-during-call.spec.ts
```

### 2-2. 테스트 시나리오
| 테스트 | 설명 |
|--------|------|
| customer-flow | 고객 참여 → 대기 → 통화 → 피드백 |
| agent-flow | 상담사 로그인 → 대시보드 → 수락 → 통화 → 종료 |
| full-session | 고객+상담사 동시 다중 브라우저 전체 세션 |
| chat-during-call | 통화 중 채팅 메시지 교환 |

### 2-3. 실제 셀렉터 (코드베이스 기반)
- 고객 참여: `input#name`, `input#contact`, `button[aria-label="상담 시작하기"]`
- 대기실: `div[role="status"]` (스피너), `p.text-3xl.font-bold` (순번)
- 상담사 로그인: `input#username`, `input#password`, `button[aria-label="로그인"]`
- 상담사 수락: `button[aria-label$="수락"]`
- 피드백 별점: `button[aria-label="5점"]`
- 통화 종료: `data-testid` 속성 추가 필요

## 3. Frontend Performance Optimization

### 3-1. Code Splitting (next/dynamic)
- `fe/app-customer/src/app/call/[id]/page.tsx`: LiveKit `CallInner` → `next/dynamic({ ssr: false })`
- `fe/app-agent/src/app/call/[id]/page.tsx`: `CallPageInner` → `next/dynamic({ ssr: false })`

### 3-2. Package Import Optimization
- 양쪽 `next.config.ts`에 `experimental.optimizePackageImports` 추가
  - `@heroicons/react`, `@livekit/components-react`

### 3-3. TanStack Query Tuning
- 양쪽 `query-client.ts`에 `gcTime: 300_000` 추가
- `fe/app-agent/src/hooks/use-queue.ts`: `refetchInterval` 5s → 10s (SSE가 실시간 처리)

## 4. Backend Performance Optimization

### 4-1. Redis Connection Pooling
- `commons-pool2:2.12.0` 의존성 추가 (`build.gradle.kts`)
- `RedisConfig.kt`: `LettucePoolingClientConfiguration` — maxTotal=50, maxIdle=25, minIdle=5
- `application.yml`: `spring.data.redis.lettuce.pool` 설정 추가

### 4-2. MongoDB Configuration
- 새 `MongoConfig.kt` 생성
  - `ReadPreference.secondaryPreferred()` (로컬 단일노드에서는 primary 폴백)
  - `WriteConcern.MAJORITY`
- `application.yml`: `auto-index-creation: true`

### 4-3. LiveKit Room Settings
- `LiveKitAdapter.kt` `createRoom()`: `emptyTimeoutSec=300`, `maxParticipants=2`

## 5. Implementation File List

| # | File | Action | Subtask |
|---|------|--------|---------|
| 1 | `e2e/package.json` | CREATE | E2E 인프라 |
| 2 | `e2e/tsconfig.json` | CREATE | E2E 인프라 |
| 3 | `e2e/playwright.config.ts` | CREATE | E2E 인프라 |
| 4 | `e2e/global-setup.ts` | CREATE | E2E 인프라 |
| 5 | `e2e/fixtures/test-helpers.ts` | CREATE | E2E 인프라 |
| 6 | `e2e/tests/customer-flow.spec.ts` | CREATE | E2E 테스트 |
| 7 | `e2e/tests/agent-flow.spec.ts` | CREATE | E2E 테스트 |
| 8 | `e2e/tests/full-session.spec.ts` | CREATE | E2E 테스트 |
| 9 | `e2e/tests/chat-during-call.spec.ts` | CREATE | E2E 테스트 |
| 10 | `fe/app-customer/next.config.ts` | MODIFY | FE 성능 |
| 11 | `fe/app-agent/next.config.ts` | MODIFY | FE 성능 |
| 12 | `fe/app-customer/src/lib/query-client.ts` | MODIFY | FE 성능 |
| 13 | `fe/app-agent/src/lib/query-client.ts` | MODIFY | FE 성능 |
| 14 | `fe/app-agent/src/hooks/use-queue.ts` | MODIFY | FE 성능 |
| 15 | `fe/app-customer/src/app/call/[id]/page.tsx` | MODIFY | FE 성능 |
| 16 | `fe/app-agent/src/app/call/[id]/page.tsx` | MODIFY | FE 성능 |
| 17 | `be/api/build.gradle.kts` | MODIFY | BE 성능 |
| 18 | `be/api/.../config/RedisConfig.kt` | MODIFY | BE 성능 |
| 19 | `be/api/.../config/MongoConfig.kt` | CREATE | BE 성능 |
| 20 | `be/api/.../resources/application.yml` | MODIFY | BE 성능 |
| 21 | `be/api/.../resources/application-local.yml` | MODIFY | BE 성능 |
| 22 | `be/api/.../external/LiveKitAdapter.kt` | MODIFY | BE 성능 |

## 6. Build Sequence

**Phase 1**: E2E 인프라 (파일 1-5) → `npm install && npx playwright install`
**Phase 2**: E2E 테스트 (파일 6-9) → 테스트 작성
**Phase 3**: FE 성능 (파일 10-16) → 빌드 검증
**Phase 4**: BE 성능 (파일 17-22) → 빌드 검증
**Phase 5**: 통합 검증 → E2E 테스트 실행
