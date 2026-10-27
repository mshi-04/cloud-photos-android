# Result / Error / SyncStatus

provider 由来の失敗を表す `AuthResult` / `UploadResult`・`AuthError` / `UploadError` と、同期状態を表す `SyncStatus` の構造と波及先。Result と例外の使い分けは [`docs/implementation-guidelines.md`](../../../../docs/implementation-guidelines.md) の「エラー処理と CancellationException」を正本とする。

## 確認する対象

- Auth: [`AuthResult.kt`](../../../../feature/auth/domain/src/main/kotlin/com/appvoyager/cloudphotos/domain/auth/model/AuthResult.kt)、[`AuthError.kt`](../../../../feature/auth/domain/src/main/kotlin/com/appvoyager/cloudphotos/domain/auth/model/AuthError.kt)、[`docs/authentication.md`](../../../../docs/authentication.md)
- Media: [`UploadResult.kt`](../../../../feature/media/domain/src/main/kotlin/com/appvoyager/cloudphotos/domain/media/model/UploadResult.kt)、[`UploadError.kt`](../../../../feature/media/domain/src/main/kotlin/com/appvoyager/cloudphotos/domain/media/model/UploadError.kt)、[`SyncStatus.kt`](../../../../feature/media/domain/src/main/kotlin/com/appvoyager/cloudphotos/domain/media/model/SyncStatus.kt)、[`docs/media-sync.md`](../../../../docs/media-sync.md)、[`docs/data-model.md`](../../../../docs/data-model.md)

## 実装時の注意

- **必須制約**:
  - `AuthError` / `UploadError` は provider の例外型名ではなく、呼び出し側（UI の表示分岐や Worker の retry/永続失敗判定）が必要とする単位で定義する。
  - domain の Error 型に `cause: Throwable` や SDK 固有型を持たせない（Amplify 等の例外型が domain へ漏れるのを防ぐため）。想定外の例外を受ける受け皿として `Unknown` を用意する。
  - `SyncStatus` の enum 名（`PENDING_UPLOAD` / `SYNCED` / `PENDING_DELETE` / `ERROR`）は Room の `upload_records.syncStatus` 保存文字列および `UploadRecordDao` の SQL リテラルと直結しているため、安易にリネームしない。
- **変更時の波及先**:
  - **`AuthError` を変更した場合**:
    - Data: [`AuthErrorMapper.kt`](../../../../feature/auth/data/src/main/kotlin/com/appvoyager/cloudphotos/data/auth/util/AuthErrorMapper.kt)
    - UI: `LoginViewModel`、`ForgotPasswordViewModel`、`ResetPasswordViewModel`、`VerificationCodeViewModel`、および必要に応じた `AuthSnackbarMessage` / `AuthFieldError`
    - 契約: [`docs/authentication.md`](../../../../docs/authentication.md) のアカウント列挙防止ルール
  - **`UploadError` を変更した場合**:
    - Data: [`UploadErrorMapper.kt`](../../../../feature/media/data/src/main/kotlin/com/appvoyager/cloudphotos/data/media/util/UploadErrorMapper.kt)、[`UploadMediaWorker.kt`](../../../../feature/media/data/src/main/kotlin/com/appvoyager/cloudphotos/data/media/worker/UploadMediaWorker.kt)（永続失敗と retry の振り分け）
  - **`SyncStatus` を変更した場合**:
    - Domain: `DeleteMediaUseCase`、`PrepareUploadQueueUseCase`、`RecordMediaUploadUseCase`
    - Data / DB: [`UploadRecordEntityMapper.kt`](../../../../feature/media/data/src/main/kotlin/com/appvoyager/cloudphotos/data/media/db/UploadRecordEntityMapper.kt)、[`UploadRecordDao.kt`](../../../../feature/media/data/src/main/kotlin/com/appvoyager/cloudphotos/data/media/db/dao/UploadRecordDao.kt)、[`RemoteUploadRecordMapper.kt`](../../../../feature/media/data/src/main/kotlin/com/appvoyager/cloudphotos/data/media/util/RemoteUploadRecordMapper.kt)、`UploadMediaWorker`、`DeleteMediaWorker`
    - 契約: [`docs/media-sync.md`](../../../../docs/media-sync.md) と [`docs/data-model.md`](../../../../docs/data-model.md)

## テスト判断

- `AuthResult` 等の拡張関数を変更した場合は domain の Unit Test を更新する。
- `AuthError` / `UploadError` / `SyncStatus` のバリアントを変更した場合は、波及先の Mapper テスト、ViewModel テスト、Worker テスト、DAO 契約テスト（`UploadRecordDaoSyncStatusTest`）を合わせて更新する。

## 検証観点

- 新しいエラーバリアントを追加した際、Mapper 側と UI / Worker 側の `when` 分岐が網羅されているか。
- `SyncStatus` 変更時に既存 DB 行の復元（未知 status が `ERROR` に落ちる挙動）や未同期抽出 SQL との整合が保たれているか。
