# Value object テスト

`feature/*/domain/src/test/.../valueobject/` に置く JUnit 5 のテストです。
`EmailTest`、`GridColumnCountTest`、`MediaIdTest`、`MediaUrlTest`、`MediaCreatedAtTest`、
`MediaUploadedAtTest`、`UploadSuccessCountTest` があります。

## クラスの形

対象は `of()` factory だけです。mock も `runTest` も dispatcher も `@OptIn` も使いません。
`@BeforeEach` も持たず、`@Test` が並ぶだけの class になります。
テスト名の対象関数名部分は `of` になり、`of returns ...` / `of throws ...` の形に揃います。

Arrange で生の入力を `raw` という名前の local val に置き、Act で `Email.of(raw)` を呼び、Assert で
`email.value` を比較する、という 3 段の形です。

## 正規化の検証

`of()` が trim などの正規化を行う場合、前後に空白を含む入力を渡して `value` が正規化後の値になることを確認します。
value object 自体の等価比較ではなく、`value` プロパティを比較します。

## 例外の検証

例外は `org.junit.jupiter.api.assertThrows` の reified 版を import して `assertThrows<IllegalArgumentException> { }` で確認します。
`Assertions.assertThrows` ではありません。

`of()` が複数の `require` を持ち、それぞれ別の message を出す場合は、`assertThrows` の戻り値を val に受け、
`ex.message` まで assert します。`Email` は blank 判定と format 判定で別の message を返すため、
型だけの assert では「blank を弾いたつもりが format 側で落ちている」ケースを区別できません。
このとき Act に `assertThrows` を置き、Assert に message の比較を置くので、AAA の 3 ラベルがそのまま使えます。

message を持たず例外型だけを保証する場合は、`assertThrows` の戻り値を受けずに `// Act & Assert` の 1 ラベルで書きます。

## 数値の境界値

`GridColumnCount` のように上下限を持つ value object では、境界を生の数字ではなく production 側の定数
（`GridColumnCount.MIN` と `GridColumnCount.MAX`）から導出します。
下限超えは `MIN - 1`、上限超えは `MAX + 1` と書き、上下限を変えたときにテストが自動で追従するようにします。

`GridColumnCount` が持つケースは、範囲内の代表値、`MIN`、`MAX`、`MIN - 1`、`MAX + 1`、`0`、負値です。
`0` と負値は `MIN - 1` と結果が重なることがありますが、意味の異なる入力として別テストに残します。

## 新しい value object を追加するとき

正規化があるか（trim、lowercase、prefix 除去）、空文字や blank をどう扱うか、上下限や桁数の制約があるか、
不正入力で throw するか `null` を返すか、を production 側の `of()` を読んで確認します。
実際に存在する分岐だけをテストし、存在しない制約のテストは書きません。

命名の許可動詞は `../../../../docs/testing-conventions.md` を参照してください。
