# Scheduler

Scheduler は WorkManager への enqueue だけを担う薄いクラス。interface を domain の `feature/media/domain/src/main/kotlin/com/appvoyager/cloudphotos/domain/media/repository/` に、実装を Worker と同じ `feature/media/data/src/main/kotlin/com/appvoyager/cloudphotos/data/media/worker/` に置く。既存は `UploadScheduler` / `UploadSchedulerImpl` と `DeleteScheduler` / `DeleteSchedulerImpl` の 2 組。

## interface (domain)

メソッドは 1 つだけで、`scheduleUpload()` / `scheduleDelete()` のように引数も返り値も持たない。何をアップロード・削除するかを呼び出し側が渡さないのは、対象を Worker が Room の pending レコードから取り直すため。結果として domain 側に WorkManager の型も対象 ID も現れない。

呼び出し元はアップロードが `ScheduleUploadUseCase`、`PrepareUploadQueueUseCase`、`RecordMediaUploadUseCase`、削除が `ScheduleDeleteUseCase`、`DeleteMediaUseCase`。

## 実装 (data)

`@Inject constructor` で `@param:ApplicationContext` の `Context` だけを受け取り、domain の interface を実装する。メソッド本体は `OneTimeWorkRequestBuilder<対応する Worker>()` に `Constraints` を設定して build し、`WorkManager.getInstance(context).enqueueUniqueWork(...)` を呼ぶだけ。`UploadSchedulerImpl` と `DeleteSchedulerImpl` の差は Worker 型と参照する `WORK_NAME` のみで、それ以外は同一。

## enqueue 契約

- unique work name は Worker の companion object の `WORK_NAME` を参照する。Scheduler 側にリテラルを書くと、両者の名前がずれても気付けない。
- 既存 work の扱いは `ExistingWorkPolicy.KEEP`。実行中のアップロードを再起動しないための選択で、`REPLACE` にすると進行中の work が中断される。
- constraints は `NetworkType.CONNECTED` のみ。バッテリー、ストレージ、充電状態の条件は付けていない。
- 定期実行ではなく `OneTimeWorkRequest`。周期実行へ変えると pending の消化タイミングが UseCase の呼び出しから切り離される。
- input Data は渡さない。KEEP では 2 回目以降の enqueue が捨てられるため、input Data に依存すると渡したつもりの値が届かないケースが生まれる。
- backoff は既定のまま設定しない。Worker が返す `Result.retry()` がそのまま WorkManager の再実行になる。
- tag も付けていない。`getWorkInfosForUniqueWork(WORK_NAME)` で参照できる前提で書かれている。

unique work name、enqueue policy、constraints を変更すると、変更前にキューされた work との互換性が失われる。

## Hilt binding

`app/src/main/kotlin/com/appvoyager/cloudphotos/di/UploadRecordModule.kt` の `@Module @InstallIn(SingletonComponent::class)` に `@Binds @Singleton` で登録する。Impl 側に `@Singleton` は付けず、binding 側で付けている。

## 検証

enqueue 契約は unit test では確認できないため androidTest に置く。`feature/media/data/src/androidTest/kotlin/com/appvoyager/cloudphotos/data/media/worker/MediaSchedulerInstrumentedTest.kt` が `WorkManagerTestInitHelper.initializeTestWorkManager()` で初期化し、Impl を直接 new して実行している。

確認しているのは 2 点。1 つは enqueue 後の `WorkSpec` を private な data class にまとめ、work 件数、`WorkInfo.State.ENQUEUED`、Worker クラス名、`requiredNetworkType`、input Data の有無をまとめて `assertEquals` で比較すること。もう 1 つは同じ Scheduler を 2 回呼び、`getWorkInfosForUniqueWork(WORK_NAME)` の件数が 1 のままであること、つまり KEEP が効いていること。

実行コマンドは `./gradlew :feature:media:data:connectedDebugAndroidTest`。エミュレータや実機がない場合は CI の instrumented-test job に委ねたことを報告する。
