# v1-14: Setup Frontend Agent App (fe/app-agent)

## 1. 개요

상담사용 대시보드 프론트엔드 앱. Next.js 16 + React 19 + TypeScript + Tailwind v4.
fe/app-customer 패턴을 기반으로 JWT 인증, 사이드바 레이아웃, 상담 관리 기능 추가.
포트 3100, 단독 pnpm 프로젝트.

## 2. 핵심 차이점 (vs fe/app-customer)

| 항목 | app-customer | app-agent |
|---|---|---|
| 포트 | 3000 | 3100 |
| 인증 | 없음 (익명 참여) | JWT 로그인 (access + refresh) |
| 레이아웃 | 단일 컬럼 중앙 | 사이드바 + 메인 / 풀스크린(통화) |
| 라우트 | /, /waiting, /call/[id], /feedback | /login, /dashboard, /call/[id], /history |
| API 인터셉터 | 기본 axios | Bearer 토큰 + 401 리프레시 |
| Store 퍼시스트 | sessionStorage | localStorage (auth), sessionStorage (call) |
| 상태 관리 | 고객 정보만 | 에이전트 상태 (Online/Away/Wrap-up) |
| 통화 도구 | 마이크, 카메라, 종료 | + 화면공유, 캡처, 채팅, 메모 |

## 3. 파일 목록

```
fe/app-agent/
├── .editorconfig, .gitignore, .prettierrc    # app-customer 복사
├── .env.example                               # API_URL, LIVEKIT_URL
├── eslint.config.mjs, postcss.config.mjs      # app-customer 복사
├── next.config.ts, tsconfig.json              # app-customer 복사
├── package.json                               # name: app-agent, port 3100
├── pnpm-workspace.yaml
├── src/app/
│   ├── globals.css                            # app-customer 동일 (@theme 토큰)
│   ├── layout.tsx                             # title: "상담사 대시보드"
│   ├── login/page.tsx                         # JWT 로그인 (사이드바 없음)
│   ├── dashboard/page.tsx                     # 대기열 + 상태 + 최근 세션
│   ├── call/[id]/page.tsx                     # 풀스크린 통화 (사이드바 없음)
│   └── history/page.tsx                       # 상담 이력 테이블
├── src/components/
│   ├── providers.tsx                          # QueryClientProvider
│   ├── ui/button.tsx                          # app-customer 복사
│   ├── layout/
│   │   ├── sidebar.tsx                        # 네비게이션, 상태 표시, 사용자 정보
│   │   ├── sidebar-layout.tsx                 # aside + main 구성
│   │   └── theme-toggle.tsx                   # app-customer 복사
│   ├── queue/queue-list.tsx                   # 대기열 항목 + 수락 버튼
│   └── call/
│       ├── video-room.tsx                     # LiveKit 비디오
│       ├── chat-panel.tsx                     # 실시간 채팅
│       ├── note-panel.tsx                     # 메모 작성
│       └── tool-bar.tsx                       # 미디어 컨트롤 바
├── src/hooks/
│   ├── use-auth.ts                            # useLogin, useLogout
│   ├── use-agent-status.ts                    # 상태 조회/변경
│   ├── use-queue.ts                           # 대기열 폴링 + 수락
│   └── use-video-call.ts                      # LiveKit + 화면공유
├── src/stores/
│   ├── auth-store.ts                          # JWT, user info (localStorage)
│   └── call-store.ts                          # 통화 상태, 탭, 메모 (sessionStorage)
└── src/lib/
    ├── api.ts                                 # Axios + JWT 인터셉터
    └── query-client.ts                        # app-customer 복사
```

## 4. 에이전트 상태 색상

| 상태 | 색상 | 사용자 선택 |
|---|---|---|
| Online | Green-500 | O |
| Away | Amber-500 | O |
| Wrap-up | Purple-500 | O |
| Offline | Gray-400 | X (로그인 전) |
| Busy | Red-500 | X (시스템 설정) |

## 5. 서브태스크별 구현

### ST-1: Next.js 초기화 + 의존성 설치 (의존성 없음)
- pnpm create next-app + 의존성 설치
- 설정 파일 (app-customer에서 복사/생성)

### ST-2: Tailwind CSS 디자인 시스템 (→ ST-1)
- globals.css (app-customer 동일 @theme 토큰)

### ST-3: 사이드바 레이아웃 (→ ST-2)
- sidebar.tsx, sidebar-layout.tsx, theme-toggle.tsx, button.tsx, providers.tsx
- layout.tsx (root)

### ST-4: 상태관리 + 인증 hooks (→ ST-1)
- auth-store.ts, call-store.ts
- api.ts (JWT 인터셉터), query-client.ts
- use-auth.ts, use-agent-status.ts, use-queue.ts, use-video-call.ts

### ST-5: 페이지 + 컴포넌트 스텁 (→ ST-3, ST-4)
- login/page.tsx, dashboard/page.tsx, call/[id]/page.tsx, history/page.tsx
- queue-list.tsx, video-room.tsx, chat-panel.tsx, note-panel.tsx, tool-bar.tsx

## 6. 빌드 시퀀스

**Phase 1** (ST-1): scaffold → `pnpm dev --port 3100` 검증
**Phase 2** (ST-2): globals.css → `pnpm build` 검증
**Phase 3** (ST-3 + ST-4 병렬): 레이아웃 + 상태관리 → `pnpm build && pnpm lint` 검증
**Phase 4** (ST-5): 페이지 + 컴포넌트 → `pnpm build` 최종 검증

## 7. 주요 결정사항

1. **app-customer 패턴 따름**: 동일 deps, 동일 디자인 토큰, 동일 코딩 규약
2. **포트 3100**: 고객앱(3000)과 충돌 방지
3. **JWT localStorage**: 탭 닫아도 세션 유지 (sessionStorage 아님)
4. **사이드바 조건부**: /login, /call/[id]는 사이드바 없음 (풀스크린)
5. **.gitkeep 미사용**: 빈 디렉토리 미생성
6. **통화 사이드 패널**: 4탭 (채팅/메모/파일/문서) — 파일/문서는 placeholder
