# テスト規約

`AGENTS.md` から参照されます（すべてのルールの情報源）。
情報源の優先順位：`AGENTS.md` → フィーチャーローカルパターン → `CLAUDE.md` → `.agent/skills/*`。
このファイルと `AGENTS.md` が矛盾する場合は `AGENTS.md` を優先すること。
テストパターンのガイダンスは `.agent/skills/android-testing/SKILL.md` も参照。
このファイルとスキルがアノテーションルールや命名の詳細で矛盾する場合は、このファイルを優先すること。

---

## テストスタック

- JUnit 5
- MockK
- `kotlinx-coroutines-test`
- Arrange / Act / Assert 構造に従う。

## テスト関数の命名

すべてのテスト関数名はこの形式を厳密に使用しなければなりません：

```text
`[テスト対象の関数名] [期待される結果] when [条件]`
```

この形式は値オブジェクト、ユースケース、リポジトリ、マッパー、ワーカー、ViewModelなど
すべてのレイヤーに適用されます。

**各セグメントの定義：**

| セグメント             | ルール                                                                                           |
|------------------------|--------------------------------------------------------------------------------------------------|
| `テスト対象の関数名`   | テスト対象のKotlin関数、プロパティ、またはイベントハンドラの正確な名前。必ず最初に来る。         |
| `期待される結果`       | 許可された動詞のみを使用する動詞句（下記参照）。                                                 |
| `when [条件]`          | シナリオまたは入力状態。必ず含める — 省略しない。                                                |

**許可された動詞：** `returns` / `throws` / `sets` / `emits` / `calls` / `rethrows` / `ignores`

**命名の制約：**

- `test`、`should`、`verify` などのプレフィックスは禁止。
- 名前のどこにもsnake_caseは禁止。
- バッククォートで囲まれたテスト名内の識別子（例：`onSignIn`、`fetchMedia`）にcamelCaseは許可。
  シンボリックな名前（クラス名や型）には役立つ場合にPascalCase（例：`SignedInState`、`NetworkError`）を使用。
  条件や期待値を説明する自然言語部分にはスペース区切りの小文字を優先する。
- 日本語文字は禁止。
- 曖昧な結果を示す単語（`works`、`handles`、`correctly`、`properly`）は禁止。
- カテゴリラベル（`success case`、`failure case`、`happy path`、`error case`）は禁止。
- `success` や `failure` 単独の結果は禁止 — 具体的な型、状態、またはエフェクト名を書くこと
  （例：`returns SignedInState`、`returns NetworkError`）。
- 一つの関数名に複数の動作を含めることは禁止。
- `updates` は許可された動詞ではない。代わりに `sets` を使用すること。

**例：**

```kotlin
fun `invoke returns SignedInState when repository returns done state`()
fun `of throws IllegalArgumentException when email is blank after trim`()
fun `onSignIn emits NavigateToHome when credentials are valid`()
fun `onSignIn sets passwordError when password is blank`()
fun `invoke returns NetworkError when network is unavailable`()
fun `of returns Email when input is valid`()
```

## アノテーション

許可：`@Test`、`@BeforeEach`、`@AfterEach`、`@OptIn(ExperimentalCoroutinesApi::class)`、
`@ParameterizedTest`（`@ValueSource` / `@CsvSource` / `@MethodSource` と共に）、
`@ExtendWith`（外部ライブラリまたはカスタム拡張のJUnit拡張が必要な場合のみ — 下記注釈参照）。

禁止：`@DisplayName`（バッククォートの名前で十分）、`@Disabled`（修正するか削除する — 無効なテストをコミットしない）、
`@Nested`、`@Tag`、`@Timeout`、`@RepeatedTest`。

`@ExtendWith` とMockKに関する注記：MockKを使用する標準的な `*Test.kt` ファイルでは、
`mockk<>()` を直接呼び出すこと — `@ExtendWith(MockKExtension::class)` は不要であり推奨もされません。
`@ExtendWith` はJUnit拡張が本当に必要な場合（例：カスタムテストライフサイクル拡張や、
MockKに相当するものがないサードパーティライブラリ拡張）にのみ使用してください。

## 検証タイミング

| タイミング           | 実行者     | 内容                                                        |
|----------------------|------------|-------------------------------------------------------------|
| タスク完了時         | AIエージェント | `./gradlew ktlintCheck detekt` → 影響モジュールのテスト |
| プッシュ/PR作成      | 人間       | プッシュ、PR作成、マージ判断                                |
| PR/マージゲート      | CI         | lintチェック（ktlintCheck + detekt）+ 全ユニットテスト      |

テストを実行しなかった場合は、その旨を明示的に記載すること。

変更タイプ別のスコープを含む完全なポリシーは `docs/verification-policy.md` を参照。

## モジュールテストターゲット

```bash
./gradlew :feature:auth:domain:test
./gradlew :feature:auth:data:test
./gradlew :feature:auth:ui:test
./gradlew :feature:media:domain:test
./gradlew :feature:media:data:test
./gradlew :feature:media:ui:test
```
