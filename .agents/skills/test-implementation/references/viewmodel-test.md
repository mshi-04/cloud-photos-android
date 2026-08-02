# ViewModel テスト

`feature/*/ui/src/test/.../viewmodel/` に置く JUnit 5 のテストです。
代表例は `feature/auth/ui/src/test/kotlin/com/appvoyager/cloudphotos/ui/auth/viewmodel/LoginViewModelTest.kt` と
`feature/media/ui/src/test/kotlin/com/appvoyager/cloudphotos/ui/media/viewmodel/MediaViewModelTest.kt` です。

## クラスの形

class 名は `対象ViewModel名 + Test`、class に `@OptIn(ExperimentalCoroutinesApi::class)` を付けます。
private field として `StandardTestDispatcher()` の `testDispatcher` と、UseCase を `mockk<T>()` で作ったものを持ちます。
ViewModel 自体は `lateinit var` で持ちます。

`@BeforeEach` では最初に `Dispatchers.setMain(testDispatcher)` を呼び、その後に ViewModel を実体で構築します。
`@AfterEach` では `Dispatchers.resetMain()` を呼びます。
`src/test` は JUnit 5 で `@Rule` が使えないため、Main dispatcher の差し替えは rule ではなくこの 2 つの callback が担います。

依存が多く、テストごとに mock の戻り値を変えたい ViewModel では、`@BeforeEach` では `mockk()` の生成と
`Dispatchers.setMain` だけを行い、ViewModel の構築は private `createViewModel()` に切り出します。
`init` で Flow を collect する ViewModel では、`createViewModel()` を呼ぶ前に collect 対象の UseCase を
`every { ... } returns flowOf(...)` で stub し、構築直後に `advanceUntilIdle()` を呼んで init の副作用を流し切ります。

`SavedStateHandle` を取る ViewModel には、mock ではなく空の `SavedStateHandle()` を渡します。

## state の assert

UiState 全体を直接 `assertEquals` しません。テストクラス内に private data class として
`対象UiState名 + Snapshot`（`LoginFormSnapshot` など）を定義し、比較したいフィールドだけを持たせます。
全フィールドに default 値を与え、companion object に `from(state: XxxUiState)` factory を置いて
UiState から snapshot へ詰め替えます。

assert は `assertEquals(LoginFormSnapshot(email = "..."), LoginFormSnapshot.from(state))` の形になり、
初期 state の検証は引数なしの `LoginFormSnapshot()` との比較で書けます。
UiState に検証対象外のフィールドが増えても既存テストが壊れないことがこの形の目的です。

1 フィールドだけ見れば足りるテストは `viewModel.uiState.value.passwordError` を直接 assert しても構いません。
enum や sealed class の分岐だけを見る場合は `assertTrue(state.screenState is MediaUiState.ScreenState.Success)` のように
型判定で書きます。

## effect の assert

effect は Turbine の `viewModel.effect.test { }` の中で確認します。
ブロック内で event 関数を呼び、`advanceUntilIdle()` で仮想時間を進め、`awaitItem()` で 1 件取り出して assert し、
最後に `cancelAndConsumeRemainingEvents()` で購読を解除します。effect は完了しない Flow なので `awaitComplete()` は使いません。
AAA ラベルは `// Act & Assert` になります。

`ShowSnackbar` のように effect が payload を持つ場合は、`(awaitItem() as MediaEffect.ShowSnackbar).message` へ
cast してから message の値を assert します。

## 二重実行のガード

`isLoading` や `isSigningOut` による二重実行防止は、UseCase の stub を `coAnswers` で遅延させ、
event 関数を 2 回呼んでから `advanceUntilIdle()` し、`coVerify(exactly = 1) { useCase(any()) }` で確認します。
遅延の作り方は 2 通りあります。単純に `delay(1000.milliseconds)` を挟む形と、`CompletableDeferred` を用意して
`coAnswers { deferred.await() }` で待たせる形です。

後者は 1 回目の呼び出し後に `testScheduler.advanceTimeBy(1)` を挟んでフラグの立ち上がりを跨いでから 2 回目を呼び、
その後 `deferred.complete(...)` で解放します。フラグを立てる coroutine が起動する前に 2 回目を呼ぶと
ガードを通過してしまうため、この 1 tick が必要です。

## 見落としやすい点

UseCase が呼ばれたことだけを `coVerify` して終えると、state と effect の変化を検証しないまま通ります。
画面契約として見える state か effect を必ず assert します。

命名規則、許可される期待結果の動詞、観点コメントの書式は `../../../../docs/testing-conventions.md` を参照してください。
