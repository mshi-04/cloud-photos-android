---
name: android-media-upload
description: "Use when creating or modifying upload workers, delete workers, upload schedulers, upload/delete use cases, SyncStatus transitions, or UploadRecord persistence in the media feature"
---

# Media Upload / Delete Skill

## Primary references

Read these first before using this skill:
1. `AGENTS.md`
2. `feature/media/AGENTS.md`
3. `docs/media-upload-flow.md` — full flow sequence and SyncStatus transitions
4. `docs/error-handling-guide.md` — CancellationException and error classification

This skill is a quick pattern guide for the highest-risk area of the repository.
If it conflicts with repository guidance, prefer the repository guidance.

## Why this area is high-risk

The media feature mixes domain rules, Room persistence, S3 storage, remote API calls,
WorkManager lifecycle, and coroutine cancellation in a single background execution path.
A change in one layer can silently break retry behavior, produce inconsistent sync states,
or orphan remote records.

## Class responsibilities — do not collapse these

| Class | Layer | Responsibility |
|---|---|---|
| `UploadSchedulerImpl` | Data | Enqueue `UploadMediaWorker` via WorkManager |
| `DeleteSchedulerImpl` | Data | Enqueue `DeleteMediaWorker` via WorkManager |
| `UploadMediaWorker` | Data | Execute per-record upload: S3 + remote registration + DB update |
| `DeleteMediaWorker` | Data | Execute per-record delete: remote → S3 → local DB |
| `UploadDataSourceImpl` | Data | S3 stream upload / S3 delete via Amplify.Storage |
| `UploadRecordLocalDataSourceImpl` | Data | Room DAO operations for upload records |
| `UploadRecordRemoteDataSourceImpl` | Data | REST API calls via Amplify.API |
| `UploadRepositoryImpl` | Data | Delegate upload execution to data sources |

Do not merge these responsibilities into one class. Each has a different lifecycle and
testability profile.

## SyncStatus transitions

```
PENDING_UPLOAD → SYNCED          (upload + remote registration succeeded)
PENDING_UPLOAD → ERROR           (permanent failure)
SYNCED         → PENDING_DELETE  (user-triggered delete)
PENDING_DELETE → (removed)       (delete succeeded)
PENDING_DELETE → ERROR           (permanent failure)
```

When adding or modifying workers, keep these transitions consistent.
If you change a transition, update `docs/media-upload-flow.md`.

## Error handling rules

1. Re-throw `CancellationException` in every `runCatching` and `try/catch` block —
   including nested ones.
2. Classify errors as permanent or temporary before deciding on retry vs ERROR state.
3. Error mapping belongs in `UploadErrorMapper` and similar data-layer mapper objects,
   not in workers or repositories.

## Worker behavior rules

- `completeUpload()` is called once per worker run after all records are processed,
  not inside the per-record loop.
- If `cloudStoragePath` is null (never uploaded), skip remote/S3 steps in the delete worker.
- S3 orphan on delete is tolerated: if `deleteUploadedObject()` fails with a non-cancellation
  error, continue rather than retry.
- `hasTemporaryFailure = true` means `Result.retry()` at the end of `doWork()`.
  Do not return `Result.retry()` early inside the per-record loop.

## Decision checklist before changing this area

1. Does the change affect SyncStatus transitions?
2. Does the change affect retry behavior or worker result?
3. Does the change affect the order of remote registration vs local DB update?
4. Does the change affect `completeUpload()` timing?
5. Are all `CancellationException` re-throws preserved, including nested blocks?

## Output expectations

When using this skill, report:
- which media layer changed (domain / data / ui)
- whether worker or scheduler behavior changed
- whether SyncStatus transitions changed
- whether retry logic changed
- tests run
- remaining risks around background execution or record consistency
