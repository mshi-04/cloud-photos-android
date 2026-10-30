# Media Sync

この文書はメディアのアップロード・削除・リモート同期、`SyncStatus` の状態遷移、Worker の失敗分類、WorkManager 契約、およびアップロード通知の扱いをまとめます。
Room テーブル構成と DB 保存文字列の互換性は [`docs/data-model.md`](data-model.md) を正本とします。

## 責任境界

| 要素 | 責任 |
| --- | --- |
| [`MediaViewModel`](../feature/media/ui/src/main/kotlin/com/appvoyager/cloudphotos/ui/media/viewmodel/MediaViewModel.kt) | 画面復帰時の同期・キュー準備・スケジュール要求、UI state / effect の公開 |
| UseCase（`:feature:media:domain`） | リモート同期での pending 保護、未登録メディアの差分抽出、削除予約、Scheduler 起動 |
| Scheduler（`:feature:media:domain` / `:data`） | WorkManager への unique work enqueue 条件（`KEEP`、ネットワーク制約） |
| Worker（`:feature:media:data`） | バックグラウンドでの S3 転送・API 登録/削除、一時失敗と永続失敗の判定、進行中通知 |
| Repository / DataSource（`:feature:media:data`） | Room、S3、REST API、MediaStore の具体 I/O と Mapper 適用 |

ViewModel から WorkManager を直接呼ばず、Worker 内に画面都合のロジックを持ち込まない。

## アップロード開始とキュー準備

```text
MediaViewModel.onScreenResumed()
  ├─ SyncUploadRecordsUseCase        (リモート状態を取得し、pending を除いてローカル保存)
  ├─ PrepareUploadQueueUseCase       (ローカルメディアと既存レコードの差分を PENDING_UPLOAD で保存)
  └─ ScheduleUploadUseCase
       └─ UploadScheduler.scheduleUpload()
            └─ UploadSchedulerImpl -> WorkManager.enqueueUniqueWork(UploadMediaWorker)
```

- **画面復帰時の間引きと直列化**:
  - `MediaViewModel.onScreenResumed()` は権限ダイアログ開閉などによる連続発火を防ぐため、`MIN_RESUME_INTERVAL_MS` 未満の呼び出しを早期 return で無視する。
  - 新しい `launch` 内では前回の同期 Job を `cancelAndJoin()` で完了待ちしてから次の同期を開始し、Room と WorkManager への重複登録を防ぐ。
- **キュー準備とチャンク取得**:
  - [`PrepareUploadQueueUseCase`](../feature/media/domain/src/main/kotlin/com/appvoyager/cloudphotos/domain/media/usecase/PrepareUploadQueueUseCase.kt) は端末メディアが空、または未登録の差分が空のときは早期 return し、不要な Scheduler 起動を行わない。
  - [`RecordMediaUploadUseCase`](../feature/media/domain/src/main/kotlin/com/appvoyager/cloudphotos/domain/media/usecase/RecordMediaUploadUseCase.kt) は `PENDING_UPLOAD` レコードの保存と `scheduleUpload()` を一体で実行する。
  - [`GetPendingMediaUseCase`](../feature/media/domain/src/main/kotlin/com/appvoyager/cloudphotos/domain/media/usecase/GetPendingMediaUseCase.kt) は SQLite のバインド変数上限を超えないよう、メディア ID リストを 900 件ずつ `chunked` して `getUploadRecords` を呼ぶ。

## リモート同期と Pending 保護

- **未送信操作の保護（[`SyncUploadRecordsUseCase`](../feature/media/domain/src/main/kotlin/com/appvoyager/cloudphotos/domain/media/usecase/SyncUploadRecordsUseCase.kt)）**:
  - リモートから取得したレコード一覧をローカル DB へ保存する際、`localRepository.getPendingRecordMediaIds()`（`PENDING_UPLOAD` と `PENDING_DELETE`）に含まれる `mediaId` を除外してから保存する。
  - リモートの古い状態でローカルの未送信アップロードや削除予約を上書きして消さないための必須契約である。
- **部分的な破損レコードのスキップ（[`RemoteUploadRecordMapper.fromFetchResponse`](../feature/media/data/src/main/kotlin/com/appvoyager/cloudphotos/data/media/util/RemoteUploadRecordMapper.kt)）**:
  - `records` 配列の各要素を `runCatching { ... }.getOrNull()` で変換し、必須フィールド欠落や値オブジェクト検証失敗を起こした要素 1 件だけをスキップして残りを返す。1 件の不正データで同期一覧全体が失敗するのを防ぐトレードオフであり、レスポンス全体の JSON 構造が壊れている場合は通常どおり例外を伝播する。

## `UploadMediaWorker` の処理順と契約

[`UploadMediaWorker.doWork()`](../feature/media/data/src/main/kotlin/com/appvoyager/cloudphotos/data/media/worker/UploadMediaWorker.kt) は次の順序で実行する。

1. `localRepository.getPendingUploadRecords()` を取得し、空なら通知を出さずに `Result.success()` を返す。
2. 処理対象がある場合のみ `notificationHelper.show(pendingRecords.size)` で進行中通知を表示する。
3. 各レコードをループ処理する:
   - ループ先頭で `localRepository.getUploadRecords(listOf(record.mediaId)).firstOrNull()` を取り直し、存在しない場合や `SyncStatus.PENDING_UPLOAD` でなくなっている場合はスキップする（一覧取得後に削除予約などへ変わったレコードを誤処理しないため）。
   - `ContentTypeResolver` で MIME type を解決し、`ContentType.of` で検証する。失敗した場合は永続失敗として `SyncStatus.ERROR` を保存し次へ進む。
   - **S3 アップロードと再送防止**:
     - `current.cloudStoragePath != null` の場合は、すでに S3 転送が完了しているため S3 アップロードをスキップし、リモート API 登録から再開する（retry ごとに重複オブジェクトを作らないための分岐）。
     - `cloudStoragePath == null` の場合は `resolveUri` で URI を解決し（null なら `SyncStatus.ERROR`）、`uploadDataSource.uploadMedia` で S3 へ送信する。成功したら直ちに `cloudStoragePath` をローカル DB に保存してからリモート API 登録へ進む。
   - **リモート API 登録（`remoteRepository.createUploadRecord`）**:
     - 成功したら返却されたレコード（`SyncStatus.SYNCED`）をローカル DB に保存し、`successCount` を増やす。
     - 永続失敗した場合は、先にアップロードした S3 オブジェクトの削除（`deleteUploadedObject`）を試みる。S3 削除に成功したら `cloudStoragePath = null, syncStatus = SyncStatus.ERROR` を保存し、S3 削除に失敗したら孤立オブジェクトを残さないよう `SyncStatus.PENDING_DELETE` を保存して削除フローへ引き継ぐ。
4. ループ終了後に `notificationHelper.cancel()` で進行中通知を消す。
5. **`completeUpload()` の呼び出し単位**:
   - `!hasTemporaryFailure && successCount > 0` のときだけ、ループの外（Worker 実行単位）で 1 回だけ `remoteRepository.completeUpload(UploadSuccessCount.of(successCount))` を呼ぶ。レコードごとに呼ぶと完了通知が重複するため禁止する。
   - `completeUpload()` の失敗は `CancellationException` のみ再スローし、アップロード自体の成否は巻き戻さない。
6. `hasTemporaryFailure` が true なら `Result.retry()`、false なら `Result.success()` を返す。

## 削除フローと `DeleteMediaWorker`

- **削除予約（[`DeleteMediaUseCase`](../feature/media/domain/src/main/kotlin/com/appvoyager/cloudphotos/domain/media/usecase/DeleteMediaUseCase.kt)）**:
  - 対象レコードの `syncStatus` が `PENDING_UPLOAD` の場合は、ローカル DB から物理削除（`deleteUploadRecord`）して早期 return し、`DeleteScheduler` は起動しない。S3 転送後にリモート API 登録が一時失敗した場合など、`cloudStoragePath` が設定済みでもこの状態に残る。この行を `DeleteMediaUseCase` で削除すると、S3 オブジェクトが削除されず孤立する可能性がある。
  - それ以外は `isDeleted = true, syncStatus = SyncStatus.PENDING_DELETE` に更新して保存し、`DeleteScheduler.scheduleDelete()` を呼ぶ。
- **`DeleteMediaWorker.doWork()`（[`DeleteMediaWorker`](../feature/media/data/src/main/kotlin/com/appvoyager/cloudphotos/data/media/worker/DeleteMediaWorker.kt)）**:
  - `cloudStoragePath == null` のレコードは未アップロード扱いのため、リモート API 削除と S3 削除をスキップしてローカル DB から物理削除する。
  - `cloudStoragePath != null` のレコードは次の順で削除する:
    1. `remoteRepository.deleteUploadRecord(record.mediaId)` を呼ぶ。失敗時は一時失敗なら `hasTemporaryFailure = true`、永続失敗なら `SyncStatus.ERROR` を保存して次へ進む。
    2. `uploadDataSource.deleteUploadedObject(cloudStoragePath)` で S3 オブジェクトを削除する。**既存仕様として、S3 削除の失敗は `CancellationException` のみ再スローして握り潰し、孤立オブジェクトを許容してローカル削除へ進む。** この動作を変える場合は retry 増加やローカル/リモート整合への影響を報告する。
    3. `localRepository.deleteUploadRecord(record.mediaId)` でローカルレコードを物理削除する。

## `SyncStatus` の状態遷移と失敗分類

```text
新規ローカルメディア -> PENDING_UPLOAD -> SYNCED -> PENDING_DELETE -> (物理削除)
                        │                           │
                        └─────────> ERROR <─────────┘
```

- `PENDING_UPLOAD`: ローカルにありアップロード待ち。
- `SYNCED`: S3 アップロードとリモート API 登録が完了。
- `PENDING_DELETE`: 削除予約済み（またはアップロード中のリモート登録永続失敗かつ S3 クリーンアップ失敗からの引き継ぎ）。
- `ERROR`: 自動 retry では回復しない永続的失敗。

### エラー分類の基準

| 発生箇所 / 条件 | 分類 | Worker の扱い |
| --- | --- | --- |
| `CancellationException`（全経路・cleanup 含む） | キャンセル | 状態を変更せず直ちに再スロー |
| ContentType 解決不可 / 生成失敗 / localUri 解決不可 | 永続失敗 | `SyncStatus.ERROR` を保存し retry しない |
| S3 `UploadError`: `AccessDenied` / `NotAuthenticated` / `StorageLimitExceeded` / `FileNotFound` | 永続失敗 | `SyncStatus.ERROR` を保存し retry しない |
| S3 `UploadError`: `Network` / `Unknown` | 一時失敗 | 状態を維持し `hasTemporaryFailure = true` → `Result.retry()` |
| リモート API 例外: `Unexpected response code (\d+)` が `404`・`429` を除く `400..499` | 永続失敗 | `SyncStatus.ERROR`（または S3 削除失敗時 `PENDING_DELETE`）を保存 |
| リモート API 例外: `404` / `429` / `5xx` / レスポンスコードなし | 一時失敗 | 状態を維持し `hasTemporaryFailure = true` → `Result.retry()` |

リモート API の `isPermanentFailure` 判定は `UploadMediaWorker` と `DeleteMediaWorker` の双方にあるため、条件を変更する場合は両方の整合を保つ。

## WorkManager 契約と SDK キャンセル伝播

- **Scheduler の enqueue 契約（[`UploadSchedulerImpl`](../feature/media/data/src/main/kotlin/com/appvoyager/cloudphotos/data/media/worker/UploadSchedulerImpl.kt) / [`DeleteSchedulerImpl`](../feature/media/data/src/main/kotlin/com/appvoyager/cloudphotos/data/media/worker/DeleteSchedulerImpl.kt)）**:
  - unique work name: `UploadMediaWorker.WORK_NAME`（`"upload_media_worker"`）、`DeleteMediaWorker.WORK_NAME`（`"delete_media_worker"`）。
  - policy: `ExistingWorkPolicy.KEEP`（進行中のアップロード・削除を中断させないため）。
  - constraints: `NetworkType.CONNECTED` のみ。
  - request 種別: `OneTimeWorkRequest`、input Data は渡さない（`KEEP` では後続の enqueue が破棄されるため、処理対象は常に Worker が Room の pending レコードから取得する）。
  - これらを変更すると既存キューとの互換性が変わるため、変更時は [`MediaSchedulerInstrumentedTest`](../feature/media/data/src/androidTest/kotlin/com/appvoyager/cloudphotos/data/media/worker/MediaSchedulerInstrumentedTest.kt) と影響を合わせて確認する。
- **SDK 操作のキャンセル伝播**:
  - S3 アップロード（[`UploadDataSourceImpl`](../feature/media/data/src/main/kotlin/com/appvoyager/cloudphotos/data/media/datasource/UploadDataSourceImpl.kt) の `Amplify.Storage.uploadInputStream`）と REST API 呼び出し（[`awaitAmplifyRestCall`](../core/data/src/main/kotlin/com/appvoyager/cloudphotos/data/common/AmplifyRestClient.kt)）は `Cancelable` operation を返すため、`coroutine.invokeOnCancellation { operation?.cancel() }` でキャンセルを伝播させる。

## アップロード通知

アップロードに関する通知は 2 種類あり、どちらも `POST_NOTIFICATIONS` 権限（Android 13 以降）を確認してから表示する。通知チャネルは [`CloudPhotosApp.onCreate()`](../app/src/main/kotlin/com/appvoyager/cloudphotos/CloudPhotosApp.kt) で作成する。

- **アップロード進行中通知（[`UploadNotificationHelper`](../feature/media/data/src/main/kotlin/com/appvoyager/cloudphotos/data/media/worker/UploadNotificationHelper.kt)）**:
  - チャネル ID `"upload_progress"`（`IMPORTANCE_LOW`）。`UploadMediaWorker` が処理開始時に表示し、ループ終了時に `cancel()` する。
- **アップロード完了プッシュ通知（[`CloudPhotosFirebaseMessagingService.onMessageReceived`](../app/src/main/kotlin/com/appvoyager/cloudphotos/fcm/CloudPhotosFirebaseMessagingService.kt)）**:
  - チャネル ID `"upload_complete"`（`IMPORTANCE_DEFAULT`、silent）。`completeUpload()` 後にバックエンドから配信される FCM notification メッセージを受信し、`MainActivity` を開く `PendingIntent` 付きで表示する。
