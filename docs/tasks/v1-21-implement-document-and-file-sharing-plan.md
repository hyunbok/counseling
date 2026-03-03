# v1-21: Implement Document and File Sharing

## 1. Overview
File upload/download during counseling sessions. Both agent and customer can share files (PDF, images, docs). Metadata stored in PostgreSQL (write) + MongoDB (read). Files on local filesystem behind `FileStoragePort`. Real-time notifications via SSE (same Sinks pattern as chat).

**CQRS Flow**: Upload → validate → store file → save R2DBC → project to Mongo → emit SSE notification

## 2. API Design

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/channels/{channelId}/files` | permitAll | Upload file (multipart/form-data: file, senderType, senderId) → 201 |
| GET | `/api/channels/{channelId}/files?before=&limit=20` | permitAll | List files (cursor pagination) → 200 |
| GET | `/api/channels/{channelId}/files/{fileId}/download` | permitAll | Download file (streaming) → 200 |
| GET | `/api/channels/{channelId}/files/stream` | permitAll | SSE stream for new file events |
| DELETE | `/api/channels/{channelId}/files/{fileId}` | authenticated | Soft delete → 204 |

**Response DTO** — `SharedFileResponse`: `{id, channelId, uploaderId, uploaderType, originalFilename, contentType, fileSize, createdAt}`
**List DTO** — `SharedFileListResponse`: `{files: [...], hasMore: boolean, oldestTimestamp: Instant?}`

## 3. Data Model

### Domain: `SharedFile`
```
id: UUID, channelId: UUID, uploaderId: String, uploaderType: SenderType,
originalFilename: String, storedFilename: String, contentType: String,
fileSize: Long, storagePath: String, createdAt: Instant, deleted: Boolean
```

### PostgreSQL (V011): `shared_files`
```sql
id UUID PK, channel_id UUID FK→channels, uploader_id VARCHAR(100), uploader_type VARCHAR(20),
original_filename VARCHAR(255), stored_filename VARCHAR(255), content_type VARCHAR(100),
file_size BIGINT, storage_path TEXT, created_at TIMESTAMPTZ, deleted BOOLEAN DEFAULT FALSE
INDEX idx_shared_files_channel (channel_id, created_at DESC) WHERE NOT deleted
```

### MongoDB: `shared_files` collection
```
id, tenantId, channelId, uploaderId, uploaderType, originalFilename, contentType,
fileSize, createdAt, deleted
```
Indexes: `{tenantId:1, channelId:1, createdAt:-1}`

## 4. Implementation File List

### Phase 1: Domain + Ports (independent)
| File | Role |
|------|------|
| `domain/SharedFile.kt` | Domain model |
| `config/FileStorageProperties.kt` | `@ConfigurationProperties("file-storage")`: basePath, maxFileSize, allowedTypes |
| `port/inbound/SharedFileUseCase.kt` | `UploadFileCommand` + `SharedFileResource` + interface (upload, download, delete) |
| `port/inbound/SharedFileQuery.kt` | `SharedFileListResult` + interface (listFiles, streamFileEvents) |
| `port/outbound/SharedFileRepository.kt` | Write repo: save, findByIdAndNotDeleted |
| `port/outbound/SharedFileReadRepository.kt` | Read repo: save, findByChannelId, markDeleted |
| `port/outbound/FileStoragePort.kt` | store(path, content), load(path), delete(path) |
| `port/outbound/FileNotificationPort.kt` | emitFile, subscribeFiles, removeChannel |

### Phase 2: Adapters (depends on Phase 1)
| File | Role |
|------|------|
| `resources/db/tenant-migration/V011__create_shared_files.sql` | SQL migration |
| `adapter/outbound/persistence/SharedFileR2dbcRepository.kt` | PostgreSQL write via DatabaseClient |
| `adapter/outbound/persistence/SharedFileDocument.kt` | Mongo document with fromDomain/toDomain |
| `adapter/outbound/persistence/SharedFileMongoRepository.kt` | Mongo read repo |
| `adapter/outbound/external/InMemoryFileNotificationAdapter.kt` | Sinks-based SSE notification |
| `adapter/outbound/external/LocalFileStorageAdapter.kt` | Local filesystem storage |

### Phase 3: Services (depends on Phase 1+2)
| File | Role |
|------|------|
| `application/SharedFileService.kt` | Command: validate → store → R2DBC save → Mongo project → notify |
| `application/SharedFileQueryService.kt` | Query: list from Mongo, stream from notification port |

### Phase 4: Web Layer (depends on Phase 3)
| File | Role |
|------|------|
| `adapter/inbound/web/dto/SharedFileDtos.kt` | Response DTOs |
| `adapter/inbound/web/controller/SharedFileController.kt` | REST endpoints |

### Phase 5: Config Modifications
| File | Change |
|------|--------|
| `config/SecurityConfig.kt` | Add permitAll for file GET/POST paths |
| `resources/application.yml` | Add `file-storage` block + `spring.webflux.multipart` limits |

### Phase 6: Backend Tests
| File | Role |
|------|------|
| `test/.../application/SharedFileServiceTest.kt` | Unit tests for command service |
| `test/.../application/SharedFileQueryServiceTest.kt` | Unit tests for query service |

### Phase 7: Frontend — Agent App (`fe/app-agent`)
| File | Role |
|------|------|
| `src/hooks/use-files.ts` | TanStack Query: list query, upload mutation, SSE stream |
| `src/components/call/file-panel.tsx` | File list + upload UI with drag-and-drop |
| `src/app/call/[id]/page.tsx` (modify) | Replace 'file' tab placeholder with FilePanel |

### Phase 8: Frontend — Customer App (`fe/app-customer`)
| File | Role |
|------|------|
| `src/hooks/use-files.ts` | Same hook pattern for customer |
| `src/components/call/file-panel.tsx` | Customer file panel |
| `src/app/call/[id]/page.tsx` (modify) | Add file panel to customer call page |

## 5. Key Decisions
- **Soft delete**: Matches Recording/CounselNote pattern. Physical files cleaned later.
- **permitAll for upload/list/download**: Matches chat endpoint auth pattern. Only DELETE requires agent JWT.
- **Inline projection**: No domain events — save to Mongo inside service with `onErrorResume` (ChatService pattern).
- **FileStoragePort abstraction**: Swappable to S3/MinIO later without business logic changes.
- **storedFilename = UUID-based**: Prevents path traversal. originalFilename kept for display.
- **No storagePath in Mongo**: Read model never exposes storage internals.
- **SSE for real-time**: Same Sinks pattern as chat notifications for live file list updates.

## 6. Build Sequence
Phase 1 → Phase 2 → Phase 3 → Phase 4+5 → Phase 6 → Phase 7+8 (frontend parallel)

Backend subtasks can be split: Phase 1-5 (one agent), Phase 6 (tests). Frontend: agent + customer in parallel.
