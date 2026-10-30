# Repository 実装

domain の Repository interface を DataSource へ接続する `XxxRepositoryImpl` を `feature/<name>/data/.../repository/` に置く。Repository と DataSource の責務分担は [`docs/architecture.md`](../../../../docs/architecture.md) の「Repository / DataSource / Mapper」を正本とする。

## 確認する対象

- Auth: [`AuthRepositoryImpl.kt`](../../../../feature/auth/data/src/main/kotlin/com/appvoyager/cloudphotos/data/auth/repository/AuthRepositoryImpl.kt)
- Media: [`feature/media/data/src/main/kotlin/com/appvoyager/cloudphotos/data/media/repository/`](../../../../feature/media/data/src/main/kotlin/com/appvoyager/cloudphotos/data/media/repository/)（`LocalMediaRepositoryImpl.kt`、`LocalUploadRecordsRepositoryImpl.kt`、`RemoteUploadRecordsRepositoryImpl.kt`、`UploadRepositoryImpl.kt`、`SettingsRepositoryImpl.kt`）
- DI 配線: [`app/src/main/kotlin/com/appvoyager/cloudphotos/di/`](../../../../app/src/main/kotlin/com/appvoyager/cloudphotos/di/)（`AuthRepositoryModule.kt`、`UploadRecordModule.kt`、`UploadRepositoryModule.kt`、`MediaDataModule.kt`）

## 実装時の注意

- **必須制約**:
  - 戻り値・引数はすべて domain の型（`AuthResult<T>`、`UploadResult<T>`、`List<UploadRecord>`、`Flow<GridColumnCount>` など）とし、Amplify・Room・DataStore の型を漏らさない。
  - provider 例外の catch と `AuthResult` / `UploadResult` への変換は DataSource 側で行い、Repository 実装で二重に `try/catch` や `runCatching` を書かない。また、`upload_records` 系の例外は握り潰さずそのまま呼び出し元（UseCase / Worker）へ伝播させる。
  - local と remote の突き合わせや同期状態（`SyncStatus`）の遷移判断は UseCase / Worker に置き、Repository 実装へ持ち込まない。
- **既存実装の例（全クラスへの一律制約ではない）**:
  - 現行の多くの Repository 実装は対応する DataSource 1 つを注入して委譲する構成になっている。
  - `SettingsRepositoryImpl` は DataStore の格納型（`Int`）と domain の value object（`GridColumnCount`）の境界変換を担当する（既定値や 0 以下の無視など DataStore 固有の扱いは `SettingsDataSourceImpl` に残す）。
  - `LocalMediaRepositoryImpl.getMediaListFlow()` は DataSource の suspend 関数を `flow` builder で 1 回 emit する形になっている（Repository 内にポーリングの `while` ループ等は書かない）。
- **波及先**:
  - 新しい Repository 実装を追加した場合は、`app/src/main/kotlin/com/appvoyager/cloudphotos/di/` の対応する Hilt module に `@Binds @Singleton` を追加する。

## テスト判断

- Repository 実装を追加・変更した場合は、`src/test/.../repository/XxxRepositoryImplTest` で DataSource を mock 差し替えし、戻り値の一致と `coVerify(exactly = 1)`（および必要に応じた例外伝播）を確認する（[`docs/testing.md`](../../../../docs/testing.md) を参照）。

## 検証観点

- Repository と DataSource で例外変換が重複していないか。
- DI module（`app/src/main/.../di/`）への binding 登録が漏れていないか。
