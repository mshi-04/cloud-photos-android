---
name: media-sync-implementation
description: media 同期・アップロード・削除・Worker・Scheduler・SyncStatus の設計・実装・修正に使う。UploadMediaWorker、DeleteMediaWorker、WorkManager 契約、Room upload_records、S3/API 整合性、retry、状態遷移を触る作業で使用する。
---

# Media Sync Implementation

## 実装手順

1. 変更対象を `MediaViewModel`、UseCase、Scheduler、Worker、Repository、DataSource、Room、UI 表示のどこに入れるか分ける。
2. アップロード開始フローを触る場合は、`MediaViewModel.onScreenResumed()` から `SyncUploadRecordsUseCase`、`PrepareUploadQueueUseCase`、`ScheduleUploadUseCase`、`UploadSchedulerImpl`、`UploadMediaWorker` までの呼び出し順を確認する。
3. `UploadMediaWorker` を触る場合は、pending record 取得、content type 解決、S3 upload、local record 保存、remote record 作成、`completeUpload()` の順序と失敗分岐を追ってから修正する。
4. 削除フローを触る場合は、`ScheduleDeleteUseCase`、`DeleteSchedulerImpl`、`DeleteMediaWorker`、remote delete、S3 delete、local delete の順序を確認する。
5. `SyncStatus` を追加・変更する場合は、domain enum、DAO query、Entity mapper、UI 表示、Worker 分岐、テスト、`docs/media-upload-flow.md` を同じ変更単位に含める。
6. WorkManager 契約を触る場合は、unique work name、enqueue policy、constraints、tag、input/output Data、retry/backoff、foreground/notification の変更影響を先に整理する。
7. DB query/entity を触る場合は `db-implementation`、ViewModel/画面表示を触る場合は `ui-implementation`、Repository/DataSource/Mapper を触る場合は `data-implementation` も併用する。

## 検証手順

1. Worker/Scheduler/SyncStatus 変更は `./gradlew :feature:media:data:test` を実行する。
2. domain の状態遷移や UseCase に触れた場合は `./gradlew :feature:media:domain:test` も実行する。
3. UI 表示や ViewModel に触れた場合は `./gradlew :feature:media:ui:test` も実行する。
4. 一時/永続エラー、`CancellationException`、`cloudStoragePath == null`、pending 状態の上書き回避、`completeUpload()` の呼び出し回数をテストまたは報告で確認する。

## 参考資料

- [docs/media-upload-flow.md](../../../docs/media-upload-flow.md)
- [docs/error-handling-guide.md](../../../docs/error-handling-guide.md)
- [docs/implementation-rules.md](../../../docs/implementation-rules.md)
