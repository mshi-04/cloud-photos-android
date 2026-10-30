# DataSource / 具体 I/O

外部 SDK（Amplify Auth / Storage / API、Firebase Messaging）、Room DAO、DataStore、MediaStore / ContentResolver を直接扱う実装クラス。

## 確認する対象

- Core REST / Token: [`AmplifyRestClient.kt`](../../../../core/data/src/main/kotlin/com/appvoyager/cloudphotos/data/common/AmplifyRestClient.kt)、[`DeviceTokenDataSource.kt`](../../../../core/data/src/main/kotlin/com/appvoyager/cloudphotos/data/fcm/DeviceTokenDataSource.kt)
- Auth: [`AuthDataSourceImpl.kt`](../../../../feature/auth/data/src/main/kotlin/com/appvoyager/cloudphotos/data/auth/datasource/AuthDataSourceImpl.kt)、[`docs/authentication.md`](../../../../docs/authentication.md)
- Media DataSource: [`feature/media/data/src/main/kotlin/com/appvoyager/cloudphotos/data/media/datasource/`](../../../../feature/media/data/src/main/kotlin/com/appvoyager/cloudphotos/data/media/datasource/)（`LocalMediaDataSourceImpl.kt`、`UploadDataSourceImpl.kt`、`UploadRecordLocalDataSourceImpl.kt`、`UploadRecordRemoteDataSourceImpl.kt`、`SettingsDataSourceImpl.kt`）
- Media 具体 I/O: [`MediaStoreCapturedPhotoWriter.kt`](../../../../feature/media/data/src/main/kotlin/com/appvoyager/cloudphotos/data/media/datasource/MediaStoreCapturedPhotoWriter.kt)、[`ContentTypeResolver.kt`](../../../../feature/media/data/src/main/kotlin/com/appvoyager/cloudphotos/data/media/worker/ContentTypeResolver.kt)
- キャンセル伝播とエラー処理の基本: [`docs/implementation-guidelines.md`](../../../../docs/implementation-guidelines.md) の「Coroutines / Flow」「エラー処理と CancellationException」

## 実装時の注意

- **必須制約**:
  - DataSource interface や domain abstraction（`CapturedPhotoWriter`）のシグネチャは domain 型（またはプリミティブ）のみで宣言し、Amplify 等の SDK 型を公開しない。
  - `try/catch` や `runCatching` で例外を捕捉して mapper へ渡す前に、必ず `CancellationException` を再スローする（`if (e is CancellationException) throw e` または `rethrowIfCancellation()`）。
  - ブロッキング I/O（`ContentResolver.query` / `openInputStream` / `insert` / `openOutputStream` など）は `withContext(Dispatchers.IO)` 内で実行し、Stream や Cursor は `use` で確実にクローズする。
- **プロジェクト固有の契約**:
  - **SDK callback の橋渡しとキャンセル伝播**: Amplify API / Storage と Firebase Task は `suspendCancellableCoroutine` で橋渡しする。`Cancelable` な operation を返す `Amplify.Storage` や `Amplify.API`（`awaitAmplifyRestCall`）では `invokeOnCancellation { operation?.cancel() }` を繋ぐ。`Amplify.Auth` は `Cancelable` を返さないため待ち側の再開のみとなる（詳細は [`docs/authentication.md`](../../../../docs/authentication.md)・[`docs/media-sync.md`](../../../../docs/media-sync.md)）。
  - **REST 呼び出し**: バックエンド API 呼び出しは `core:data` の `awaitAmplifyRestCall` を使う。非 2xx 応答は `AmplifyRestException(statusCode, ...)` となり、`isNonServerError`（4xx 等）で retry 可否やエラー分類を判定する。
  - **サインアウト・退会の順序**: `AuthDataSourceImpl.signOut` / `deleteUser` における FCM cleanup・バックエンド削除・Cognito 操作の順序と失敗時の扱いは [`docs/authentication.md`](../../../../docs/authentication.md) の契約を厳守する。
  - **DataStore のテスト差し替え口**: `SettingsDataSourceImpl` は `internal constructor(private val dataStore: DataStore<Preferences>)` を主コンストラクタにし、`@Inject constructor(@ApplicationContext context: Context)` から委譲することでテストから任意の `DataStore` を注入できるようにしている。
  - **撮影画像の保存（`MediaStoreCapturedPhotoWriter`）**: Android Q（API 29）以上では `IS_PENDING = 1` で `MediaStore` に挿入し、ストリーム書き込み完了後に `IS_PENDING = 0` に更新する。ストリーム書き込みや `IS_PENDING` の更新に失敗した場合は、作成したレコードの `contentResolver.delete` を試み、`SavePhotoResult.Error`（容量不足は `STORAGE_FULL`、それ以外は `SAVE_FAILED`）を返す。`CancellationException` は再スローし、この経路ではレコードの削除を行わない。
- **波及先**:
  - 新しい DataSource や具体 I/O 実装を追加した場合は、`app/src/main/kotlin/com/appvoyager/cloudphotos/di/` の対応する Hilt module（`AuthDataSourceModule` / `UploadDataSourceModule` / `UploadRecordModule` / `MediaDataModule`）に binding を登録する。

## テスト判断

- REST / Room / S3 / Auth 呼び出しの変換と `CancellationException` 再スローは `src/test/.../datasource/XxxDataSourceImplTest`（および `core/data/src/test/`）で検証する。
- `DataStore<Preferences>` の実ファイル読み書きは `feature/media/data/src/androidTest/.../SettingsDataSourceInstrumentedTest` で検証する（[`docs/testing.md`](../../../../docs/testing.md) を参照）。

## 検証観点

- `CancellationException` が `AuthError.Unknown` や `UploadError.Unknown` に変換されてしまっていないか。
- `Cancelable` を返す SDK 操作で `invokeOnCancellation` の接続漏れがないか。
- `ContentResolver` の Cursor や Stream のクローズ漏れ、途中失敗時のゴミデータ残存がないか。
