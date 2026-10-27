# UseCase

1 操作 1 class の呼び出し単位として `feature/<name>/domain/.../usecase/` に置く。作成基準と省略条件は [`docs/architecture.md`](../../../../docs/architecture.md) の「UseCase と Domain Abstraction」を正本とする。

## 確認する対象

- [`feature/media/domain/src/main/kotlin/com/appvoyager/cloudphotos/domain/media/usecase/`](../../../../feature/media/domain/src/main/kotlin/com/appvoyager/cloudphotos/domain/media/usecase/)（`DeleteMediaUseCase.kt`、`PrepareUploadQueueUseCase.kt`、`SyncUploadRecordsUseCase.kt`、`GetPendingMediaUseCase.kt`、`RecordMediaUploadUseCase.kt` ほか）
- [`feature/auth/domain/src/main/kotlin/com/appvoyager/cloudphotos/domain/auth/usecase/`](../../../../feature/auth/domain/src/main/kotlin/com/appvoyager/cloudphotos/domain/auth/usecase/)
- [`feature/auth/domain/src/main/kotlin/com/appvoyager/cloudphotos/domain/auth/request/`](../../../../feature/auth/domain/src/main/kotlin/com/appvoyager/cloudphotos/domain/auth/request/) / [`feature/media/domain/src/main/kotlin/com/appvoyager/cloudphotos/domain/media/request/`](../../../../feature/media/domain/src/main/kotlin/com/appvoyager/cloudphotos/domain/media/request/)
- メディア同期の業務契約: [`docs/media-sync.md`](../../../../docs/media-sync.md)

## 実装時の注意

- **必須制約**:
  - クラス名は `<動詞><対象>UseCase` とし、`@Inject constructor` で Repository / Scheduler / `Clock`（`core:common`）を受け取る。
  - 公開メンバーは `operator fun invoke` 1 つだけとする。
  - 現在時刻が必要な場合は `System.currentTimeMillis()` を直接呼ばず、`core:common` の `Clock` interface を注入してテストで時刻を固定できるようにする。
  - 保存と scheduler 起動のように順序が意味を持つ一連の操作（例: `RecordMediaUploadUseCase`、`DeleteMediaUseCase`、`PrepareUploadQueueUseCase`）は、呼び出し側に分解させず UseCase 内にまとめる。
- **既存実装のパターン**:
  - 戻り値は操作の性質に応じて、(1) provider 越しの操作は `AuthResult<T>` / `UploadResult<T>`、(2) 継続監視は非 `suspend` の `Flow<T>`、(3) ローカル DB 更新や scheduler 起動は `suspend` の値または `Unit` を使い分ける。
  - 引数が複数ある場合は `request/` パッケージの data class にまとめることが多い（value object 2 個で完結する `RecordMediaUploadUseCase(mediaId, mediaUploadedAt)` のように直接並べる形も可）。
  - `feature/auth/domain` の既存 UseCase は Repository への 1 行委譲になっているが、新規 UseCase 追加時は [`docs/architecture.md`](../../../../docs/architecture.md) の基準（複数依存の調整・状態遷移・分割取得など）に照らして必要性を判断する。
- **波及先**:
  - メディア同期系 UseCase（`DeleteMediaUseCase` の未アップロード物理削除分岐、`SyncUploadRecordsUseCase` の pending 保護、`GetPendingMediaUseCase` の 900 件分割取得など）を変更する場合は、[`docs/media-sync.md`](../../../../docs/media-sync.md) の契約と Worker 側の前提が崩れないか確認する。

## テスト判断

- 新規・変更した UseCase は `src/test/.../usecase/XxxUseCaseTest` で正常系・分岐・早期 return・例外伝播を JVM Unit Test で検証する（[`docs/testing.md`](../../../../docs/testing.md) の「対象別のテストの書き方」を参照）。

## 検証観点

- 複数ステップの途中で早期 return すべき条件（空リスト、差分 0 件、`PENDING_UPLOAD` の削除など）で無駄な書き込みや scheduler 起動が走らないか。
- 時刻や外部状態がハードコードされず注入可能になっているか。
