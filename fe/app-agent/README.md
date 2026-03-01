# fe/app-agent — 상담사 대시보드 앱

상담사가 대기열을 관리하고 화상 상담을 수행하는 프론트엔드 앱.

## Tech Stack

- Next.js 16 + React 19 + TypeScript
- Tailwind CSS v4 (@theme 디자인 토큰)
- Zustand (상태관리) + TanStack Query (서버 상태)
- LiveKit Client (화상 통화 + 화면공유)
- Axios (API 클라이언트 + JWT 인터셉터)

## 실행

```bash
pnpm install
pnpm dev        # http://localhost:3100
```

## 빌드

```bash
pnpm build
pnpm lint
```

## 환경 변수

`.env.example` 참조:

```
NEXT_PUBLIC_API_URL=http://localhost:8080
NEXT_PUBLIC_LIVEKIT_URL=ws://localhost:7880
```

## 페이지

| 경로 | 레이아웃 | 설명 |
|---|---|---|
| `/login` | 풀스크린 | JWT 로그인 |
| `/dashboard` | 사이드바 | 대기열 + 상태 관리 |
| `/call/[id]` | 풀스크린 | 화상 통화 (채팅, 메모, 화면공유) |
| `/history` | 사이드바 | 상담 이력 |

## 상담사 상태

| 상태 | 색상 | 선택 가능 |
|---|---|---|
| Online | Green-500 | O |
| Away | Amber-500 | O |
| Wrap-up | Purple-500 | O |
| Offline | Gray-400 | X (로그인 전) |
| Busy | Red-500 | X (시스템 설정) |

## 디렉토리 구조

```
src/
├── app/           # 페이지 라우트
├── components/    # layout (sidebar), call (video, chat, note, toolbar), ui
├── hooks/         # use-auth, use-agent-status, use-queue, use-video-call
├── stores/        # auth-store (localStorage), call-store (sessionStorage)
└── lib/           # api (JWT 인터셉터), query-client
```
