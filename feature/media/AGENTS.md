# feature/media/AGENTS.md

Local guidance for the `media` feature.
Read root `AGENTS.md` first, then apply the rules below.
If this file conflicts with root `AGENTS.md`, prefer the root file and keep the change conservative.

## Scope

This feature is split into:
- `feature/media/domain`
- `feature/media/data`
- `feature/media/ui`

## Intent of this feature

This feature owns media listing, upload, delete, upload-record persistence, sync status handling, and background scheduling related to media operations.
This is one of the highest-risk areas in the repository because it mixes domain rules, local persistence, remote sync, and background workers.

## Domain rules

Keep `feature/media/domain` focused on:
- media domain models
- upload/delete-related use cases
- repository contracts
- scheduler contracts
- value objects for validated media concepts

Do not place here:
- Android framework types
- Room/DAO/entity code
- Amplify/API/storage SDK specifics
- WorkManager implementation details

Rules:
- Keep use cases single-purpose.
- Preserve the distinction between upload orchestration, sync, local record management, and scheduling.
- Do not move worker behavior into domain use cases.

## Data rules

`feature/media/data` is responsible for:
- local media access
- upload record persistence
- remote upload record sync
- repository implementations
- worker/scheduler implementations
- mapping between database/remote/data models and domain models

Rules:
- Preserve the split between local data access, remote sync, repository coordination, and worker execution.
- Do not collapse multiple responsibilities into one large class.
- Repository implementations should delegate to data sources and mapper objects.
- Keep sync-status translation and error translation localized.
- Re-throw `CancellationException` in coroutine error handling.

## Worker and scheduler guardrails

Be especially careful when touching:
- upload queue preparation
- retry behavior
- sync status transitions
- local/remote upload record consistency
- notification side effects
- delete scheduling and background cleanup

Rules:
- Prefer existing worker/scheduler patterns.
- Avoid changing execution semantics unless the task explicitly requires it.
- If worker input/output or retry behavior changes, mention it explicitly in the report.

## UI rules

`feature/media/ui` is responsible for:
- media screen rendering
- media ViewModels
- media UI state/effect models
- user-triggered actions such as selecting layout or starting UI-driven flows

Rules:
- Keep business logic in use cases, not in composables.
- ViewModels should orchestrate use cases and expose state/effects.
- Keep grid/layout preferences or filtering behavior aligned with settings/domain responsibilities.
- Be careful when changing user-visible status handling for uploads/deletes.

## Model separation rules

Do not return:
- Room entities
- remote DTOs
- worker-only models
- raw provider/storage responses

to UI or domain APIs directly.
Always map them to domain-facing models first.

## Testing guidance

Prefer targeted media tests first:

```bash
./gradlew :feature:media:domain:test
./gradlew :feature:media:data:test
./gradlew :feature:media:ui:test
```

Run broader tests if changes affect app wiring, shared modules, or multiple media layers.

## Report back with

- which media layer changed
- whether worker/scheduler behavior changed
- whether sync status or upload/delete flow semantics changed
- tests run
- remaining risks around background execution or record consistency
