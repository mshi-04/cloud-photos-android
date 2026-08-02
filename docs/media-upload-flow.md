# メディアアップロード/削除フロー

`feature:media` の同期処理は、Room、API、S3、WorkManager、UI stateをまたぐ高リスク領域です。

## 責任境界

| 要素 | 責任 |
|---|---|
| `MediaViewModel` | 画面イベント、UI state/effect、UseCase呼び出し |
| UseCase | 同期、アップロード準備、削除予約、スケジュール要求 |
| Scheduler | WorkManagerへenqueueする条件と方法 |
| Worker | バックグラウンド実行、retry/failure判断 |
| Repository | domain contract、DataSource調整、mapper適用 |
| DataSource | Room、DataStore、S3、APIなど具体I/O |

責任を混ぜないこと。特にWorkerへUI都合を入れず、ViewModelからWorkManagerを直接呼びません。

## アップロード開始

```text
MediaViewModel.onScreenResumed()
  ├─ syncRemote()
  │    └─ SyncUploadRecordsUseCase
  ├─ prepareUploadQueue()
  │    └─ PrepareUploadQueueUseCase
  └─ scheduleUpload()
       └─ ScheduleUploadUseCase
            └─ UploadScheduler.scheduleUpload()
                 └─ UploadSchedulerImpl
                      └─ WorkManager.enqueueUniqueWork(UploadMediaWorker)
```

## UploadMediaWorker

```text
UploadMediaWorker.doWork()
  └─ LocalUploadRecordsRepository.getPendingUploadRecords()
       └─ for each record
            ├─ ContentTypeResolver.resolve()
            ├─ UploadDataSource.uploadMedia()
            ├─ LocalUploadRecordsRepository.saveUploadRecords()
            └─ RemoteUploadRecordsRepository.createUploadRecord()
       └─ RemoteUploadRecordsRepository.completeUpload()
```

守ること:

- `completeUpload()` はWorker実行単位で1回呼ぶ。
- リモート登録失敗を成功扱いにしない。
- 一時エラーではretry可能な状態を維持する。
- 永続エラーでは `ERROR` へ遷移させる。
- `CancellationException` は再スローする。

## 削除開始

```text
MediaViewModel.scheduleDelete()
  └─ ScheduleDeleteUseCase
       └─ DeleteScheduler.scheduleDelete()
            └─ DeleteSchedulerImpl
                 └─ WorkManager.enqueueUniqueWork(DeleteMediaWorker)
```

## DeleteMediaWorker

```text
DeleteMediaWorker.doWork()
  └─ LocalUploadRecordsRepository.getPendingDeleteRecords()
       └─ for each record
            ├─ RemoteUploadRecordsRepository.deleteUploadRecord()
            ├─ UploadDataSource.deleteUploadedObject()
            └─ LocalUploadRecordsRepository.deleteUploadRecord()
```

`cloudStoragePath == null` は未アップロード扱いです。リモート/S3削除をスキップしてローカルレコードを削除します。

既存仕様では、削除時のS3削除失敗を孤立オブジェクトとして許容して処理継続するケースがあります。
この動作を変える場合は、retry増加、ユーザー表示、ローカル/リモート不整合を報告します。

## リモート同期

```text
SyncUploadRecordsUseCase
  └─ RemoteUploadRecordsRepository.getUploadRecords()
       └─ LocalUploadRecordsRepository.saveUploadRecords()
```

目的:

- 他デバイス/他セッションのアップロード結果を取り込む。
- ローカル表示をリモート状態へ再同期する。

`PENDING_UPLOAD` や `PENDING_DELETE` をリモート状態で上書きする変更は慎重に扱います。

## SyncStatus

```text
new local media
  -> PENDING_UPLOAD
  -> SYNCED
  -> PENDING_DELETE
  -> deleted

PENDING_UPLOAD / PENDING_DELETE
  -> ERROR
```

意味:

- `PENDING_UPLOAD`: ローカルにあり、アップロード待ち。
- `SYNCED`: アップロードとリモート登録が完了。
- `PENDING_DELETE`: 削除予約済み。
- `ERROR`: 永続的失敗。自動retryでは解消しない。

ステータス追加や意味変更は、domain、data、ui、テスト、文書を同時に更新します。

## エラー分類

| エラー | Worker応答 | SyncStatus |
|---|---|---|
| `CancellationException` | 再スロー | 変更なし |
| 一時的エラー | `Result.retry()` | 原則変更なし |
| 永続的エラー | retryしない | `ERROR` |
| 削除時の孤立許容ケース | 処理継続 | レコード削除される場合あり |

[docs/error-handling-guide.md](error-handling-guide.md) と一致させます。

## WorkManager契約

変更時に確認する:

- unique work name
- enqueue policy
- constraints
- tag
- input/output Data
- retry/backoff
- foreground/notification

これらを変えた場合は、既存キューや互換性への影響を報告します。

## 変更チェックリスト

- Worker/Scheduler/Repository/DataSourceの責任が混ざっていない。
- SyncStatusの意味がdomain/data/uiで一致している。
- ローカルDB更新とリモートAPI呼び出しの順序が不整合を生まない。
- `CancellationException` を再スローしている。
- retryされる失敗とretryされない失敗がテストされている。
- WorkManager契約を不用意に変えていない。
