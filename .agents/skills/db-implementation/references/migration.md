# Database / Migration / DI

`CloudPhotosDatabase`（`feature:media:data`）と `DatabaseModule`（`app`）の構成、およびスキーマ変更・version 更新時の判断。DB 互換性と migration の基本方針は [`docs/data-model.md`](../../../../docs/data-model.md) の「データベース構成と DI 分担」「Schema 変更と Migration の判断」を正本とする。

## 確認する対象

- [`CloudPhotosDatabase.kt`](../../../../feature/media/data/src/main/kotlin/com/appvoyager/cloudphotos/data/media/db/CloudPhotosDatabase.kt)
- [`DatabaseModule.kt`](../../../../app/src/main/kotlin/com/appvoyager/cloudphotos/di/DatabaseModule.kt)
- Gradle 依存設定: [`feature/media/data/build.gradle.kts`](../../../../feature/media/data/build.gradle.kts)、[`app/build.gradle.kts`](../../../../app/build.gradle.kts)、[`docs/development-setup.md`](../../../../docs/development-setup.md)

## 実装時の注意

- **現状の前提（未導入の仕組みを実装済みとして扱わない）**:
  - `CloudPhotosDatabase` は現在 `version = 1, exportSchema = false` であり、schema JSON ファイルや schema export タスクは存在しない。
  - `DatabaseModule` の `Room.databaseBuilder` には `addMigrations(...)` も `fallbackToDestructiveMigration()` も設定されていない。
- **必須制約**:
  - Entity の構造を変更して `version` を上げる場合は、そのままでは既存インストールが起動時に `IllegalStateException` でクラッシュするため、実装前に以下を決定する。
    1. **既存データを保持する場合**: `Migration(startVersion, endVersion)` を実装して `DatabaseModule` の `.addMigrations(...)` に登録する。`MigrationTestHelper` によるテストを行う場合は `exportSchema = true` と KSP の `room.schemaLocation` 設定、schema JSON の管理を合わせて導入する。
    2. **既存データを破棄してよい場合**: `upload_records` を消すとアップロード済み判定が失われ再同期・再アップロードが走る影響を確認したうえで、`DatabaseModule` に destructive migration の設定を明示的に追加する。
- **波及先**:
  - `CloudPhotosDatabase` に新しい Entity や DAO を追加した場合は、`app` モジュールの `DatabaseModule` にも DAO の `@Provides` メソッドを追加する。
  - 別モジュールに Room Entity / DAO を新設する場合は、そのモジュールに `room.runtime` / `room.ktx` と `ksp(libs.androidx.room.compiler)` の依存が必要になる（[`docs/development-setup.md`](../../../../docs/development-setup.md) を参照）。

## テスト判断

- `Migration` を追加した場合は、`androidx.room.testing.MigrationTestHelper` を用いて旧 schema から新 schema への移行と既存行の保持を検証するテストを追加する（[`docs/testing.md`](../../../../docs/testing.md) を参照）。

## 検証観点

- `version` を上げた際、`DatabaseModule` に `addMigrations` または明示的なフォールバック設定が対で入っているか。
- 新規インストール時だけでなく、旧バージョン DB からのアップデート経路で起動時クラッシュしないか。
