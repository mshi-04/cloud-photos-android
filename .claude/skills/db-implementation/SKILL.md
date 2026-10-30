---
name: db-implementation
description: Room のテーブル構造、カラム、DAO クエリ、保存値の互換性、DB バージョンや migration を変更する依頼で使う。Cloud Photos の Room DB（feature:media:data の CloudPhotosDatabase、UploadRecordEntity、UploadRecordDao、UploadRecordEntityMapper、および app の DatabaseModule）を担当する。DataStore 設定や REST API のみの変更、画面表示のみの変更は対象外。
---

# 目的

Cloud Photos の Room DB（`upload_records` テーブル、DAO、Entity Mapper、Hilt `DatabaseModule`）を、既存の保存値契約と互換性を保ちながら安全に変更する。

# 条件別の参照先

全技術文書を一律に読まず、変更内容に該当する文書・節だけを確認する。

- `upload_records` のスキーマ、domain 値との対応、DAO 契約、`SyncStatus` 文字列互換性、DB version / migration 判断を確認するとき: [`docs/data-model.md`](../../../docs/data-model.md)（必須）
- `SyncStatus` の業務上の状態遷移や同期フローへの影響を確認するとき: [`docs/media-sync.md`](../../../docs/media-sync.md)
- モジュール間の Room 依存分担やローカル検証コマンドを確認するとき: [`docs/development-setup.md`](../../../docs/development-setup.md)
- DAO の Instrumented Test、契約ガードテスト、migration テストの書き方を確認するとき: [`docs/testing.md`](../../../docs/testing.md)
- 対象モジュールの実装とテスト:
  - [`feature/media/data/src/main/kotlin/com/appvoyager/cloudphotos/data/media/db/`](../../../feature/media/data/src/main/kotlin/com/appvoyager/cloudphotos/data/media/db/)
  - [`app/src/main/kotlin/com/appvoyager/cloudphotos/di/DatabaseModule.kt`](../../../app/src/main/kotlin/com/appvoyager/cloudphotos/di/DatabaseModule.kt)
  - [`feature/media/data/src/test/kotlin/com/appvoyager/cloudphotos/data/media/db/`](../../../feature/media/data/src/test/kotlin/com/appvoyager/cloudphotos/data/media/db/)
  - [`feature/media/data/src/androidTest/kotlin/com/appvoyager/cloudphotos/data/media/db/`](../../../feature/media/data/src/androidTest/kotlin/com/appvoyager/cloudphotos/data/media/db/)

# 役割クラスと参照先

変更対象の役割を見分け、該当する reference を読んでから実装する。スキーマ変更時は 3 つすべてを一体で確認する。

| 役割 | 参照 |
| --- | --- |
| Entity / Entity Mapper（`UploadRecordEntity`・`UploadRecordEntityMapper`） | [`references/entity.md`](references/entity.md) |
| DAO（`UploadRecordDao`） | [`references/dao.md`](references/dao.md) |
| Database / Migration / DI（`CloudPhotosDatabase`・`DatabaseModule`） | [`references/migration.md`](references/migration.md) |

# 手順

1. [`docs/data-model.md`](../../../docs/data-model.md) と該当する reference を読み、変更が DB スキーマ変更（version 更新）を伴うか、DAO クエリのみの変更かを判定する。
2. スキーマ変更を伴う場合は、Entity・`UploadRecordEntityMapper`・DAO・`CloudPhotosDatabase`・`DatabaseModule`・migration 方針（既存データの保持か破棄か、必要に応じた `exportSchema = true` の導入）を一体で計画する。
3. reference の実装時の注意と波及先に沿って変更する。
4. [`test-implementation`](../test-implementation/SKILL.md)（および [`docs/testing.md`](../../../docs/testing.md)）に沿って、Mapper の JVM Unit Test、DAO の `UploadRecordDaoSyncStatusTest`（JVM）と `UploadRecordDaoInstrumentedTest`（`androidTest`）、必要に応じた migration テストを追加・更新する。
5. 変更範囲に応じたローカル検証を [`docs/development-setup.md`](../../../docs/development-setup.md) の「ローカル検証の選び方とコマンド」から選んで実行する。
6. 変更内容、実行した検証と結果、未実施の検証（実機/エミュレータ未接続時の `connectedDebugAndroidTest` など）と理由を簡潔に報告する。

# 注意事項

- Cloud Photos の現行 `CloudPhotosDatabase` は `version = 1, exportSchema = false` であり、保存済み schema JSON や schema コピータスクは未導入である。未導入の仕組みを存在するものとして扱わない。
- `DatabaseModule` には destructive migration のフォールバックが設定されていないため、migration なしで `version` だけを上げると既存インストールが起動時クラッシュする。
- DataSource や Repository 実装、Worker へ変更が波及する場合は [`data-implementation`](../data-implementation/SKILL.md) を併用する。
