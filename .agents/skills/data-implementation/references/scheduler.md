# Scheduler

メディア同期 Worker を WorkManager へ enqueue する `UploadSchedulerImpl` / `DeleteSchedulerImpl`（`feature/media/data/.../worker/`）。WorkManager のキュー契約の詳細は [`docs/media-sync.md`](../../../../docs/media-sync.md) の「WorkManager 契約と SDK キャンセル伝播」を正本とする。

## 確認する対象

- Domain interface: [`UploadScheduler.kt`](../../../../feature/media/domain/src/main/kotlin/com/appvoyager/cloudphotos/domain/media/repository/UploadScheduler.kt)、[`DeleteScheduler.kt`](../../../../feature/media/domain/src/main/kotlin/com/appvoyager/cloudphotos/domain/media/repository/DeleteScheduler.kt)
- Data 実装: [`UploadSchedulerImpl.kt`](../../../../feature/media/data/src/main/kotlin/com/appvoyager/cloudphotos/data/media/worker/UploadSchedulerImpl.kt)、[`DeleteSchedulerImpl.kt`](../../../../feature/media/data/src/main/kotlin/com/appvoyager/cloudphotos/data/media/worker/DeleteSchedulerImpl.kt)
- DI 配線: [`UploadRecordModule.kt`](../../../../app/src/main/kotlin/com/appvoyager/cloudphotos/di/UploadRecordModule.kt)
- Instrumented Test: [`MediaSchedulerInstrumentedTest.kt`](../../../../feature/media/data/src/androidTest/kotlin/com/appvoyager/cloudphotos/data/media/worker/MediaSchedulerInstrumentedTest.kt)

## 実装時の注意

- **必須制約**:
  - unique work name は対応する Worker の `companion object` の `WORK_NAME`（`UploadMediaWorker.WORK_NAME` / `DeleteMediaWorker.WORK_NAME`）を参照し、Scheduler 側に文字列リテラルを重複定義しない。
  - メディア系 Scheduler は `OneTimeWorkRequest` + `ExistingWorkPolicy.KEEP` + `NetworkType.CONNECTED` 制約とし、input Data は渡さない（`KEEP` により後続の enqueue が破棄されても Worker 側が Room の pending レコードを再取得して全件処理するため）。
  - unique work name、enqueue policy、constraints を変更すると既存キューとの互換性が崩れるため、仕様変更の指示がない限り維持する。
- **`RegisterDeviceTokenWorker` との違い**:
  - FCM デバイストークン登録は domain の Scheduler interface を持たず、`RegisterDeviceTokenWorker.enqueue(context, token)` が `ExistingWorkPolicy.REPLACE` と input Data（`KEY_TOKEN`）で直接 enqueue する（詳細は [`worker.md`](worker.md) と [`docs/authentication.md`](../../../../docs/authentication.md) を参照）。メディア系 Scheduler の `KEEP` パターンと混同しない。
- **波及先**:
  - 新しい Scheduler interface / 実装を追加する場合は `app/src/main/kotlin/com/appvoyager/cloudphotos/di/UploadRecordModule.kt`（または対応する DI module）に `@Binds @Singleton` を登録する。

## テスト判断

- Scheduler の WorkSpec（状態、`requiredNetworkType`、Worker クラス名、input Data、2 回呼び出し時の `KEEP` 動作）は JVM テストではなく `feature/media/data/src/androidTest/.../MediaSchedulerInstrumentedTest.kt` で検証する（[`docs/testing.md`](../../../../docs/testing.md) の「対象別のテストの書き方」の InstrumentedTest 節を参照）。

## 検証観点

- 実行中の Worker がある状態で再度 schedule されたとき、`KEEP` によって進行中の処理が中断されずに維持されるか。
- Worker 側の `WORK_NAME` 定数を直接参照しているか。
