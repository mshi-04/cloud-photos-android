# Worker テスト

`feature/media/data/src/test/.../worker/` の `UploadMediaWorkerTest` と `DeleteMediaWorkerTest`、
`app/src/test/.../fcm/RegisterDeviceTokenWorkerTest` が対象です。いずれも JUnit 5 です。

## クラスの形

WorkManager を起動しません。`WorkManagerTestInitHelper` も使わず、Worker の constructor を直接呼んで
`worker.doWork()` を実行します。enqueue の契約（unique name、constraints、input Data）は
`src/androidTest` の Scheduler テスト側で見ます。

class に `@OptIn(ExperimentalCoroutinesApi::class)` を付け、private field として `StandardTestDispatcher()`、
`Context`、`WorkerParameters`、各 repository / dataSource の mock を持ちます。Worker は `lateinit var` です。

`Context`、`WorkerParameters`、通知 helper のように「振る舞いを検証しない Android 依存」は `mockk(relaxed = true)` にします。
検証対象の repository と dataSource は `relaxed` にせず、必要な stub を各テストで明示します。
`relaxed` を広げると、stub し忘れたまま default 値が返って分岐が意図せず通ります。

`@BeforeEach` で `Dispatchers.setMain(testDispatcher)` を呼んでから Worker を構築し、
`@AfterEach` で `Dispatchers.resetMain()` を呼びます。テスト本体は素の `runTest` です。

## Arrange をまとめる private helper

正常系の stub が 5〜6 個必要になるため、`arrangePendingUploads()` のような private helper に default 引数付きでまとめます。
異常系は default を 1 つだけ差し替えて表現します（`contentType = null`、`localUri = null` など）。

`UploadMediaWorkerTest` は生成関数を 2 つ持ちます。`createUploadRecord()` は `cloudStoragePath` が入った record、
`createPendingRecord()` は `cloudStoragePath` が `null` の record を返します。
S3 アップロード済みかどうかで Worker の分岐が変わるため、この 2 つを使い分けます。

## assert するもの

戻り値は `ListenableWorker.Result.success()` / `Result.retry()` / `Result.failure()` と `assertEquals` で比較します。

状態更新は MockK の `slot<T>()` と `capture(slot)` で保存引数を捕捉し、`slot.captured` の中身を assert します。
`localRepository.saveUploadRecords(capture(slot))` を `just runs` で stub し、
`slot.captured.first().syncStatus` が `SyncStatus.SYNCED` か `ERROR` かを見ます。
`CreateUploadRecordRequest` や `UploadSuccessCount` のように Worker が組み立てて渡す値も同じ方法で確認します。

戻り値と状態更新の両方を見る理由は、この Worker では一時エラーが `Result.retry()`、
永続エラーが `Result.success()` + `SyncStatus.ERROR` 保存という分岐だからです。
戻り値だけを見ると、永続エラーで record が ERROR に落ちていないケースを取りこぼします。

呼ばれないことの確認は `coVerify(exactly = 0) { ... }`、呼ばれた回数の確認は `coVerify(exactly = n) { ... }` です。
アップロード済み record に対して `uploadMedia` を呼び直さないこと、永続エラー時に S3 の後始末
（`deleteUploadedObject`）を行い一時エラー時には行わないこと、成功数が 0 のときや一時エラーがあるときに
完了通知 API を呼ばないこと、が既存の検証対象です。

## CancellationException

握り潰さない契約は `rethrows` という動詞のテストで確認します。
依存の stub を `throws CancellationException()` にし、`assertThrows<CancellationException> { worker.doWork() }` で assert します。
import は `kotlin.coroutines.cancellation.CancellationException` です。
catch-all で握り潰されていると `Result.retry()` が返り、このテストだけが落ちます。
