# Repository 実装テスト

`feature/*/data/src/test/.../repository/` に置く `XxxRepositoryImpl` の JUnit 5 テストです。
代表例は `feature/auth/data/src/test/kotlin/com/appvoyager/cloudphotos/data/auth/repository/AuthRepositoryImplTest.kt`、
`feature/media/data/src/test/kotlin/com/appvoyager/cloudphotos/data/media/repository/LocalUploadRecordsRepositoryImplTest.kt`、
`feature/media/data/src/test/kotlin/com/appvoyager/cloudphotos/data/media/repository/LocalMediaRepositoryImplTest.kt` です。

## クラスの形

mock するのは DataSource だけで、Repository は実体を組み立てます。

field を初期化子で 1 度だけ作る形（`mockk<AuthDataSource>()` と `AuthRepositoryImpl(dataSource)` を private val に持つ）と、
`@BeforeEach` で `mockk()` から作り直す形の 2 通りが既存にあります。

前者を選んだ場合は `@BeforeEach` に `clearAllMocks()` を置きます。mock instance がテスト間で共有されるため、
呼び出し記録が残ったままだと `coVerify(exactly = n)` が他テストの影響を受けます。後者ではこの呼び出しは不要です。

`runTest(StandardTestDispatcher())` と素の `runTest` の両方が使われています。
`StandardTestDispatcher` を渡す class には `@OptIn(ExperimentalCoroutinesApi::class)` を付けます。

## suspend API の assert

DataSource の戻り値を期待値として Arrange で組み立て、`coEvery { dataSource.signUp(request) } returns expected` で stub し、
Assert では `assertEquals(expected, actual)` と `coVerify(exactly = 1) { dataSource.signUp(request) }` を並べます。

Repository が状態に応じて別の DataSource API を呼び分ける実装では、呼ばれない側に対して
`coVerify(exactly = 0) { dataSource.signOut() }` を明示します。副作用がないことを契約として固定するためです。

戻り値が `Unit` の suspend 関数の stub には `just runs` を使います（`io.mockk.just` と `io.mockk.runs` を import）。
委譲だけの API では戻り値の assert がないため、`coVerify(exactly = 1)` が唯一の Assert になります。
このとき、引数が加工されずそのまま渡ることを見たいので、`any()` ではなく実際の値を書きます。

## Flow API の assert

Flow を返す Repository は Turbine の `repository.getMediaListFlow().test { }` で確認します。
`flow { }` builder で 1 回だけ emit して完了する実装なら、`awaitItem()` の後に `awaitComplete()` を置きます。
AAA ラベルは `// Act & Assert` です。

## 例外の伝播

同じデータを suspend 版と Flow 版の両方で公開している Repository では、正常系と例外伝播をそれぞれ両方に書きます。
suspend 版は `assertThrows<RuntimeException> { repository.getMediaList() }`、
Flow 版は Turbine ブロック内で `assertInstanceOf(RuntimeException::class.java, awaitError())` になります。
Flow builder の中で例外が起きると collect 時まで遅延するため、片方だけのテストでは伝播経路の違いを検出できません。

## 入力データ

data 層の fixture 関数は `signUpRequestFixture()` のように `Fixture` suffix を持ちます。
そのテストクラスでしか使わない model は、file 末尾に private `createXxx()` を置きます。
