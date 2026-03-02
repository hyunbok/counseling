# v1-17: Implement Admin Dashboard Features — Design Plan

## 1. Overview

Wire the existing admin frontend (fe/app-admin) placeholder pages to real backend APIs (be/api-admin).
The admin app already has fully built UI pages with mock data. The backend has all controllers but
response DTOs don't match frontend expectations and list endpoints lack pagination.

**Scope**: Full-stack (be/api-admin + fe/app-admin)
**Dependencies**: Task 11 (Admin API - done), Task 16 (Admin App Setup - done)

## 2. Gap Analysis Summary

| Area | Gap | Fix |
|------|-----|-----|
| Pagination | BE returns `Flux<T>`, FE expects `PageResponse<T>` | Add pagination params to BE controllers |
| Agent DTO | BE has no `groupName`, `email`; FE has no `role`, `agentStatus` | Enrich BE AgentResponse, align FE type |
| Group DTO | BE has no `agentCount`, `tenantName` | Add agentCount to BE GroupResponse |
| Tenant DTO | FE has `domain`/`plan`, BE has `slug`/`status` | Align FE type to match BE |
| Monitoring | FE calls `/sessions`, BE has `/channels` + `/agents` | Adapt FE to call real endpoints |
| Feedback DTO | BE has no `agentName`/`clientName` | Keep minimal, show channelId instead |
| DELETE endpoints | Missing for tenants and agents | Use soft-delete (status change) on FE |
| Mock data | FE agents/groups pages use MOCK_TENANTS/MOCK_GROUPS | Replace with real hook data |
| Stats | No FE stats hook | Create `use-stats.ts` hook, wire to `/api-adm/stats/*` |
| Agent creation | FE sends password, BE auto-generates | Show temp password from response |

## 3. API Endpoints (BE Changes Required)

### 3.1 Add Pagination Parameters to List Endpoints

All list endpoints get `page` (default 0), `size` (default 20) query params and return `PageResponse<T>`:

```
GET /api-adm/tenants?page=0&size=20          → PageResponse<TenantSummaryResponse>
GET /api-adm/groups?page=0&size=20           → PageResponse<GroupResponse>
GET /api-adm/agents?page=0&size=20           → PageResponse<AgentResponse>
GET /api-adm/feedbacks?page=0&size=20&rating=&agentId=  → PageResponse<FeedbackResponse>
```

### 3.2 Enrich DTOs

**AgentResponse** — add `groupName: String?` (join with Group)
**GroupResponse** — add `agentCount: Int` (count from Agent table)

### 3.3 No New DELETE Endpoints

Use existing `PATCH /agents/{id}/status` (deactivate) and `PATCH /tenants/{id}/status` instead.

## 4. Data Model (No Schema Changes)

Existing domain models and DB schemas are sufficient. Changes are DTO-level only.

## 5. Implementation File List

### Subtask 1: BE — Add Pagination to List Controllers
**Files to modify:**
- `be/api-admin/.../adapter/inbound/web/controller/TenantController.kt` — add page/size params
- `be/api-admin/.../adapter/inbound/web/controller/GroupController.kt` — add page/size params
- `be/api-admin/.../adapter/inbound/web/controller/AgentController.kt` — add page/size params
- `be/api-admin/.../adapter/inbound/web/controller/FeedbackController.kt` — add page/size params
- `be/api-admin/.../port/inbound/*Query.kt` — add pagination params to query interfaces
- `be/api-admin/.../application/*QueryService.kt` — implement pagination
- `be/api-admin/.../port/outbound/*Repository.kt` — add paginated query methods
- `be/api-admin/.../adapter/outbound/persistence/*RepositoryImpl.kt` — implement paginated queries

### Subtask 2: BE — Enrich DTOs (AgentResponse.groupName, GroupResponse.agentCount)
**Files to modify:**
- `be/api-admin/.../adapter/inbound/web/dto/AgentDtos.kt` — add groupName field
- `be/api-admin/.../adapter/inbound/web/dto/GroupDtos.kt` — add agentCount field
- `be/api-admin/.../application/AgentQueryService.kt` — join with group for name
- `be/api-admin/.../application/GroupQueryService.kt` — count agents per group

### Subtask 3: FE — Align Types, Hooks, and API Layer
**Files to modify:**
- `fe/app-admin/src/types/index.ts` — align types with real BE DTOs
- `fe/app-admin/src/hooks/use-agents.ts` — fix request/response shapes
- `fe/app-admin/src/hooks/use-groups.ts` — fix request/response shapes
- `fe/app-admin/src/hooks/use-tenants.ts` — fix request/response shapes
- `fe/app-admin/src/hooks/use-monitoring.ts` — call correct endpoints
- `fe/app-admin/src/hooks/use-feedbacks.ts` — fix response shapes
**Files to create:**
- `fe/app-admin/src/hooks/use-stats.ts` — GET /api-adm/stats/summary, GET /api-adm/stats/agents

### Subtask 4: FE — Wire CRUD Pages (Agents, Groups, Tenants)
**Files to modify:**
- `fe/app-admin/src/app/agents/page.tsx` — replace MOCK_TENANTS/MOCK_GROUPS, handle temp password
- `fe/app-admin/src/app/groups/page.tsx` — replace MOCK_TENANTS, align create form
- `fe/app-admin/src/app/tenants/page.tsx` — align with BE (slug/status instead of domain/plan)

### Subtask 5: FE — Wire Dashboard, Monitoring, and Feedbacks
**Files to modify:**
- `fe/app-admin/src/app/dashboard/page.tsx` — wire to real stats API
- `fe/app-admin/src/app/monitoring/page.tsx` — call /monitoring/channels + /monitoring/agents
- `fe/app-admin/src/app/feedbacks/page.tsx` — align with real FeedbackResponse

## 6. Key Decisions

1. **Pagination on backend**: Use existing `PageResponse<T>` with Spring's `Pageable`-like params
2. **No plan/domain on Tenant**: Align FE to match BE (slug/status). Plan can be added later.
3. **Soft-delete only**: Use status changes instead of adding DELETE endpoints for tenants/agents
4. **Agent password**: Show auto-generated temp password in a modal after creation
5. **Monitoring**: FE calls two separate endpoints and composes the data client-side
6. **Feedback**: Display channelId, not agentName/clientName (avoids expensive joins)

## 7. Build Sequence

### Phase 1 — Backend (parallel, 2 worktrees)
- **wt-1**: Subtask 1 (pagination) — all 4 controllers
- **wt-2**: Subtask 2 (DTO enrichment) — AgentResponse + GroupResponse

### Phase 2 — Frontend (parallel, 3 worktrees, after BE merge)
- **wt-3**: Subtask 3 (types + hooks alignment)
- **wt-4**: Subtask 4 (CRUD pages) — blocked by wt-3
- **wt-5**: Subtask 5 (dashboard + monitoring + feedbacks) — blocked by wt-3

Effective parallelism: wt-1 ∥ wt-2, then merge, then wt-3, then wt-4 ∥ wt-5.
