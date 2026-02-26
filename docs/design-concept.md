# 디자인 컨셉 — 화상 상담 플랫폼

## 1. 디자인 원칙

| 원칙 | 설명 |
|------|------|
| **모던 SaaS** | 깨끗한 여백, 라운드 UI, 소프트 그림자 (Notion / Linear 참고) |
| **일관성** | 3개 앱(고객·상담사·어드민) 전체에서 동일한 컬러·타이포·컴포넌트 시스템 사용 |
| **접근성** | WCAG 2.1 AA 대비비 준수, 키보드 내비게이션 지원 |
| **반응형** | 데스크톱 → 태블릿 → 모바일 브레이크포인트 대응 |
| **다크 모드** | 라이트/다크 모드 토글 지원 (`prefers-color-scheme` 연동) |

---

## 2. 컬러 팔레트

### 브랜드 컬러

| 토큰 | Light | Dark |
|------|-------|------|
| Primary | Indigo-600 `#4F46E5` | Indigo-400 `#818CF8` |
| Primary Hover | Indigo-700 `#4338CA` | Indigo-300 `#A5B4FC` |

### 배경

| 토큰 | Light | Dark |
|------|-------|------|
| BG Base | White `#FFFFFF` | Gray-900 `#111827` |
| BG Surface | Gray-50 `#F9FAFB` | Gray-800 `#1F2937` |
| BG Elevated | White `#FFFFFF` | Gray-700 `#374151` |

### 텍스트

| 토큰 | Light | Dark |
|------|-------|------|
| Text Primary | Gray-900 `#111827` | Gray-100 `#F3F4F6` |
| Text Secondary | Gray-700 `#374151` | Gray-300 `#D1D5DB` |
| Text Tertiary | Gray-500 `#6B7280` | Gray-400 `#9CA3AF` |

### 시맨틱

| 토큰 | 컬러 | 용도 |
|------|------|------|
| Success | Green-500 / Green-400 | 성공, 온라인 |
| Warning | Amber-500 / Amber-400 | 경고, 자리비움 |
| Error | Red-500 / Red-400 | 에러, 오프라인 |
| Info | Blue-500 / Blue-400 | 정보 |

### 상담사 상태 컬러

| 상태 | 컬러 | Tailwind 클래스 |
|------|------|-----------------|
| Online | Green-500 | `bg-green-500` |
| Offline | Gray-400 | `bg-gray-400` |
| Busy | Red-500 | `bg-red-500` |
| Away | Amber-500 | `bg-amber-500` |
| Wrap-up | Purple-500 | `bg-purple-500` |

---

## 3. 타이포그래피

### 폰트 패밀리

- **한글**: Pretendard
- **영문/숫자**: Inter
- **코드/모노스페이스**: JetBrains Mono

```css
font-family: 'Pretendard', 'Inter', -apple-system, BlinkMacSystemFont, system-ui, sans-serif;
```

### 스케일

| 토큰 | 크기 | 무게 | 용도 |
|------|------|------|------|
| Display | 30px / 1.875rem | Bold (700) | 페이지 타이틀 |
| Heading L | 24px / 1.5rem | SemiBold (600) | 섹션 헤더 |
| Heading M | 20px / 1.25rem | SemiBold (600) | 카드 타이틀 |
| Body L | 16px / 1rem | Regular (400) | 본문 |
| Body M | 14px / 0.875rem | Regular (400) | 보조 텍스트 |
| Caption | 12px / 0.75rem | Medium (500) | 라벨, 배지 |

---

## 4. 컴포넌트 스타일

### 카드

```
border-radius: 12px (rounded-xl)
shadow: shadow-sm (light) / shadow-none + border (dark)
padding: 24px (p-6)
```

### 버튼

| 종류 | 스타일 |
|------|--------|
| Primary | `bg-indigo-600 text-white rounded-lg px-4 py-2 hover:bg-indigo-700` |
| Secondary | `bg-white border border-gray-300 text-gray-700 rounded-lg px-4 py-2` |
| Ghost | `text-gray-500 hover:text-gray-700 hover:bg-gray-100 rounded-lg px-3 py-2` |
| Danger | `bg-red-600 text-white rounded-lg px-4 py-2 hover:bg-red-700` |

### 입력 필드

```
rounded-lg border border-gray-300 px-3 py-2
focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500
dark:bg-gray-700 dark:border-gray-600
```

### 테이블

```
- 헤더: bg-gray-50 dark:bg-gray-700 text-sm font-medium
- 행: hover:bg-gray-50 dark:hover:bg-gray-700/50
- 구분선: divide-y divide-gray-200 dark:divide-gray-700
```

### 배지/상태

```
inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium
```

---

## 5. 레이아웃 패턴

### 고객 앱

- **단일 컬럼 중앙 정렬** — 접속, 대기, 피드백 화면
- **풀스크린** — 화상 상담 화면 (영상 + 사이드 채팅 패널)

### 상담사 앱

- **사이드바 + 메인** — 대시보드, 상담 이력
- **풀스크린** — 화상 상담 화면 (영상 + 멀티탭 사이드 패널)

### 어드민 앱

- **사이드바 + 메인** — 모든 화면
- 사이드바: 로고, 내비게이션 메뉴, 사용자 정보

---

## 6. 아이콘

- **Heroicons** (Outline 스타일, SVG inline)
- 크기: 20×20 (메뉴), 24×24 (액션), 16×16 (인라인)

---

## 7. 다크 모드 전환

- Tailwind `dark:` 클래스 기반
- `<html class="dark">` 토글 방식
- 우측 상단 토글 버튼 (Sun / Moon 아이콘)
- `localStorage` 에 저장, `prefers-color-scheme` 미디어 쿼리 초기값 연동

---

## 8. 목업 파일 목록

| 파일 | 설명 |
|------|------|
| `mockups/customer-join.html` | 고객: 접속 (이름/연락처 입력) |
| `mockups/customer-waiting.html` | 고객: 대기열 |
| `mockups/customer-call.html` | 고객: 화상 상담 |
| `mockups/customer-feedback.html` | 고객: 만족도 평가 |
| `mockups/agent-login.html` | 상담사: 로그인 |
| `mockups/agent-dashboard.html` | 상담사: 대시보드 |
| `mockups/agent-call.html` | 상담사: 화상 상담 |
| `mockups/agent-history.html` | 상담사: 상담 이력 |
| `mockups/admin-login.html` | 어드민: 로그인 |
| `mockups/admin-dashboard.html` | 어드민: 대시보드 |
| `mockups/admin-agents.html` | 어드민: 상담사 관리 |
| `mockups/admin-groups.html` | 어드민: 그룹 관리 |
| `mockups/admin-tenants.html` | 어드민: 테넌트 관리 |
| `mockups/admin-feedbacks.html` | 어드민: 피드백 목록 |
