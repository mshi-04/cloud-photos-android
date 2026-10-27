# Value Object

検証済みまたは意味付けされた単一値を表す `@JvmInline value class` を `feature/<name>/domain/.../valueobject/` に置く。

## 確認する対象

- [`feature/auth/domain/src/main/kotlin/com/appvoyager/cloudphotos/domain/auth/valueobject/`](../../../../feature/auth/domain/src/main/kotlin/com/appvoyager/cloudphotos/domain/auth/valueobject/)（`Email.kt`、`Password.kt`、`ConfirmationCode.kt`、`UserId.kt`）
- [`feature/media/domain/src/main/kotlin/com/appvoyager/cloudphotos/domain/media/valueobject/`](../../../../feature/media/domain/src/main/kotlin/com/appvoyager/cloudphotos/domain/media/valueobject/)（`MediaId.kt`、`MediaUrl.kt`、`CloudStoragePath.kt`、`GridColumnCount.kt`、`MediaCreatedAt.kt`、`MediaUploadedAt.kt`、`IsDeleted.kt` ほか）

## 実装時の注意

- **必須制約**:
  - `@JvmInline value class Xxx private constructor(val value: T)` とし、`companion object` の `fun of(raw: T): Xxx` を唯一の生成経路にする。
  - `of` の処理順は「正規化（`String` の `trim()` など）→ `require` による検証 → コンストラクタ呼び出し」とする。
  - 検証条件が複数ある場合（例: `Email` の空チェックと形式チェック、`ConfirmationCode` の空チェックと 6 桁数字チェック）は 1 つの `require` に `&&` でまとめず、条件ごとに `require` を分けてどの条件で失敗したか判別できるようにする。
  - Domain 層に `android.net.Uri` や `android.util.Patterns` を持ち込まない。`MediaUrl` のように URI / URL 妥当性を判定する場合は `java.net.URI` / `URL` を用い、`content` scheme の扱いを含めて純粋 Kotlin / JDK で完結させる。
  - data / UI との境界（mapper、DTO 組み立て、DB 書き込み、UI 表示）まで value object のまま運び、途中で `.value` の生プリミティブに戻さない。
- **プロジェクト固有の設計**:
  - 検証条件を持たない値（`IsDeleted`）や、同じ検証条件の `Long`（`MediaCreatedAt` と `MediaUploadedAt`）も、引数順の取り違えをコンパイル時に防ぐため別々の value object にしている。
  - UI 設定画面などで共有する境界値は `GridColumnCount.MIN` / `MAX` のように `const val` で公開する。
- **波及先**:
  - `of()` の検証条件を厳しくする場合は、DB やリモート API から既存データを復元する mapper（`UploadRecordEntityMapper`、`RemoteUploadRecordMapper`）で既存レコードが `IllegalArgumentException` で落ちないか確認する。
  - 認証入力の value object（`Email` / `Password` / `ConfirmationCode`）の検証条件を変える場合は、[`docs/authentication.md`](../../../../docs/authentication.md) と UI 側の `ValidationUtils.kt`・auth ViewModel の `AuthFieldError` 変換との整合を確認する。

## テスト判断

- 新規・変更した value object は `src/test/.../valueobject/XxxTest` で有効値・正規化（前後空白除去）・境界値・無効値（`assertThrows<IllegalArgumentException>`）を網羅する（[`docs/testing.md`](../../../../docs/testing.md) を参照）。

## 検証観点

- `private constructor` になっており、`of()` を通さずに未検証の値を生成する経路がないか。
- 既存の保存データ・テスト fixture（`testutil/`）が新しい検証条件と矛盾しないか。
