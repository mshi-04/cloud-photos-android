# Worker

WorkManager のバックグラウンド処理を実行する `CoroutineWorker`。メディア同期系の 2 Worker（`feature:media:data`）と FCM デバイストークン登録 Worker（`app`）がある。

## 確認する対象

- Media Worker: [`UploadMediaWorker.kt`](../../../../feature/media/data/src/main/kotlin/com/appvoyager/cloudphotos/data/media/worker/UploadMediaWorker.kt)、[`DeleteMediaWorker.kt`](../../../../feature/media/data/src/main/kotlin/com/appvoyager/cloudphotos/data/media/worker/DeleteMediaWorker.kt)、[`UploadNotificationHelper.kt`](../../../../feature/media/data/src/main/kotlin/com/appvoyager/cloudphotos/data/media/worker/UploadNotificationHelper.kt)
- Device Token Worker: [`RegisterDeviceTokenWorker.kt`](../../../../app/src/main/kotlin/com/appvoyager/cloudphotos/fcm/RegisterDeviceTokenWorker.kt)、[`FcmTokenRegistrar.kt`](../../../../app/src/main/kotlin/com/appvoyager/cloudphotos/fcm/FcmTokenRegistrar.kt)
- WorkManager 初期化: [`CloudPhotosApp.kt`](../../../../app/src/main/kotlin/com/appvoyager/cloudphotos/CloudPhotosApp.kt)、[`app/src/main/AndroidManifest.xml`](../../../../app/src/main/AndroidManifest.xml)
- 業務契約の正本: メディア同期は [`docs/media-sync.md`](../../../../docs/media-sync.md)、デバイストークン登録は [`docs/authentication.md`](../../../../docs/authentication.md)

## 実装時の注意

- **必須制約**:
  - `@HiltWorker` と `@AssistedInject constructor(@Assisted context: Context, @Assisted workerParams: WorkerParameters, ...)` を使う。`CloudPhotosApp` の `HiltWorkerFactory` 設定と `AndroidManifest.xml` の `WorkManagerInitializer` 無効化（`tools:node="remove"`）を崩さない。
  - `doWork()` 内のすべての `runCatching` / `try-catch`（本処理だけでなく `completeUpload()` や S3 クリーンアップ等の副次処理を含む）で、`CancellationException` を最初に再スローする。握り潰すとキャンセルが `Result.retry()` や `Result.failure()` に化ける。
- **プロジェクト固有の契約**:
  - **`UploadMediaWorker` / `DeleteMediaWorker`**:
    - `doWork()` の処理順、1 件ごとの再取得と status 確認、`cloudStoragePath` 既存時の S3 スキップ、永続失敗（`SyncStatus.ERROR`）と一時失敗（状態維持 + `Result.retry()`）の分類、`completeUpload()` を Worker 実行単位の末尾で 1 回だけ呼ぶ契約、S3 孤立オブジェクトの扱いは [`docs/media-sync.md`](../../../../docs/media-sync.md) を厳守する。
    - リモート API の `isPermanentFailure` 判定（404・429 を除く 4xx）は `UploadMediaWorker` と `DeleteMediaWorker` の両方に存在するため、変更時は両 Worker の整合を確認する。
    - `UploadNotificationHelper` の進行中通知は pending が 1 件以上ある場合のみ表示し、ループ終了時に必ず `cancelNotification()` する。
  - **`RegisterDeviceTokenWorker`（`app` モジュール）**:
    - Scheduler interface を介さず `companion object` の `enqueue(context, token)` から直接 WorkManager へ登録する。
    - メディア系 Worker（`KEEP`・input Data なし）とは異なり、最新トークンを優先するため `ExistingWorkPolicy.REPLACE`・`KEY_TOKEN` input Data・指数バックオフを使用する。
    - 空トークンや `AmplifyRestException.isNonServerError`（4xx 等）は `Result.failure()`、それ以外の一時失敗は `Result.retry()` を返す（詳細は [`docs/authentication.md`](../../../../docs/authentication.md)）。
- **波及先**:
  - Worker の `WORK_NAME` や input Data キー、enqueue 条件を変更する場合は、対応する Scheduler（[`scheduler.md`](scheduler.md)）または `FcmTokenRegistrar` / `CloudPhotosFirebaseMessagingService`、およびテストを同時に確認する。

## テスト判断

- Worker の分岐・状態遷移・retry/failure/success 戻り値・`CancellationException` 伝播は、WorkManager を起動せず Worker コンストラクタに直接 mock を渡して `worker.doWork()` を呼ぶ JVM Unit Test（`UploadMediaWorkerTest` / `DeleteMediaWorkerTest` / `RegisterDeviceTokenWorkerTest`）で検証する（[`docs/testing.md`](../../../../docs/testing.md) の「対象別のテストの書き方」の Worker 節を参照）。

## 検証観点

- 一時エラー時に誤って `SyncStatus.ERROR` へ落として自動 retry 不能にしていないか。
- 永続エラー時に無限 `Result.retry()` ループになっていないか。
- `UploadMediaWorker` と `DeleteMediaWorker` で共通の失敗判定ロジックに片側だけの修正漏れがないか。
