# fe/app-customer — 고객용 화상 상담 앱

고객이 상담을 신청하고 화상 통화에 참여하는 프론트엔드 앱.

## Tech Stack

- Next.js 16 + React 19 + TypeScript
- Tailwind CSS v4 (@theme 디자인 토큰)
- Zustand (상태관리) + TanStack Query (서버 상태)
- LiveKit Client (화상 통화)
- Axios (API 클라이언트)

## 실행

```bash
pnpm install
pnpm dev        # http://localhost:3000
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

| 경로 | 설명 |
|---|---|
| `/` | 상담 신청 (이름, 연락처 입력) |
| `/waiting` | 대기열 (순번 표시) |
| `/call/[id]` | 화상 통화 |
| `/feedback` | 상담 후 피드백 (별점 + 코멘트) |

## 디렉토리 구조

```
src/
├── app/           # 페이지 라우트
├── components/    # UI 컴포넌트 (providers, button, theme-toggle)
├── hooks/         # use-queue, use-video-call
├── stores/        # customer-store (Zustand, sessionStorage)
└── lib/           # api, query-client
```
