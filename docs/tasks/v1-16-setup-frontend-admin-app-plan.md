# v1-16: Setup Frontend Admin App (fe/app-admin)

## 1. 개요

관리자용 대시보드 프론트엔드 앱. Next.js 16 + React 19 + TypeScript + Tailwind v4.
fe/app-agent 패턴 기반, JWT 인증 + 역할 기반 접근 제어(RBAC) + CRUD 관리 기능.
포트 3200, 단독 pnpm 프로젝트. LiveKit 의존성 없음.

## 2. 핵심 차이점 (vs fe/app-agent)

| 항목 | app-agent | app-admin |
|---|---|---|
| 포트 | 3100 | 3200 |
| API 베이스 | localhost:8080 /api/ | localhost:8081 /api-adm/ |
| 인증 역할 | 단일 agent 역할 | SUPER_ADMIN, COMPANY_ADMIN, GROUP_ADMIN |
| 사이드바 | 2개 고정 메뉴 | 6개 역할 필터링 메뉴 |
| Store 키 | auth-store | admin-auth-store |
| LiveKit | O | X |
| 페이지 | 4개 | 8개 (login, dashboard, tenants, groups, agents, monitoring, feedbacks) |
| UI 컴포넌트 | Button | Button + StatCard + DataTable + CreateModal |

## 3. 파일 목록

```
fe/app-admin/
├── .editorconfig, .gitignore, .prettierrc     # app-agent 복사
├── .env.example                                # API_URL (8081)
├── eslint.config.mjs, postcss.config.mjs       # app-agent 복사
├── next.config.ts, tsconfig.json               # app-agent 복사
├── package.json                                # name: app-admin, port 3200, no livekit
├── pnpm-workspace.yaml
├── src/app/
│   ├── globals.css                             # app-agent 동일 (@theme 토큰)
│   ├── layout.tsx                              # title: "관리자 대시보드"
│   ├── page.tsx                                # redirect('/dashboard')
│   ├── login/page.tsx                          # 관리자 로그인
│   ├── dashboard/page.tsx                      # StatCard 그리드 + 최근 활동
│   ├── tenants/page.tsx                        # 테넌트 CRUD (SUPER_ADMIN)
│   ├── groups/page.tsx                         # 그룹 CRUD (SUPER/COMPANY_ADMIN)
│   ├── agents/page.tsx                         # 상담사 CRUD (전 역할)
│   ├── monitoring/page.tsx                     # 실시간 세션 모니터링
│   └── feedbacks/page.tsx                      # 피드백 목록 + 필터
├── src/components/
│   ├── providers.tsx                           # app-agent 복사
│   ├── ui/button.tsx                           # app-agent 복사
│   ├── ui/stat-card.tsx                        # 통계 카드 (아이콘, 값, 트렌드)
│   ├── ui/data-table.tsx                       # 제네릭 테이블 (정렬, 페이지네이션)
│   ├── ui/create-modal.tsx                     # 모달 다이얼로그 (생성/수정 폼)
│   ├── layout/sidebar.tsx                      # 역할 기반 네비게이션
│   ├── layout/sidebar-layout.tsx               # app-agent 복사
│   └── layout/theme-toggle.tsx                 # app-agent 복사
├── src/hooks/
│   ├── use-auth.ts                             # /api-adm/auth/login, logout
│   ├── use-tenants.ts                          # 테넌트 CRUD hooks
│   ├── use-groups.ts                           # 그룹 CRUD hooks
│   ├── use-agents.ts                           # 상담사 CRUD hooks
│   ├── use-monitoring.ts                       # 실시간 세션 (5s poll)
│   └── use-feedbacks.ts                        # 피드백 목록 hooks
├── src/stores/
│   └── auth-store.ts                           # AdminRole, localStorage persist
├── src/lib/
│   ├── api.ts                                  # Axios + JWT + /api-adm prefix
│   └── query-client.ts                         # app-agent 복사
└── src/types/
    └── index.ts                                # AdminRole, Tenant, Group, Agent 등
```

## 4. 역할별 메뉴 접근

| 메뉴 | SUPER_ADMIN | COMPANY_ADMIN | GROUP_ADMIN |
|---|---|---|---|
| 대시보드 | O | O | O |
| 테넌트 관리 | O | X | X |
| 그룹 관리 | O | O | X |
| 상담사 관리 | O | O | O |
| 모니터링 | O | O | O |
| 피드백 | O | O | O |

## 5. 서브태스크별 구현

### ST-1: Next.js 초기화 + 의존성 설치 (의존성 없음)
- pnpm create next-app + 의존성 설치 (livekit 제외)
- 설정 파일 (app-agent에서 복사/생성)
- package.json: port 3200, no livekit deps

### ST-2: Tailwind CSS 디자인 시스템 (→ ST-1)
- globals.css (app-agent 동일 @theme 토큰)

### ST-3: 사이드바 레이아웃 + 역할 기반 네비 (→ ST-2)
- sidebar.tsx (역할 필터링), sidebar-layout.tsx, theme-toggle.tsx, button.tsx, providers.tsx
- layout.tsx (root)

### ST-4: 상태관리 + 인증 + 타입 정의 (→ ST-1)
- types/index.ts (AdminRole, 엔티티 타입)
- auth-store.ts (AdminRole, admin-auth-store 키)
- api.ts (port 8081, /api-adm/ 프리픽스)
- query-client.ts, use-auth.ts

### ST-5: 페이지 + 재사용 컴포넌트 (→ ST-3, ST-4)
- stat-card.tsx, data-table.tsx, create-modal.tsx
- login, dashboard, tenants, groups, agents, monitoring, feedbacks 페이지
- use-tenants.ts, use-groups.ts, use-agents.ts, use-monitoring.ts, use-feedbacks.ts

## 6. 빌드 시퀀스

**Phase 1** (ST-1): scaffold → `pnpm dev --port 3200` 검증
**Phase 2** (ST-2): globals.css → `pnpm build` 검증
**Phase 3** (ST-3 + ST-4 병렬): 레이아웃 + 상태관리 → `pnpm build && pnpm lint` 검증
**Phase 4** (ST-5): 페이지 + 컴포넌트 → `pnpm build` 최종 검증

## 7. 주요 결정사항

1. **app-agent 패턴 따름**: 동일 deps(livekit 제외), 동일 디자인 토큰, 동일 코딩 규약
2. **포트 3200**: 고객앱(3000), 상담사앱(3100)과 충돌 방지
3. **admin-auth-store 키**: 같은 도메인에서 상담사앱과 localStorage 충돌 방지
4. **LiveKit 미포함**: 관리자 앱은 순수 CRUD + 모니터링
5. **DataTable 제네릭 설계**: Column<T> 타입으로 모든 CRUD 페이지에서 재사용
6. **역할 필터링 사이드바**: allNavItems 배열에 roles 속성, user.role로 필터
7. **.gitkeep 미사용**: 빈 디렉토리 미생성
