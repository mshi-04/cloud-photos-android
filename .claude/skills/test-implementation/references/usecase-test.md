# UseCase テスト

`feature/*/domain/src/test/.../usecase/` に置く JUnit 5 のテストです。
代表例は `feature/auth/domain/src/test/kotlin/com/appvoyager/cloudphotos/domain/auth/usecase/SignInUseCaseTest.kt` と
`feature/media/domain/src/test/kotlin/com/appvoyager/cloudphotos/domain/media/usecase/GetMediaListUseCaseTest.kt` です。

## クラスの形

mock するのは domain の Repository interface だけで、UseCase は実体を組み立てます。

依存が 1 つで stub をテストごとに上書きするだけなら、private field の初期化子で `mockk<AuthRepository>()` と
`SignInUseCase(repository)` をそのまま作り、`@BeforeEach` を持ちません。
`@BeforeEach` で `mockk()` と UseCase を作り直す形もあり、こちらは `lateinit var` になります。どちらも既存にあります。

suspend で dispatcher を切り替える UseCase のテストでは class に `@OptIn(ExperimentalCoroutinesApi::class)` を付け、
`runTest(StandardTestDispatcher())` を使います。仮想時間の操作が不要なテストは素の `runTest` で書かれています。

## suspend UseCase の assert

`AuthResult` を返す UseCase では、期待値を `AuthResult.Success(...)` または `AuthResult.Error(AuthError.XXX(...))` として
Arrange で組み立て、`coEvery { repository.signIn(request) } returns expected` で stub し、
`assertEquals(expected, actual)` と `coVerify(exactly = 1) { repository.signIn(request) }` の両方を Assert に置きます。

戻り値だけを見ると、UseCase が Repository を複数回呼んでも通ってしまうため、回数の固定を対で書きます。

`AuthResult.Success` の中身がさらに分岐する場合（`SignInState.SignedIn` と `SignInState.MFARequired(step)`）は、
1 テスト 1 ケースに分けます。error 系も `AuthError` のサブタイプごとにテストを分けます。

## Flow を返す UseCase の assert

Flow を返す UseCase は `every { repository.getMediaListFlow() } returns MutableStateFlow(expected)` のように stub し、
`useCase().test { }` の Turbine ブロックで `awaitItem()` を assert します。AAA ラベルは `// Act & Assert` です。

`MutableStateFlow` のように完了しない Flow は `cancelAndConsumeRemainingEvents()` で購読を切ります。
`awaitComplete()` を待つと hang します。完了する Flow なら `awaitComplete()` を確認します。

## 例外の伝播

Repository の例外をそのまま流す契約は `rethrows` という動詞で書きます。

Flow の場合は stub を `flow { throw expected }` にし、Turbine ブロック内の `awaitError()` の戻り値を assert します。
suspend 関数の場合は `assertThrows<RuntimeException> { useCase(...) }` で確認します。
`assertThrows` は `org.junit.jupiter.api.assertThrows` の reified 版を import します。

`CancellationException` を握り潰さない契約がある UseCase では、`throws CancellationException()` を stub して
`rethrows` テストを追加します。catch-all で握り潰されていると、このテストだけが落ちます。

## 入力データ

request や value object は `testutil` の fixture 関数から作ります。domain 層の fixture は
`signInRequest()`、`validEmail()` のように suffix を持ちません。
model を組み立てる必要があるテストでは、テストクラス内に private の生成関数か
companion object の定数として置いている例もあります。
