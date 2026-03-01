# fe/app-admin — 관리자 대시보드 앱

테넌트, 그룹, 상담사를 관리하고 실시간 모니터링하는 관리자 프론트엔드 앱.

## Tech Stack

- Next.js 16 + React 19 + TypeScript
- Tailwind CSS v4 (@theme 디자인 토큰)
- Zustand (상태관리) + TanStack Query (서버 상태)
- Axios (API 클라이언트 + JWT 인터셉터)

## 실행

```bash
pnpm install
pnpm dev        # http://localhost:3200
```

## 빌드

```bash
pnpm build
pnpm lint
```

## 환경 변수

`.env.example` 참조:

```
NEXT_PUBLIC_API_URL=http://localhost:8081
```

## 페이지

| 경로 | 설명 | 접근 권한 |
|---|---|---|
| `/login` | 관리자 로그인 | 전체 |
| `/dashboard` | 통계 대시보드 | 전 역할 |
| `/tenants` | 테넌트 관리 (CRUD) | SUPER_ADMIN |
| `/groups` | 그룹 관리 (CRUD) | SUPER_ADMIN, COMPANY_ADMIN |
| `/agents` | 상담사 관리 (CRUD) | 전 역할 |
| `/monitoring` | 실시간 세션 모니터링 | 전 역할 |
| `/feedbacks` | 피드백 목록 | 전 역할 |

## 역할

| 역할 | 설명 |
|---|---|
| SUPER_ADMIN | 시스템 관리자 — 모든 기능 접근 |
| COMPANY_ADMIN | 회사 관리자 — 그룹/상담사 관리 |
| GROUP_ADMIN | 그룹 관리자 — 소속 상담사 관리 |

## 디렉토리 구조

```
src/
├── app/           # 페이지 라우트
├── components/    # layout (sidebar), ui (button, stat-card, data-table, create-modal)
├── hooks/         # use-auth, use-auth-guard, use-tenants, use-groups, use-agents, ...
├── stores/        # auth-store (localStorage, partialize)
├── lib/           # api (JWT 인터셉터), query-client
└── types/         # AdminRole, Tenant, Group, Agent 등 타입 정의
```
