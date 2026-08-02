# Mapper テスト

`feature/*/data/src/test/.../util/` に置く Mapper の JUnit 5 テストです。
`AuthErrorMapperTest`、`AuthSignInStepMapperTest`、`RemoteUploadRecordMapperTest`、`UploadErrorMapperTest` があります。

## クラスの形

Mapper は production 側が object なので、mock を一切使わず `AuthErrorMapper.map(throwable)` のように直接呼びます。
suspend でもないため、`runTest` も dispatcher も `@OptIn` も付けません。素の `@Test` だけで書きます。

`@BeforeEach` は基本的に持ちません。複数テストで共通の入力がある場合は、テストクラスの private val として
1 度だけ組み立てておき、テストごとに変える引数だけを呼び出し側に書きます。

Arrange に書くことがなく変換呼び出しがそのまま Act になる場合は、`// Arrange & Act` の 1 ラベルにまとめます。

## provider 例外を domain error へ変換する Mapper

`AuthErrorMapper` は Amplify の例外を `AuthError` へ写します。網羅すべき分岐は 3 種類あります。

例外型ごとの分岐（`SessionExpiredException` と `SignedOutException` → `InvalidCredentials`、
`ServiceException` → `Unknown`、`IOException` → `Network`）。
同じ例外型の中で message 内容によって結果が変わる分岐（`ValidationException` の message に code が含まれれば
`CodeMismatch`、含まれなければ `Unknown`）。
どの分岐にも当たらない型のフォールバック（`IllegalArgumentException` → `Unknown`）。

message で分岐する Mapper は、必ず「当たるケース」と「当たらないケース」を対で書きます。
片方だけでは条件式の反転を検出できません。

assert は error の型だけでなく message も含めた値の等価比較にします。
`AuthError` は message を保持する data class なので、`assertEquals` 1 回で型と message の両方が固定されます。
Amplify の例外は message と recovery suggestion の 2 引数を取るため、Arrange では両方を渡します。

## DTO / request から domain model を組み立てる Mapper

`RemoteUploadRecordMapper.fromCreateResponse` は API response の値と request を受け取り、`UploadRecord` を返します。

出力フィールドは 1 テスト 1 フィールドで assert します。model 全体の等価比較にはしません。
Mapper のテストは「どのフィールドの写し漏れか」を失敗メッセージから直接読めることが重要です。

見るべき点は、response の値が null のときに fallback 引数が使われること、
request から素通しされるフィールドが正しく写ること、Mapper が固定値を与えるフィールド
（`syncStatus` を `SYNCED`、`isDeleted` を `false`）がその値になることの 3 つです。

## timestamp

固定の timestamp を使い、現在時刻を呼びません。Mapper テストでは桁区切りの `1_700_000_000_000L`、
他の層では `1700000000000L` の表記が使われています。
