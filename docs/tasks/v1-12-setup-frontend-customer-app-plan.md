# v1-12: Setup Frontend Customer App (fe/app-customer)

## 1. 개요

고객용 화상 상담 프론트엔드 앱. Next.js 15 + React 19 + TypeScript + Tailwind CSS.
4개 페이지: 상담 신청(Join) → 대기열(Waiting) → 화상 통화(Call) → 피드백(Feedback).
포트 3000, 단독 pnpm 프로젝트.

## 2. 디자인 시스템 (design-concept.md 기반)

- **Primary**: Indigo-600 `#4F46E5` / Dark: Indigo-400 `#818CF8`
- **Fonts**: Pretendard, Inter (sans), JetBrains Mono (mono)
- **Dark mode**: `darkMode: 'class'`, localStorage 퍼시스트
- **Component**: Card `rounded-xl shadow-sm p-6`, Button `rounded-lg px-4 py-2`
- **Icons**: Heroicons (Outline)

## 3. 파일 목록

```
fe/app-customer/
├── .editorconfig                    # indent 2, utf-8, lf
├── .env.example                     # NEXT_PUBLIC_API_URL, LIVEKIT_URL
├── .eslintrc.json                   # next + prettier
├── .prettierrc                      # singleQuote, semi, trailing comma
├── next.config.ts                   # output: standalone
├── tailwind.config.ts               # 디자인 토큰, darkMode: class
├── postcss.config.mjs               # tailwind + autoprefixer
├── tsconfig.json                    # strict, @/* alias
├── package.json
├── src/
│   ├── app/
│   │   ├── globals.css              # Tailwind directives, font imports
│   │   ├── layout.tsx               # Root layout, Providers, metadata
│   │   ├── page.tsx                 # "/" Join 페이지
│   │   ├── waiting/page.tsx         # "/waiting" 대기열
│   │   ├── call/[id]/page.tsx       # "/call/[id]" 화상 통화
│   │   └── feedback/page.tsx        # "/feedback" 피드백
│   ├── components/
│   │   ├── providers.tsx            # QueryClientProvider + ThemeProvider
│   │   ├── ui/button.tsx            # Button variant component
│   │   └── layout/theme-toggle.tsx  # Dark mode 토글
│   ├── hooks/
│   │   ├── use-queue.ts             # 대기열 enter/leave/polling
│   │   └── use-video-call.ts        # LiveKit room 연결
│   ├── stores/
│   │   └── customer-store.ts        # Zustand: 고객 상태
│   └── lib/
│       ├── api.ts                   # Axios instance
│       └── query-client.ts          # TanStack QueryClient
```

## 4. 서브태스크별 구현 파일

### ST-1: Next.js 프로젝트 초기화 (의존성 없음)
- `pnpm create next-app@latest` 실행
- scaffold 생성 파일: package.json, next.config.ts, tsconfig.json, tailwind.config.ts 등
- 보일러플레이트 정리 (기본 SVG, 샘플 CSS 제거)

### ST-2: 의존성 설치 (→ ST-1)
- Runtime: zustand, @tanstack/react-query, livekit-client, @livekit/components-react, axios, @heroicons/react
- Dev: prettier, eslint-config-prettier
- .eslintrc.json 수정 (prettier extend)

### ST-3: Tailwind CSS 디자인 시스템 (→ ST-1)
- tailwind.config.ts: colors, fontFamily, fontSize, darkMode, borderRadius
- globals.css: font imports (Pretendard CDN, Inter), base styles
- .editorconfig 생성

### ST-4: 폴더 구조 + 기반 설정 (→ ST-2, ST-3)
- .prettierrc, .env.example
- next.config.ts 수정 (standalone)
- src/lib/api.ts, src/lib/query-client.ts
- src/stores/customer-store.ts
- src/components/providers.tsx, ui/button.tsx, layout/theme-toggle.tsx
- src/hooks/use-queue.ts, use-video-call.ts

### ST-5: 페이지 스텁 생성 (→ ST-4)
- layout.tsx 교체 (Providers, fonts, metadata)
- page.tsx (Join), waiting/page.tsx, call/[id]/page.tsx, feedback/page.tsx

## 5. 환경 변수

```
NEXT_PUBLIC_API_URL=http://localhost:8080
NEXT_PUBLIC_LIVEKIT_URL=ws://localhost:7880
```

## 6. 빌드 시퀀스

**Phase 1** (ST-1): scaffold → `pnpm dev` 검증
**Phase 2** (ST-2 + ST-3 병렬): 의존성 + 디자인 시스템 → `pnpm build` 검증
**Phase 3** (ST-4): 기반 설정 + lib/store/hooks → `pnpm build && pnpm lint` 검증
**Phase 4** (ST-5): 페이지 스텁 → `pnpm build` 최종 검증

## 7. 주요 결정사항

1. **단독 pnpm 프로젝트**: pnpm workspace 미사용, 각 fe/ 앱 독립
2. **Tailwind v3 vs v4**: create-next-app 결과에 따라 대응 (v4면 @theme 사용)
3. **파일 명명**: kebab-case (use-queue.ts, customer-store.ts)
4. **언어**: `<html lang="ko">` — 한국어 플랫폼
5. **.gitkeep 미사용**: 빈 디렉토리 생성 안 함
6. **@heroicons/react 추가**: design-concept.md에 Heroicons 명시
