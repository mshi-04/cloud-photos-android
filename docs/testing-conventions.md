# テスト規約

テストの書き方を定義します。実行範囲は `docs/verification-policy.md` を参照してください。

## スタック

- JUnit 5
- MockK
- Turbine
- `kotlinx-coroutines-test`
- Arrange / Act / Assert

既存テストに合わせ、テストだけ別スタイルにしません。

## 命名

形式:

```text
`[対象関数名] [期待結果] when [条件]`
```

例:

```kotlin
fun `invoke returns SignedInState when repository returns signed in user`()
fun `of throws IllegalArgumentException when email is blank after trim`()
fun `onSignIn emits NavigateToHome when credentials are valid`()
fun `doWork returns retry when upload fails with temporary error`()
```

許可する期待結果の動詞:

- `returns`
- `throws`
- `sets`
- `emits`
- `calls`
- `rethrows`
- `ignores`

禁止:

- `test`、`should`、`verify` prefix
- snake_case
- 日本語テスト名
- `works`、`handles`、`correctly`、`properly`
- `success case`、`failure case`、`happy path`、`error case`
- 曖昧な `success` / `failure`
- 未許可動詞の `updates`

## 構造

- Arrange、Act、Assertを空行で分ける。
- `// Arrange`、`// Act`、`// Assert` のAAAラベルを基本形にする。
- Turbineの `.test {}` などで操作と検証が交錯する場合は `// Act & Assert` を使う。
- 1テスト1理由にする。
- Mockは境界の依存だけに使う。
- Mapper、value object、domain modelは可能な限り実体でテストする。
- sleepや実時間待ちを使わない。

## Coroutines / Flow

- `runTest` を使う。
- TestDispatcherを注入できる設計にする。
- 基本は `StandardTestDispatcher` を使う。
- 即時実行が目的のときだけ `UnconfinedTestDispatcher` を使う。
- `advanceUntilIdle()` などで仮想時間を明示的に進める。
- `CancellationException` の契約は `rethrows` で確認する。
- Flow、StateFlow、SharedFlowのemit検証はTurbineを使う。
- emitの回数と順序は `awaitItem()` で1件ずつ確認する。
- 完了するFlowは `awaitComplete()` を確認し、継続するFlowやSharedFlowは `cancelAndConsumeRemainingEvents()` で購読解除する。
- StateFlowは初期値が即座に流れることを、必要に応じて最初の `awaitItem()` で確認する。
- Flowの例外契約は `awaitError()` または収集時の `assertThrows` で、期待する型と必要なメッセージを確認する。

例:

```kotlin
repository.getMediaListFlow().test {
    assertEquals(expectedMediaList, awaitItem())
    awaitComplete()
}
```

## テスト観点

新規追加または大幅刷新するテストでは、レビュー時に意図が分かるように観点コメントを置きます。
AAAラベルは残したまま、`// Act` または `// Act & Assert` の直下に `// 観点: 意図` の形で1行添えます。
複数観点が絡む場合は `Flow/Error` のように `/` で連結します。

```kotlin
// Flow: sign in success emits navigation effect
```

主な観点:

- `Normal`: 代表的な入力、期待される戻り値、状態遷移
- `Error`: 例外、エラーハンドリング、フォールバック
- `Boundary`: 空、null相当、0、上下限、1件/多数/重複
- `StateTransition`: 初期状態、LoadingからSuccess/Error、不正遷移なし、冪等性
- `Interaction`: MockKの引数、回数、順序、余計な呼び出しなし
- `Flow`: emit回数、順序、初期値、完了、購読解除
- `Coroutine`: suspend完了、仮想時間、キャンセル、Dispatcher差し替え

例:

```kotlin
@Test
fun `onSubmit emits NavigateToResetPassword when reset password succeeds`() = runTest(testDispatcher) {
    // Arrange
    viewModel.onEmailChanged("test@example.com")
    coEvery { resetPasswordUseCase(any()) } returns AuthResult.Success(Unit)

    // Act & Assert
    // Flow: reset password success emits navigation effect
    viewModel.effect.test {
        viewModel.onSubmit()
        advanceUntilIdle()
        assertEquals(expectedEffect, awaitItem())
        cancelAndConsumeRemainingEvents()
    }
}
```

## ViewModel

- 初期stateを確認する。
- event後のstate/effectを確認する。
- UseCase呼び出しの有無だけで終わらせない。
- 画面契約として見える結果をassertする。
- Main dispatcher差し替えは既存Ruleを使う。

## Domain

- UseCaseは成功、domain error、validation、キャンセルを必要に応じて検証する。
- Value objectは正規化、境界値、不正値を検証する。
- Repository interfaceをmockする場合は、domain contractとして意味のある戻り値にする。

## Data / Worker

- Repository実装はDataSource呼び出し、Mapper適用、domain resultを検証する。
- Mapperはprovider例外やDTO/Entity変換を検証する。
- Workerは `Result.success()` / `Result.retry()` / `Result.failure()` と状態更新を検証する。
- WorkManagerのunique name、constraints、input Dataを変える場合は契約をテストまたは報告する。

## Annotation

許可:

- `@Test`
- `@BeforeEach`
- `@AfterEach`
- `@OptIn(ExperimentalCoroutinesApi::class)`
- `@ParameterizedTest`
- `@ValueSource`
- `@CsvSource`
- `@MethodSource`
- `@ExtendWith`（本当にJUnit拡張が必要な場合のみ）

禁止:

- `@DisplayName`
- `@Disabled`
- `@Nested`
- `@Tag`
- `@Timeout`
- `@RepeatedTest`

MockKでは通常 `mockk<>()` を直接使います。`@ExtendWith(MockKExtension::class)` は原則不要です。

## 主なGradleターゲット

```bash
./gradlew :feature:auth:domain:test
./gradlew :feature:auth:data:test
./gradlew :feature:auth:ui:test
./gradlew :feature:media:domain:test
./gradlew :feature:media:data:test
./gradlew :feature:media:ui:test
```
