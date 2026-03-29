# feature/media/AGENTS.md

Local guidance for the `media` feature.
Read root `AGENTS.md` first, then apply the rules below.
If this file conflicts with root `AGENTS.md`, prefer the root file and keep the change conservative.

## Scope

This feature is split into:

- `feature/media/domain`
- `feature/media/data`
- `feature/media/ui`

The former `feature/settings` module has been merged into this feature.
Settings domain contracts, use cases, value objects, and persistence now live inside the media
module layers.

## Intent of this feature

This feature owns media listing, upload, delete, upload-record persistence, sync status handling,
background scheduling related to media operations, and lightweight user settings.
This is one of the highest-risk areas in the repository because it mixes domain rules, local
persistence, remote sync, background workers, and user-facing settings.

## Domain rules

Keep `feature/media/domain` focused on:

- media domain models
- upload/delete-related use cases
- repository contracts
- scheduler contracts
- value objects for validated media concepts
- settings repository contracts
- settings use cases
- settings value objects
- settings-specific validation rules

Do not place here:

- Android framework types
- Room/DAO/entity code
- Amplify/API/storage SDK specifics
- WorkManager implementation details

Rules:

- Keep use cases single-purpose.
- Preserve the distinction between upload orchestration, sync, local record management, and
  scheduling.
- Do not move worker behavior into domain use cases.
- Keep validated settings concepts in value objects when that pattern already exists.
- If a setting starts being shared by multiple features, be explicit about whether it still belongs
  here or should move to a shared/core location.

## Data rules

`feature/media/data` is responsible for:

- local media access
- upload record persistence
- remote upload record sync
- repository implementations
- worker/scheduler implementations
- mapping between database/remote/data models and domain models
- reading/writing persisted settings values
- implementing settings repository contracts
- translating persistence models into domain-facing settings values

Rules:

- Preserve the split between local data access, remote sync, repository coordination, and worker
  execution.
- Do not collapse multiple responsibilities into one large class.
- Repository implementations should delegate to data sources and mapper objects.
- Keep sync-status translation and error translation localized.
- Re-throw `CancellationException` in coroutine error handling.
- Keep settings persistence details in data; do not let UI or unrelated features access storage
  details directly.
- Prefer small repository implementations and focused data source code for settings.

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
If only settings logic changed, `:feature:media:domain:test` and `:feature:media:data:test` are
sufficient as a first pass.

## Report back with

- which media layer changed
- whether worker/scheduler behavior changed
- whether sync status or upload/delete flow semantics changed
- whether any settings semantics or validation changed
- tests run
- remaining risks around background execution or record consistency
- any follow-up if a setting may outgrow this feature and should move to core
