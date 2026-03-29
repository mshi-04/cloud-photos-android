# Media Upload and Delete Flow

This document describes the upload and delete lifecycle in the `media` feature.
Rules live in `AGENTS.md` and `feature/media/AGENTS.md`. This file explains *how* the flow works
so that AI agents can reason about change impact without having to trace the entire call graph.

## Why this document exists

The `media` feature is the highest-risk area in the repository.
It mixes domain rules, local persistence, remote sync, background workers, and cancellation logic.
Misunderstanding the flow leads to broken retry behavior, inconsistent sync states, or orphaned
records.

---

## Upload flow

### Trigger (UI → Domain)

```text
MediaViewModel.onScreenResumed()
  ├─ syncRemote()         → SyncUploadRecordsUseCase
  ├─ prepareUploadQueue() → PrepareUploadQueueUseCase
  └─ scheduleUpload()     → ScheduleUploadUseCase
                               └─ UploadScheduler.scheduleUpload()
                                    └─ UploadSchedulerImpl → WorkManager.enqueueUniqueWork(UploadMediaWorker)
```

### Worker execution (Data — background)

```text
UploadMediaWorker.doWork()
  ↓
  LocalUploadRecordsRepository.getPendingUploadRecords()   [Room]
  ↓ (for each record)
  ContentTypeResolver.resolve()
  UploadDataSource.uploadMedia()
    └─ Amplify.Storage.uploadInputStream()                 [S3]
  ↓ (on success)
  LocalUploadRecordsRepository.saveUploadRecords()         [update local DB]
  RemoteUploadRecordsRepository.createUploadRecord()       [POST /media/uploads]
  ↓ (after all records)
  RemoteUploadRecordsRepository.completeUpload()           [POST /media/uploads/complete]
```

### SyncStatus transitions

```text
(new local media)
  PENDING_UPLOAD
      ↓ upload success + remote registration success
  SYNCED
      ↓ user deletes
  PENDING_DELETE
      ↓ delete success
  (record removed)

  PENDING_UPLOAD / PENDING_DELETE
      ↓ permanent failure
  ERROR
```

---

## Delete flow

### Trigger (UI → Domain)

```text
MediaViewModel.scheduleDelete()
  └─ ScheduleDeleteUseCase
       └─ DeleteScheduler.scheduleDelete()
            └─ DeleteSchedulerImpl → WorkManager.enqueueUniqueWork(DeleteMediaWorker)
```

### Worker execution (Data — background)

```text
DeleteMediaWorker.doWork()
  ↓
  LocalUploadRecordsRepository.getPendingDeleteRecords()   [Room]
  ↓ (for each record)
  RemoteUploadRecordsRepository.deleteUploadRecord()       [DELETE /media/uploads/:id]
  UploadDataSource.deleteUploadedObject()                  [S3 delete]
  LocalUploadRecordsRepository.deleteUploadRecord()        [remove from Room]
```

Special case: if `cloudStoragePath` is null (never uploaded), skip remote/S3 steps and
delete the local record directly.

S3 orphan tolerance: if `deleteUploadedObject()` fails with a non-cancellation error,
the worker continues rather than retrying, accepting the orphan.

---

## Remote sync flow

```text
SyncUploadRecordsUseCase
  └─ RemoteUploadRecordsRepository.getUploadRecords()      [GET /media/uploads]
       └─ LocalUploadRecordsRepository.saveUploadRecords() [upsert into Room]
```

This runs on resume to pull down records uploaded from other devices or sessions.

---

## Error classification

Workers distinguish between permanent and temporary failures to decide retry behavior.

| Failure type                        | Worker response                                     | SyncStatus |
|-------------------------------------|-----------------------------------------------------|------------|
| Permanent (e.g., HTTP 4xx)          | No retry; clean up and mark ERROR                   | `ERROR`    |
| Temporary (e.g., network, HTTP 5xx) | Set `hasTemporaryFailure = true` → `Result.retry()` | unchanged  |
| CancellationException               | Re-throw immediately                                | unchanged  |

---

## Key boundaries to respect when changing this flow

1. **Do not collapse worker/scheduler/repository/datasource responsibilities.**
   Each class has a different lifecycle and testability profile.
   See `docs/architecture-decisions.md` for the reasoning.

2. **SyncStatus transitions must stay consistent.**
   If you add a new status or change a transition, verify that workers, repositories, and UI state
   all agree on the meaning.

3. **Re-throw `CancellationException` in every `catch` block.**
   Workers use nested `runCatching` blocks; each must re-throw independently.
   See `docs/error-handling-guide.md`.

4. **Remote registration and local DB update must stay in sync.**
   If remote registration fails, the local record must not be left in a state that implies success.

5. **`completeUpload()` is called once per worker run, not per record.**
   It signals batch completion. Do not move it inside the per-record loop.
