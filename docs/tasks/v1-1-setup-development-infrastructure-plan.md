# v1-1: Setup Development Infrastructure with Docker

## 1. Overview

Docker Compose 기반 로컬 개발 인프라 구성. 화상 상담 플랫폼에 필요한 5개 서비스:
- **PostgreSQL 16 x2**: Meta DB (port 5432) + Tenant DB (port 5433) — CQRS write-side
- **MongoDB 7**: CQRS read-side 쿼리 모델 (port 27017)
- **Redis 7-alpine**: JWT 토큰 블랙리스트, 세션 관리 (port 6379)
- **LiveKit Server**: WebRTC SFU (ports 7880/7881/7882-udp)

CQRS 흐름: Command → PostgreSQL (write) → Event → MongoDB (read projection)

## 2. 파일 목록

| File | Action | Subtask |
|------|--------|---------|
| `docker-compose.yml` | CREATE | 1, 2, 3, 5 |
| `.env.example` | POPULATE | 4 |
| `.gitignore` | MODIFY | 5 |
| `storage/recordings/.gitkeep` | CREATE | 5 |
| `storage/files/.gitkeep` | CREATE | 5 |
| `storage/captures/.gitkeep` | CREATE | 5 |

## 3. docker-compose.yml 설계

- **Format**: Compose V2 (`name: counseling`)
- **Network**: `counseling-network` (bridge)
- **Profiles**: `db` (PostgreSQL+MongoDB+Redis), `livekit` (LiveKit only), `all` (전체)
- **Health checks**: 모든 서비스에 적용 (pg_isready, mongosh, redis-cli, curl)
- **Restart policy**: `unless-stopped`
- **Named volumes**: `counseling_` prefix로 충돌 방지

### Services

| Service | Image | Port | Container Name | Volume |
|---------|-------|------|----------------|--------|
| postgres-meta | postgres:16 | 5432:5432 | counseling-postgres-meta | postgres_meta_data |
| postgres-tenant | postgres:16 | 5433:5432 | counseling-postgres-tenant | postgres_tenant_data |
| mongodb | mongo:7 | 27017:27017 | counseling-mongodb | mongodb_data |
| redis | redis:7-alpine | 6379:6379 | counseling-redis | redis_data |
| livekit | livekit/livekit-server:latest | 7880,7881,7882/udp | counseling-livekit | - |

## 4. .env.example 설계

- PostgreSQL Meta/Tenant: host, port, name, user, password, R2DBC URL
- MongoDB: URI (`mongodb://localhost:27017/counseling_read`)
- Redis: host, port, URL
- LiveKit: URL, API key/secret (devkey/secret)
- JWT: secret, access/refresh expiration (ms)
- Server: API port 8080, admin port 8081
- Storage: recordings, files, captures paths

## 5. 주요 결정사항

1. **R2DBC URL**: Spring WebFlux 기반이므로 `r2dbc:postgresql://` 스키마 사용
2. **LiveKit dev mode**: `--dev` 플래그로 간소화된 인증 (devkey:secret)
3. **Redis AOF**: `--appendonly yes`로 컨테이너 재시작 시 데이터 보존
4. **Profile 분리**: DB만 필요한 백엔드 개발 vs 전체 스택 구분
5. **Storage .gitkeep**: 디렉토리 구조 유지하되 런타임 파일은 gitignore

## 6. Build Sequence

**Phase 1** (Subtask 1+2+3): docker-compose.yml 전체 생성 (동일 파일 순차 수정)
**Phase 2** (Subtask 4): .env.example 작성
**Phase 3** (Subtask 5): .gitignore 수정, storage 디렉토리 생성, 최종 검증
