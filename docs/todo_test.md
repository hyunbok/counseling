# 테스트 현황

## 테스트 완료

### 상담사 앱 (app-agent, :3100)

| 기능 | 상태 | 비고 |
|---|---|---|
| 로그인 / 로그아웃 | 완료 | JWT 인증, zustand persist |
| 대시보드 | 완료 | 요약 통계, 대기열, 최근 상담 |
| 상태 변경 | 완료 | 온라인/자리비움/마무리 중 (사이드바) |
| 상담 이력 목록 | 완료 | 페이지네이션, 필터링 (상태/고객명/날짜/그룹) |
| 상담 이력 상세 | 완료 | 상담정보/고객정보/녹화/피드백/메모/채팅이력 |
| 프로필 — 이름 편집 | 완료 | 인라인 편집, BE API 연동 |
| 프로필 — 비밀번호 변경 | 완료 | 필드별 유효성 검사, 서버 에러 처리 |
| 다크모드 | 완료 | 사이드바 컨텍스트 메뉴에서 토글 |

### 어드민 앱 (app-admin, :3200)

| 기능 | 상태 | 비고 |
|---|---|---|
| 로그인 / 로그아웃 | 완료 | Super Admin / Company Admin |
| 테넌트 관리 | 완료 | CRUD, 검색, 페이지네이션 |
| 그룹 관리 | 완료 | CRUD, 검색, 페이지네이션 |
| 상담사 관리 | 완료 | CRUD, 검색, 페이지네이션 |
| 모니터링 | 완료 | 실시간 상담사/채널 상태 |
| 피드백 조회 | 완료 | 목록, 검색, 페이지네이션 |
| 대시보드 (통계) | 완료 | 요약 카드, 차트 |

### 고객 앱 (app-customer, :3001)

| 기능 | 상태 | 비고 |
|---|---|---|
| 상담 접수 (대기열 진입) | 완료 | 이름/연락처 입력 후 대기 |
| 대기 화면 | 완료 | 대기 순번 표시 |
| 피드백 제출 | 완료 | 별점 + 코멘트 |

---

## 미테스트 — 통화 중 기능

> 고객앱 대기열 진입 -> 상담사앱 수락 -> LiveKit 통화 연결 상태에서 테스트 필요

### 상담사 앱

| 기능 | FE | BE | 테스트 방법 |
|---|---|---|---|
| 채팅 | chat-panel.tsx | ChatController | 통화 중 채팅 패널에서 메시지 송수신 |
| 상담 메모 | note-panel.tsx | CounselNote API | 통화 중 메모 작성/수정/저장 |
| 녹화 | use-recording.ts | RecordingController | 통화 중 녹화 시작/중지, 이력에서 재생 |
| 파일 공유 | file-panel.tsx | SharedFileController | 통화 중 파일 업로드/다운로드 |
| 화면 캡처 | tool-bar.tsx | ScreenCaptureController | 통화 중 화면 캡처 버튼 |
| 코브라우징 | cobrowse-viewer.tsx | CoBrowsingController | 통화 중 고객 화면 공유 요청/수락/제어 |

### 고객 앱

| 기능 | FE | BE | 테스트 방법 |
|---|---|---|---|
| 채팅 | chat-panel.tsx | ChatController | 통화 중 고객측 채팅 |
| 파일 공유 | file-panel.tsx | SharedFileController | 통화 중 고객측 파일 전송 |
| 코브라우징 수락 | cobrowse-request-dialog.tsx | CoBrowsingController | 상담사 요청 시 수락/거부 다이얼로그 |

---

## 미구현

| 기능 | BE | FE | 비고 |
|---|---|---|---|
| 알림 | NotificationController (구현됨) | 미구현 | 상담사 앱에 알림 UI 필요 |

---

## 테스트 환경

| 서비스 | 포트 | 비고 |
|---|---|---|
| BE API | :8080 | 상담사/고객용 |
| BE Admin API | :8081 | 관리자용 |
| FE Admin | :3200 | 관리자 앱 |
| FE Customer | :3001 | 고객 앱 |
| FE Agent | :3100 | 상담사 앱 |
| PostgreSQL | :5432 | meta_db |
| MongoDB | :27017 | counseling_read |
| Redis | :6379 | 캐시/PubSub |
| LiveKit | :7880 | 화상 통화 서버 |

### 테스트 계정

| 앱 | 아이디 | 비밀번호 | 비고 |
|---|---|---|---|
| 어드민 (Super Admin) | admin | admin123! | super_admins 시드 |
| 상담사 | agent1 | agent123! | tenant agents 시드 |
