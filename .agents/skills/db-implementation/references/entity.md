# Entity / Entity Mapper

`upload_records` テーブルの `UploadRecordEntity` と、domain `UploadRecord` との双方向変換を担う `UploadRecordEntityMapper`。保存値と null 許容列の意味、未知 status の扱いは [`docs/data-model.md`](../../../../docs/data-model.md) を正本とする。

## 確認する対象

- [`UploadRecordEntity.kt`](../../../../feature/media/data/src/main/kotlin/com/appvoyager/cloudphotos/data/media/db/entity/UploadRecordEntity.kt)
- [`UploadRecordEntityMapper.kt`](../../../../feature/media/data/src/main/kotlin/com/appvoyager/cloudphotos/data/media/db/UploadRecordEntityMapper.kt)
- [`UploadRecordLocalDataSourceImpl.kt`](../../../../feature/media/data/src/main/kotlin/com/appvoyager/cloudphotos/data/media/datasource/UploadRecordLocalDataSourceImpl.kt)
- [`docs/data-model.md`](../../../../docs/data-model.md) の「`upload_records` テーブルと Domain 対応」「保存値の互換性と Entity Mapper の契約」

## 実装時の注意

- **必須制約**:
  - Entity のフィールドは Room がそのまま扱えるプリミティブと `String`（`String?`）で定義し、domain の value object や `@TypeConverter` を Entity に直接持ち込まない（DB 読み出し時に古い行がコンストラクタ検証で読めなくなるのを防ぐため）。
  - domain との変換は `UploadRecordEntityMapper` の `toDomain` / `toEntity` に集約する。`toDomain` で `syncStatus` を復元する際は `SyncStatus.valueOf` を使わず、未知の文字列を `SyncStatus.ERROR` へフォールバックさせる契約を維持する。
  - `cloudStoragePath` などの非 status 列へ空文字列を書き込むと `toDomain` の `CloudStoragePath.of()` で `IllegalArgumentException` になるため、未確定値は必ず `null` として扱う。
- **変更時の波及先**:
  - Entity の列・型・nullability を変更した場合は、以下を同時に更新する。
    1. `UploadRecordEntityMapper` の `toDomain` / `toEntity`（往復の可逆性を保つ）
    2. `UploadRecordDao` の `@Query` 文字列（列名を指定している箇所）
    3. `CloudPhotosDatabase` の `version` と migration（[`migration.md`](migration.md) を参照）
    4. テストのレコード生成ヘルパー（`UploadRecordDaoInstrumentedTest` の `createRecord` や `testutil/` の fixture）

## テスト判断

- `UploadRecordEntityMapper` の往復変換（`toEntity` → `toDomain`）や未知 status の `SyncStatus.ERROR` フォールバック、nullable 列の境界は JVM Unit Test で検証する。
- Entity の列変更を伴う場合は `UploadRecordDaoInstrumentedTest` と migration テストの要否も判断する（[`docs/testing.md`](../../../../docs/testing.md) を参照）。

## 検証観点

- `toEntity` と `toDomain` の両方が更新され、往復で値が欠落・変質しないか。
- 未知の `syncStatus` 文字列で例外が飛ばず `SyncStatus.ERROR` に隔離されるか。
- スキーマ変更時に DB version と migration の対応が漏れていないか。
