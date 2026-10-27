# Mapper

provider の例外、SDK enum、REST API の JSON レスポンス、Room Entity を domain model / error へ変換する純粋関数群。

## 確認する対象

- Auth: [`AuthErrorMapper.kt`](../../../../feature/auth/data/src/main/kotlin/com/appvoyager/cloudphotos/data/auth/util/AuthErrorMapper.kt)、[`AuthSignInStepMapper.kt`](../../../../feature/auth/data/src/main/kotlin/com/appvoyager/cloudphotos/data/auth/util/AuthSignInStepMapper.kt)、[`docs/authentication.md`](../../../../docs/authentication.md)
- Media: [`UploadErrorMapper.kt`](../../../../feature/media/data/src/main/kotlin/com/appvoyager/cloudphotos/data/media/util/UploadErrorMapper.kt)、[`RemoteUploadRecordMapper.kt`](../../../../feature/media/data/src/main/kotlin/com/appvoyager/cloudphotos/data/media/util/RemoteUploadRecordMapper.kt)、[`UploadRecordEntityMapper.kt`](../../../../feature/media/data/src/main/kotlin/com/appvoyager/cloudphotos/data/media/db/UploadRecordEntityMapper.kt)
- 同期・DB 契約: [`docs/media-sync.md`](../../../../docs/media-sync.md)、[`docs/data-model.md`](../../../../docs/data-model.md)

## 実装時の注意

- **必須制約**:
  - mapper は状態や外部依存（`Clock` 等）を持たない `object`（原則 `internal object`）とし、時刻などの外部値は `fallbackUploadedAt` のように引数で受け取る。
  - domain の値オブジェクトへ変換する際は必ず `MediaId.of(...)` 等のファクトリを通す。
  - mapper 内で `CancellationException` を握り潰さない（呼び出し元の DataSource 側で mapper 呼び出し前に再スローする前提を守る）。
- **プロジェクト固有の契約**:
  - **認証・アップロード例外の分類**: `AuthErrorMapper` の `ServiceException.cause` 分岐やアカウント列挙防止のための分類は [`docs/authentication.md`](../../../../docs/authentication.md)、`UploadErrorMapper` の分類は [`docs/media-sync.md`](../../../../docs/media-sync.md) に従う。型で判別できるものは型分岐を優先し、message 文字列判定は型で区別できない場合に限定する。
  - **未知の enum 値のフォールバック**: `AuthSignInStepMapper` は未知の SDK step を `SignInStep.UNKNOWN` へ、`UploadRecordEntityMapper.toDomain` は未知の `syncStatus` 文字列を `SyncStatus.ERROR` へ倒し、例外で全体を止めない（[`docs/data-model.md`](../../../../docs/data-model.md) を参照）。
  - **リモート同期一覧の部分失敗許容**: `RemoteUploadRecordMapper.fromFetchResponse` は `records` 配列の各要素変換を `runCatching { ... }.getOrNull()` + `mapNotNull` で包み、不正な 1 件だけをスキップして残りの正常レコードを返す。一方、外側の `JSONObject` 解析や `records` 配列自体の欠落は包まず例外を伝播させる。
- **波及先**:
  - `UploadRecordEntityMapper` を変更する場合は [`db-implementation`](../../db-implementation/SKILL.md) の [`entity.md`](../../db-implementation/references/entity.md) と [`docs/data-model.md`](../../../../docs/data-model.md) も合わせて確認する。

## テスト判断

- 例外分岐、JSON 変換（要素欠落時のスキップと全体不正時の例外を含む）、Entity 往復変換と未知 status の `ERROR` フォールバックを `src/test/.../XxxMapperTest` で検証する（[`docs/testing.md`](../../../../docs/testing.md) を参照）。

## 検証観点

- 新しい SDK 例外や JSON フィールドの欠落に対して意図通りの domain 値またはフォールバックになるか。
- mapper が外部の現在時刻や可変状態に直接依存していないか。
