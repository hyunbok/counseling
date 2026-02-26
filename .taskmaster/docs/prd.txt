# PRD: 화상 상담 플랫폼

## 1. 개요

화상 상담 플랫폼. 상담사과 고객이 webRTC 통신을 통해 화상으로 상담을 하는 웹 서비스를 구축한다.

### 목표

- webRTC 를 통한 웹 기술을 통한 화상 상담
- 고객에게 쾌적한 상담 경험 제공
- 어드민 서비스를 통한 상담사 관리, 통계, 상담이력 관리, 고객 피드백 관리
- LiveKit(SFU) 기반 화상 상담 및 녹화
- 확장 가능한 아키텍쳐 기반위에 점진적으로 기능 추가

### 대상 사용자
| 사용자 유형 | 설명 |
|---|---|
| 고객(Customer) | 자신의 니즈를 상담받으려는 사람 |
| 상담사(Agent) | 고객의 니즈를 파악하고 상담하는 사람 |
| 회사 관리자(Company Admin) | 소속 회사 전체의 설정, 그룹, 상담사를 관리하는 사람 |
| 그룹 관리자(Group Admin) | 소속 그룹의 상담사 및 상담 현황을 관리하는 사람 |
| 슈퍼유저(Super Admin) | 플랫폼 전체를 관리하는 사람. 회사(테넌트) 관리 및 전체 데이터 조회 가능 |

---


## 2. 기술 스택

### 백엔드 (api/, api-adm/)

- **Kotlin 2.3** + **Spring Boot 4.0.2** + **Spring WebFlux (Reactive)**
- **JDK 25** toolchain, Kotlin JVM target **25**
- **Gradle (Kotlin DSL)** — `build.gradle.kts`
- **ktlint** (`ktlint_official` 스타일) — 클래스 본문 내 람다는 8칸 들여쓰기
- 각 백엔드 모듈(`api/`, `api-adm/`)은 **독립적인 빌드 환경** — 프로젝트 루트에 빌드 설정을 추가하지 않는다
- **테스트**
    - **TDD 방식**: 테스트를 먼저 작성하고 구현 코드를 작성한다
    - **JUnit 5**: Spring 통합 테스트 (`@WebFluxTest`, `@SpringBootTest` 등)
    - **Kotest**: 순수 단위 테스트 (Kotest SpringExtension은 Spring Boot 4와 호환되지 않음)
    - 에러 처리: Spring Boot 기본 에러 응답 구조 사용

### 프론트엔드 (app-agent/, app-customer/, app-admin/)

- **React** + **Next.js**
- **pnpm** 패키지 매니저
- **Tailwind CSS** 스타일링
- **Zustand**: 클라이언트 상태 관리
- **TanStack Query**: 서버 상태 관리 (캐싱, 리페치)
- **ESLint + Prettier** 린팅/포매팅

---


## 3. 시스템 아키텍쳐

### 서비스 구성

| 서비스 | 역할 | 기술 스택 |
|--------|------|-----------|
| `be/api/` | 공개 API (상담사·고객용) | Kotlin, Spring Boot 4, WebFlux |
| `be/api-admin/` | 어드민 전용 API | Kotlin, Spring Boot 4, WebFlux |
| `livekit/` | SFU 미디어 서버 (시그널링·녹화 내장) | LiveKit (Go, Docker) |
| `fe/app-agent/` | 프론트엔드 (상담사) | Next.js, React, Tailwind CSS |
| `fe/app-customer/` | 프론트엔드 (고객) | Next.js, React, Tailwind CSS |
| `fe/app-admin/` | 프론트엔드 (어드민) | Next.js, React, Tailwind CSS |

### 핵심 설계 원칙

- **CQRS**: Command(쓰기)는 JPA + PostgreSQL, Query(읽기)는 MongoDB
- **도메인 주도 개발(DDD)**
- **헥사고널 아키텍처**: `domain/` → `repository/` → `service/` → `controller/`
- **Reactive**: Spring WebFlux 기반 비동기 처리
- **멀티테넌트 (DB per Tenant)**: 회사(테넌트)마다 별도 DB 인스턴스를 할당하여 데이터 완전 격리. Meta DB(공통)에서 테넌트 목록, DB 연결 정보, 슈퍼유저 계정을 관리하고 테넌트별 DB로 동적 라우팅
- **로컬 개발 환경**: DB(PostgreSQL, MongoDB)만 Docker, 나머지는 로컬 실행
- `docker-compose.yml`로 DB 컨테이너 관리

---


## 4. 기능 요구사항

### 4.1 인증/계정

> **인증 방식**: JWT(Access Token + Refresh Token) + Redis (토큰 블랙리스트, 세션 상태 관리)
> **관리자 로그인**: 단일 로그인 후 계정에 부여된 역할(회사 관리자/그룹 관리자/슈퍼유저)에 따라 권한 분기

| ID | 기능 | 설명 | 사용자 |
|----|------|------|--------|
| AUTH-1 | 상담사 로그인 | 계정(username)/비밀번호 기반 로그인 | 상담사 |
| AUTH-2 | 고객 접속 | 상담 링크를 통한 비회원 접속 (이름, 연락처 입력) | 고객 |
| AUTH-3 | 관리자 로그인 | 계정(username)/비밀번호 기반 단일 로그인. 역할에 따라 권한 분기 | 회사 관리자, 그룹 관리자, 슈퍼유저 |
| AUTH-4 | 비밀번호 변경 | 로그인 후 본인 비밀번호 변경 | 상담사, 회사 관리자, 그룹 관리자 |
| AUTH-5 | 비밀번호 초기화 | 관리자가 소속 사용자의 비밀번호를 리셋 | 회사 관리자, 슈퍼유저 |

### 4.2 상담 세션

> **Channel**: 상담이 이루어지는 방(Room). 하나의 상담 세션 단위.
> **Endpoint**: Channel에 접속한 개별 참여자(상담사/고객)의 연결 단위.
> **동시 상담**: 상담사는 한 번에 하나의 채널만 진행 가능 (1:1)

#### 상담사 상태

| 상태 | 설명 |
|------|------|
| 온라인(Online) | 상담 가능 상태. 대기열에서 고객 배정 가능 |
| 오프라인(Offline) | 로그아웃 또는 미접속 상태 |
| 상담중(Busy) | 현재 상담 세션 진행 중 |
| 자리비움(Away) | 일시적으로 자리를 비운 상태. 새 상담 배정 불가 |
| 후처리중(Wrap-up) | 상담 종료 후 메모 작성 등 후처리 진행 중. 새 상담 배정 불가 |

#### Channel

| ID | 기능 | 설명 | 사용자 |
|----|------|------|--------|
| CH-1 | 채널 생성 | 상담사가 상담을 수락하면 채널(상담방) 생성 | 시스템 |
| CH-2 | 채널 상태 관리 | 채널 상태 관리 (대기 → 진행 → 종료) | 시스템 |
| CH-3 | 채널 종료 | 상담 종료 시 채널을 닫고 리소스 정리 | 상담사 |

#### Endpoint

| ID | 기능 | 설명 | 사용자 |
|----|------|------|--------|
| EP-1 | 상담 대기열 | 고객이 접속하면 대기열에 등록되고, 순서대로 상담사와 연결 | 고객 |
| EP-2 | 대기열 이탈 | 고객이 대기 중 상담 요청을 취소하고 대기열에서 제거 | 고객 |
| EP-3 | 상담 수락 | 대기 중인 고객을 확인하고 상담을 수락하여 채널에 참여 | 상담사 |
| EP-4 | 참여자 입장 | Endpoint가 채널에 연결되어 미디어 스트림 시작 | 상담사, 고객 |
| EP-5 | 참여자 퇴장 | Endpoint가 채널에서 연결 해제 | 상담사, 고객 |

### 4.3 화상 통화 (WebRTC)

| ID | 기능 | 설명 | 사용자 |
|----|------|------|--------|
| RTC-1 | 영상/음성 통화 | WebRTC 기반 실시간 영상·음성 통화 | 상담사, 고객 |
| RTC-2 | 카메라/마이크 제어 | 카메라·마이크 ON/OFF 토글 | 상담사, 고객 |
| RTC-3 | 상담 녹화 | LiveKit Egress를 통한 상담 세션 녹화 및 저장 | 시스템 |
| RTC-4 | 디바이스 선택 | 사용할 카메라·마이크·스피커 장치 선택 | 상담사, 고객 |
| RTC-5 | 문서 공유 | 상담 중 문서(PDF 등)를 고객에게 화면 공유 | 상담사 |
| RTC-6 | 화면 캡처 | 상담 화면을 캡처하여 저장 | 상담사 |
| RTC-7 | 파일 전송 | 상담 중 고객에게 파일 전송 | 상담사 |
| RTC-8 | Co-Browsing | 고객의 브라우저 화면을 함께 보며 안내 | 상담사, 고객 |
| RTC-9 | 실시간 채팅 | 상담 중 텍스트 기반 실시간 채팅 | 상담사, 고객 |
| RTC-10 | 화면 공유 | 상담사 데스크톱/윈도우 화면을 고객에게 실시간 공유 | 상담사 |
| RTC-11 | 네트워크 재연결 | 네트워크 불안정 시 자동 재연결 시도 및 연결 상태 표시 | 시스템 |

### 4.4 상담 이력

| ID | 기능 | 설명 | 사용자 |
|----|------|------|--------|
| HIST-1 | 상담 이력 조회 | 과거 상담 세션 목록 및 상세 정보 조회 | 상담사, 회사 관리자, 그룹 관리자 |
| HIST-2 | 녹화 영상 재생 | 저장된 상담 녹화 영상 재생 | 상담사, 회사 관리자, 그룹 관리자 |
| HIST-3 | 채팅 이력 조회 | 상담 세션별 채팅 메시지 이력 조회 | 상담사, 회사 관리자, 그룹 관리자 |
| HIST-4 | 상담 메모 | 상담 중 또는 상담 후 메모 작성 | 상담사 |

### 4.5 고객 피드백

| ID | 기능 | 설명 | 사용자 |
|----|------|------|--------|
| FB-1 | 상담 만족도 평가 | 상담 종료 후 만족도 평점(별점) 및 코멘트 제출 | 고객 |
| FB-2 | 피드백 조회 | 상담별 고객 피드백 조회 | 상담사, 회사 관리자, 그룹 관리자 |

### 4.6 어드민

| ID | 기능 | 설명 | 사용자 |
|----|------|------|--------|
| ADM-1 | 회사 관리 (플랫폼) | 회사(테넌트) 등록, 비활성화 및 DB 연결 관리 | 슈퍼유저 |
| ADM-1-1 | 회사 정보 수정 | 소속 회사의 일반 정보(이름, 연락처, 주소 등) 수정 | 회사 관리자 |
| ADM-2 | 그룹 관리 | 상담 그룹 생성, 수정, 삭제 및 상담사 배정 | 회사 관리자 |
| ADM-3 | 상담사 관리 | 상담사 등록, 수정, 비활성화 | 회사 관리자, 그룹 관리자 |
| ADM-4 | 상담 통계 | 상담 건수, 평균 상담 시간, 만족도 등 통계 대시보드 | 회사 관리자, 그룹 관리자 |
| ADM-5 | 상담 모니터링 | 현재 진행 중인 상담 세션 현황 조회 | 회사 관리자, 그룹 관리자 |

---


## 5. 비기능 요구사항

### 5.1 성능

| 항목 | 요구사항 |
|------|----------|
| 동시 상담 | 50~500건 동시 상담 처리 (중규모) |
| 영상 지연 | WebRTC 미디어 지연 500ms 이하 |
| API 응답 | 일반 API 응답 시간 200ms 이하 |
| 대기열 | 실시간 대기열 상태 업데이트 1초 이내 |

### 5.2 브라우저 호환성

| 환경 | 지원 브라우저 |
|------|--------------|
| 데스크톱 | Chrome, Firefox, Safari, Edge (최신 2개 버전) |
| 모바일 | iOS Safari, Android Chrome (최신 2개 버전) |

### 5.3 보안

| 항목 | 요구사항 |
|------|----------|
| 통신 암호화 | HTTPS (TLS 1.2+) 적용 |
| 인증 | JWT + Redis 기반 토큰 관리 |
| 입력값 검증 | 모든 API 입력값 서버 측 검증 (XSS, SQL Injection 방지) |
| 비밀번호 | bcrypt 등 단방향 해시 저장 |

### 5.4 가용성

| 항목 | 요구사항 |
|------|----------|
| 서비스 가용성 | 99.5% 이상 |
| 장애 복구 | 서비스 장애 시 30분 이내 복구 목표 |
| 네트워크 끊김 | WebRTC 자동 재연결 (최대 3회 시도) |

---


## 6. 녹화 저장소

| 항목 | 설명 |
|------|------|
| 저장소 | 로컬 디스크 또는 NAS |
| 파일 형식 | WebM 또는 MP4 (SFU 미디어 서버 출력 포맷에 따라 결정) |
| 저장 경로 | `/{tenant_id}/{channel_id}/{timestamp}.{ext}` |
| 보관 정책 | 테넌트별 보관 기간 설정 가능. 기간 만료 시 자동 삭제 |
| 접근 권한 | 상담사(본인 상담), 회사 관리자, 그룹 관리자(소속 그룹)만 재생 가능 |

---


## 7. 알림

> **알림 채널**: 인앱 실시간 알림 (WebSocket) + 이메일

| ID | 알림 | 수신자 | 채널 |
|----|------|--------|------|
| NTF-1 | 상담 요청 도착 | 상담사 | 인앱 |
| NTF-2 | 대기열 순서 변경 | 고객 | 인앱 |
| NTF-3 | 상담 연결 완료 | 고객 | 인앱 |
| NTF-4 | 고객 피드백 등록 | 상담사 | 인앱 |
| NTF-5 | 녹화 파일 보관 만료 예정 | 회사 관리자 | 이메일 |
| NTF-6 | 상담사 계정 생성/비밀번호 초기화 | 상담사 | 이메일 |

---


## 8. 데이터 모델

> CQRS 구조: Command(쓰기)는 PostgreSQL, Query(읽기)는 MongoDB
> Meta DB(공통)와 Tenant DB(회사별)로 분리

### Meta DB (공통)

#### Tenant (회사)
| 필드 | 설명 |
|------|------|
| id | 테넌트 고유 ID |
| name | 회사명 |
| status | 상태 (활성/비활성) |
| db_host, db_port, db_name | 테넌트 DB 연결 정보 |
| created_at, updated_at | 생성/수정 일시 |

#### SuperAdmin (슈퍼유저)
| 필드 | 설명 |
|------|------|
| id | 슈퍼유저 고유 ID |
| username | 계정 |
| password_hash | 비밀번호 해시 |

### Tenant DB (회사별)

#### Company (회사 정보)
| 필드 | 설명 |
|------|------|
| id | 회사 고유 ID |
| name | 회사명 |
| contact, address | 연락처, 주소 |
| updated_at | 수정 일시 |

#### Group (상담 그룹)
| 필드 | 설명 |
|------|------|
| id | 그룹 고유 ID |
| name | 그룹명 |
| status | 상태 (활성/비활성) |

#### User (상담사/관리자)
| 필드 | 설명 |
|------|------|
| id | 사용자 고유 ID |
| username | 계정 |
| password_hash | 비밀번호 해시 |
| name | 이름 |
| role | 역할 (AGENT, COMPANY_ADMIN, GROUP_ADMIN) |
| group_id | 소속 그룹 (FK → Group) |
| agent_status | 상담사 상태 (온라인/오프라인/상담중/자리비움/후처리중) |
| status | 계정 상태 (활성/비활성) |

#### Channel (상담 채널)
| 필드 | 설명 |
|------|------|
| id | 채널 고유 ID |
| agent_id | 상담사 (FK → User) |
| status | 채널 상태 (대기/진행/종료) |
| started_at, ended_at | 시작/종료 일시 |
| recording_path | 녹화 파일 경로 |

#### Endpoint (참여자)
| 필드 | 설명 |
|------|------|
| id | 엔드포인트 고유 ID |
| channel_id | 소속 채널 (FK → Channel) |
| type | 참여자 유형 (AGENT/CUSTOMER) |
| customer_name | 고객 이름 (고객인 경우) |
| customer_contact | 고객 연락처 (고객인 경우) |
| joined_at, left_at | 입장/퇴장 일시 |

#### ChatMessage (채팅 메시지)
| 필드 | 설명 |
|------|------|
| id | 메시지 고유 ID |
| channel_id | 소속 채널 (FK → Channel) |
| sender_type | 발신자 유형 (AGENT/CUSTOMER) |
| sender_id | 발신자 ID |
| content | 메시지 내용 |
| created_at | 전송 일시 |

#### CounselNote (상담 메모)
| 필드 | 설명 |
|------|------|
| id | 메모 고유 ID |
| channel_id | 소속 채널 (FK → Channel) |
| agent_id | 작성 상담사 (FK → User) |
| content | 메모 내용 |
| created_at, updated_at | 생성/수정 일시 |

#### Feedback (고객 피드백)
| 필드 | 설명 |
|------|------|
| id | 피드백 고유 ID |
| channel_id | 소속 채널 (FK → Channel) |
| rating | 만족도 평점 (1~5) |
| comment | 코멘트 |
| created_at | 작성 일시 |

---


## 9. API 설계

### 9.1 공개 API (`be/api/`)

#### 인증

| Method | Path | 설명 |
|--------|------|------|
| POST | `/api/auth/login` | 상담사 로그인 |
| POST | `/api/auth/logout` | 로그아웃 |
| POST | `/api/auth/refresh` | 토큰 갱신 |
| PUT | `/api/auth/password` | 비밀번호 변경 |

#### 고객 접속

| Method | Path | 설명 |
|--------|------|------|
| POST | `/api/customer/join` | 고객 비회원 접속 (이름, 연락처) |

#### 상담 세션 — Channel

| Method | Path | 설명 |
|--------|------|------|
| GET | `/api/channels` | 상담사 본인 채널 목록 조회 |
| GET | `/api/channels/{id}` | 채널 상세 조회 |
| POST | `/api/channels/{id}/close` | 채널 종료 |

#### 상담 세션 — Endpoint / 대기열

| Method | Path | 설명 |
|--------|------|------|
| POST | `/api/queue/enter` | 고객 대기열 등록 |
| DELETE | `/api/queue/leave` | 고객 대기열 이탈 |
| GET | `/api/queue` | 대기열 목록 조회 (상담사) |
| POST | `/api/queue/{id}/accept` | 대기 고객 상담 수락 (상담사) |

#### 상담사 상태

| Method | Path | 설명 |
|--------|------|------|
| GET | `/api/agents/me/status` | 본인 상태 조회 |
| PUT | `/api/agents/me/status` | 본인 상태 변경 |

#### 화상 통화

| Method | Path | 설명 |
|--------|------|------|
| POST | `/api/channels/{id}/chat` | 채팅 메시지 전송 |
| GET | `/api/channels/{id}/chat` | 채팅 이력 조회 |
| POST | `/api/channels/{id}/files` | 파일 전송 (상담사) |
| POST | `/api/channels/{id}/capture` | 화면 캡처 저장 (상담사) |

#### 상담 이력

| Method | Path | 설명 |
|--------|------|------|
| GET | `/api/history` | 상담 이력 목록 조회 |
| GET | `/api/history/{channelId}` | 상담 이력 상세 조회 |
| GET | `/api/history/{channelId}/recording` | 녹화 영상 스트리밍 |
| GET | `/api/history/{channelId}/chat` | 채팅 이력 조회 |

#### 상담 메모

| Method | Path | 설명 |
|--------|------|------|
| POST | `/api/channels/{id}/notes` | 메모 작성 |
| GET | `/api/channels/{id}/notes` | 메모 조회 |
| PUT | `/api/notes/{id}` | 메모 수정 |

#### 고객 피드백

| Method | Path | 설명 |
|--------|------|------|
| POST | `/api/channels/{id}/feedback` | 만족도 평가 제출 (고객) |
| GET | `/api/channels/{id}/feedback` | 피드백 조회 |

### 9.2 어드민 API (`be/api-admin/`)

#### 인증

| Method | Path | 설명 |
|--------|------|------|
| POST | `/api-adm/auth/login` | 관리자/슈퍼유저 로그인 |
| POST | `/api-adm/auth/logout` | 로그아웃 |
| POST | `/api-adm/auth/refresh` | 토큰 갱신 |

#### 회사 관리 (슈퍼유저)

| Method | Path | 설명 |
|--------|------|------|
| GET | `/api-adm/tenants` | 테넌트 목록 조회 |
| POST | `/api-adm/tenants` | 테넌트 등록 |
| PUT | `/api-adm/tenants/{id}` | 테넌트 수정 |
| PATCH | `/api-adm/tenants/{id}/status` | 테넌트 활성/비활성 |

#### 회사 정보 (회사 관리자)

| Method | Path | 설명 |
|--------|------|------|
| GET | `/api-adm/company` | 소속 회사 정보 조회 |
| PUT | `/api-adm/company` | 소속 회사 정보 수정 |

#### 그룹 관리

| Method | Path | 설명 |
|--------|------|------|
| GET | `/api-adm/groups` | 그룹 목록 조회 |
| POST | `/api-adm/groups` | 그룹 생성 |
| PUT | `/api-adm/groups/{id}` | 그룹 수정 |
| DELETE | `/api-adm/groups/{id}` | 그룹 삭제 |

#### 상담사 관리

| Method | Path | 설명 |
|--------|------|------|
| GET | `/api-adm/agents` | 상담사 목록 조회 |
| POST | `/api-adm/agents` | 상담사 등록 |
| PUT | `/api-adm/agents/{id}` | 상담사 수정 |
| PATCH | `/api-adm/agents/{id}/status` | 상담사 활성/비활성 |
| POST | `/api-adm/agents/{id}/reset-password` | 비밀번호 초기화 |

#### 통계 / 모니터링

| Method | Path | 설명 |
|--------|------|------|
| GET | `/api-adm/stats/summary` | 상담 통계 요약 (건수, 평균 시간, 만족도) |
| GET | `/api-adm/stats/agents` | 상담사별 통계 |
| GET | `/api-adm/monitoring/channels` | 현재 진행 중인 채널 현황 |
| GET | `/api-adm/monitoring/agents` | 상담사 상태 현황 |

#### 피드백

| Method | Path | 설명 |
|--------|------|------|
| GET | `/api-adm/feedbacks` | 피드백 목록 조회 |
| GET | `/api-adm/feedbacks/{id}` | 피드백 상세 조회 |

---


