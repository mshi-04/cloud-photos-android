# Testing

この文書は Cloud Photos のテスト配置、命名規則、AAA と観点コメント、および対象別のテストの書き方をまとめます。
実行するタスクの選択とコマンドは [`docs/development-setup.md`](development-setup.md) を正本とします。

## テストスタックと配置の振り分け

| 区分 | 配置先 | スタック | 対象 |
| --- | --- | --- | --- |
| UnitTest | `src/test` | JUnit 5（Jupiter）、MockK、Turbine、`kotlinx-coroutines-test` | Value Object、UseCase、Repository 実装、DataSource、Mapper、ViewModel、Worker、SQL を実行しない契約ガード |
| InstrumentedTest | `src/androidTest` | JUnit 4（`org.junit.*`）、`androidx.test`、`room-testing`、`work-testing`、`kotlinx-coroutines-test` | Android SQLite 上の Room DAO、WorkManager Scheduler の enqueue 契約、DataStore の実ファイル読み書き |

- JVM で再現できるロジックはすべて `src/test` に置く。
- `src/androidTest` は `AndroidJUnitRunner` 上で動作するため JUnit 5 の annotation（`@BeforeEach` や `org.junit.jupiter.api.Test`）は実行されない。必ず JUnit 4 の `@Test`、`@Before`、`@After`、`org.junit.Assert.*` を使う。
- Kover のカバレッジ集計対象は `src/test` の UnitTest のみであり、`src/androidTest` を追加してもカバレッジ数値は変化しない。

## 命名規則

### `src/test`（JVM UnitTest）

スペースを含む backtick 関数名を使う。

```text
`[対象関数名] [期待結果] when [条件]`
```

例:

- ``fun `invoke returns SignedInState when repository returns signed in user`()``
- ``fun `of throws IllegalArgumentException when email is blank after trim`()``
- ``fun `onSignIn emits NavigateToHome when credentials are valid`()``
- ``fun `doWork returns retry when upload fails with temporary error`()``

許可する期待結果の動詞は次の 7 つのみとする。

- `returns` / `throws` / `sets` / `emits` / `calls` / `rethrows` / `ignores`

禁止事項:

- `test`、`should`、`verify` などの prefix
- `src/test` での snake_case や camelCase、日本語の関数名
- `works`、`handles`、`correctly`、`properly`、`success case`、`failure case`、`happy path` などの曖昧な表現
- 未許可動詞（`updates` など）

### `src/androidTest`（InstrumentedTest）

D8 / DEX の制約でスペースを含む backtick 名が使えないため、同じ意味構造を保った snake_case を使う。クラス名は `対象名 + InstrumentedTest` とする。

- `fun upsertAll_returnsStoredRecords_whenDatabaseIsEmpty()`
- `fun scheduleUpload_returnsSingleUploadWork_whenCalledTwice()`

## 構造（AAA ラベルと観点コメント）

- 1 テスト 1 理由とし、Arrange / Act / Assert を空行と `// Arrange`、`// Act`、`// Assert` ラベルで分ける。
- Turbine の `.test {}` や戻り値を受けない `assertThrows` のように操作と検証が 1 式で交錯する場合は `// Act & Assert`、Arrange が不要で変換呼び出しがそのまま Act になる Mapper 等では `// Arrange & Act` を使ってよい。
- 新規追加または大幅刷新するテストでは、`// Act` または `// Act & Assert` の直下に `// <観点>: <意図>` を 1 行添える。複数観点が絡む場合は `Flow/Error` のように `/` で連結する。
  - 主な観点: `Normal`、`Error`、`Boundary`、`State`、`StateTransition`、`Interaction`、`Flow`、`Coroutine`
- 許可する JUnit 5 annotation は `@Test`、`@BeforeEach`、`@AfterEach`、`@OptIn(ExperimentalCoroutinesApi::class)`、`@ParameterizedTest`（`@ValueSource` / `@CsvSource` / `@MethodSource`）、および本当に必要な場合の `@ExtendWith` のみとする。`@DisplayName`、`@Disabled`、`@Nested`、`@Tag`、`@Timeout`、`@RepeatedTest` は使わない。

## Coroutines / Flow / 例外伝播の共通パターン

- **Coroutines**:
  - `runTest` を使い、`sleep` や実時間待ちを使わない。
  - Dispatcher の制御や仮想時間進行が必要なテストでは `StandardTestDispatcher()` を使い、`advanceUntilIdle()` や `testScheduler.advanceTimeBy(...)` を明示する。
- **Flow（Turbine）**:
  - `flow.test { ... }` の中で `awaitItem()` により emit の順序と値を 1 件ずつ確認する。
  - 完了する Flow（`flow { emit(...) }`）は `awaitComplete()` を確認し、完了しない `StateFlow` / `SharedFlow` / `Channel` 由来の Flow は `cancelAndConsumeRemainingEvents()` で購読を解除する。
- **例外と `CancellationException`**:
  - suspend 関数の例外送出は `org.junit.jupiter.api.assertThrows<ExpectedException> { ... }`（reified 版）で検証し、Flow の例外は Turbine の `awaitError()` で検証する。
  - `CancellationException` を握り潰さない契約は、依存に `throws CancellationException()` を stub し、テスト名に `rethrows` を使って `assertThrows<CancellationException>` で検証する。

## テスト Fixture（`testutil`）

テスト用データ生成はモジュールの `testutil` パッケージに top-level 関数として置き、レイヤーを跨ぐ `testutil` の import は行わない。1 つのテストクラスでしか使わない生成関数はそのファイル末尾に `private fun createXxx()` として置く。

- **domain 層の fixture**（例: [`AuthDomainFixtures.kt`](../feature/auth/domain/src/test/kotlin/com/appvoyager/cloudphotos/domain/auth/testutil/AuthDomainFixtures.kt)）:
  - value object 単体は `validEmail()`、`validPassword()` のように `valid` prefix を付ける。
  - request や model は `signInRequest()`、`authUser()` のように suffix を付けず、引数は value object 型で受けて default 値に `validXxx()` を使う。
- **data 層の fixture**（例: [`AuthDataFixtures.kt`](../feature/auth/data/src/test/kotlin/com/appvoyager/cloudphotos/data/auth/testutil/AuthDataFixtures.kt)、[`UploadDataFixtures.kt`](../feature/media/data/src/test/kotlin/com/appvoyager/cloudphotos/data/media/testutil/UploadDataFixtures.kt)）:
  - `signInRequestFixture()`、`uploadMediaRequestFixture()` のように `Fixture` suffix を付ける。
  - 引数は生の `String` 等で受け、関数内で `Email.of(email)` のように変換する。
- 時刻は `System.currentTimeMillis()` を呼ばず固定値（`1700000000000L` など）を使う。

## 対象別のテストの書き方

### Value Object（例: [`EmailTest`](../feature/auth/domain/src/test/kotlin/com/appvoyager/cloudphotos/domain/auth/valueobject/EmailTest.kt)、[`GridColumnCountTest`](../feature/media/domain/src/test/kotlin/com/appvoyager/cloudphotos/domain/media/valueobject/GridColumnCountTest.kt)）

- Mock や `runTest` は使わず、`of()` factory に対して正規化（前後空白の `trim` など）、境界値（`GridColumnCount.MIN` / `MAX`、`MIN - 1`、`MAX + 1`）、不正入力の `assertThrows<IllegalArgumentException>` を検証する。
- 複数の `require` を持つ value object では、`assertThrows` の戻り値を受けて `ex.message` まで確認し、意図した制約で失敗したことを固定する。

### UseCase / Repository 実装（例: [`SignInUseCaseTest`](../feature/auth/domain/src/test/kotlin/com/appvoyager/cloudphotos/domain/auth/usecase/SignInUseCaseTest.kt)、[`AuthRepositoryImplTest`](../feature/auth/data/src/test/kotlin/com/appvoyager/cloudphotos/data/auth/repository/AuthRepositoryImplTest.kt)）

- 境界の依存（UseCase テストでは Repository / Scheduler interface、Repository 実装テストでは DataSource）だけを `mockk()` にし、対象クラスは実体で組み立てる。フィールド初期化子で mock を共有する場合は `@BeforeEach` で `clearAllMocks()` を呼ぶ。
- 戻り値の `assertEquals(expected, actual)` と、依存呼び出しの `coVerify(exactly = 1) { ... }`（および呼ばれない分岐での `coVerify(exactly = 0)`）を対で検証する。

### Mapper（例: [`AuthErrorMapperTest`](../feature/auth/data/src/test/kotlin/com/appvoyager/cloudphotos/data/auth/util/AuthErrorMapperTest.kt)、[`RemoteUploadRecordMapperTest`](../feature/media/data/src/test/kotlin/com/appvoyager/cloudphotos/data/media/util/RemoteUploadRecordMapperTest.kt)）

- Mock を一切使わず、例外インスタンスや JSON 文字列を直接渡して変換結果を検証する。
- 例外 message の内容で分岐する Mapper は「当たるケース」と「当たらないケース」を対で書く。
- DTO / JSON から domain model を組み立てる Mapper では、どのフィールドの写し漏れかがすぐ分かるよう出力フィールド単位、または null 時の fallback・固定値付与を明確に分けて検証する。

### ViewModel（例: [`LoginViewModelTest`](../feature/auth/ui/src/test/kotlin/com/appvoyager/cloudphotos/ui/auth/viewmodel/LoginViewModelTest.kt)、[`MediaViewModelTest`](../feature/media/ui/src/test/kotlin/com/appvoyager/cloudphotos/ui/media/viewmodel/MediaViewModelTest.kt)）

- `@BeforeEach` で `Dispatchers.setMain(testDispatcher)`、`@AfterEach` で `Dispatchers.resetMain()` を呼ぶ。`SavedStateHandle` は mock ではなく実体の `SavedStateHandle()` を渡す。
- **初期化時の Flow 購読**:
  - `init` で購読する UseCase の Flow は、ViewModel の生成前に `every { ... } returns flowOf(...)` などで stub する。テストごとに戻り値を変える場合は、生成を `private fun createViewModel()` に切り出す。非同期の初期化後の state を検証する際は、生成後に `advanceUntilIdle()` で初期化処理を進めてから assert する。
- **State の assert（private Snapshot）**:
  - 複数フィールドを持つ UiState では、無関係なフィールド追加で既存テストが壊れないよう、テストクラス内に比較対象フィールドだけを持つ `private data class XxxSnapshot`（デフォルト値と `companion object.from(state)` 付き）を定義して `assertEquals` する。1 フィールドや sealed class 分岐のみを見る場合は直接 assert してよい。
- **Effect の assert**:
  - `viewModel.effect.test { ... }` 内でイベント関数を呼び、`advanceUntilIdle()` → `awaitItem()` → `cancelAndConsumeRemainingEvents()` で検証する。UseCase 呼び出しの確認だけで終わらせず、画面契約として見える state / effect を必ず検証する。
- **二重実行ガード**:
  - `isLoading` や `isSigningOut` 中の二重実行防止は、UseCase stub を `delay` または `CompletableDeferred` で待機させ、1 回目の起動（必要なら `testScheduler.advanceTimeBy(1)`）後に 2 回目を呼んで `coVerify(exactly = 1)` で確認する。

### Worker（例: [`UploadMediaWorkerTest`](../feature/media/data/src/test/kotlin/com/appvoyager/cloudphotos/data/media/worker/UploadMediaWorkerTest.kt)、[`DeleteMediaWorkerTest`](../feature/media/data/src/test/kotlin/com/appvoyager/cloudphotos/data/media/worker/DeleteMediaWorkerTest.kt)、[`RegisterDeviceTokenWorkerTest`](../app/src/test/kotlin/com/appvoyager/cloudphotos/fcm/RegisterDeviceTokenWorkerTest.kt)）

- UnitTest では WorkManager を起動せず、Worker コンストラクタに直接 mock を渡して `worker.doWork()` を呼ぶ。`Context`、`WorkerParameters`、通知 helper などの Android 依存だけ `mockk(relaxed = true)` にし、Repository / DataSource は `relaxed` にしない。
- `ListenableWorker.Result.success()` / `Result.retry()` / `Result.failure()` の戻り値だけでなく、`slot<T>()` と `capture(slot)` を使ってローカル DB に保存された `SyncStatus`（`SYNCED` / `ERROR` / `PENDING_DELETE`）や S3 クリーンアップ・`completeUpload()` の呼び出し有無を合わせて検証する。

### InstrumentedTest（[`UploadRecordDaoInstrumentedTest`](../feature/media/data/src/androidTest/kotlin/com/appvoyager/cloudphotos/data/media/db/dao/UploadRecordDaoInstrumentedTest.kt)、[`MediaSchedulerInstrumentedTest`](../feature/media/data/src/androidTest/kotlin/com/appvoyager/cloudphotos/data/media/worker/MediaSchedulerInstrumentedTest.kt)、[`SettingsDataSourceImplInstrumentedTest`](../feature/media/data/src/androidTest/kotlin/com/appvoyager/cloudphotos/data/media/datasource/SettingsDataSourceImplInstrumentedTest.kt)）

- MockK は使わず、すべて実体で組み立て、`@After` で確実に後始末する。
  - **Room DAO**: `Room.inMemoryDatabaseBuilder(...).allowMainThreadQueries().build()` を `@Before` で作り、`@After` で `database.close()` する。空 ID リスト、不在 ID、削除の冪等性、upsert 置換、全 `SyncStatus` を投入した絞り込みを確認する。
    - 順序を保証しない query の結果は、期待値と実測値の両方を `sortedBy { it.mediaId }` で揃えて比較する。重複件数を検証する必要がない場合は `toSet()` で比較してもよい。
  - **WorkManager Scheduler**: `WorkManagerTestInitHelper.initializeTestWorkManager` で初期化し、`@After` で `workManager.cancelAllWork().result.get()` を呼び、キャンセル操作の完了を待ってから後始末を終える。`WorkInfo` と `WorkSpec` から unique work 件数、`ENQUEUED` 状態、Worker クラス名、`NetworkType.CONNECTED`、空の input Data、および 2 回呼び出し時に `KEEP` で初回 work ID が保たれることを確認する。
    - 現行の `MediaSchedulerInstrumentedTest` では、unique work 名から取得した `WorkInfo.id` を使い、`WorkManagerImpl.getInstance(context).workDatabase.workSpecDao().getWorkSpec(id.toString())` で `WorkSpec` を取得する。内部 API の利用はテスト内の private helper に閉じ込め、その helper に `@Suppress("RestrictedApi")` を付ける。
  - **DataStore**: テスト間の競合を避けるため `settings-test-${UUID.randomUUID()}` の一時ファイルと専用 `CoroutineScope` で `PreferenceDataStoreFactory.create` を組み立て、`@After` で `scope.cancel()` とファイル削除を行う。
