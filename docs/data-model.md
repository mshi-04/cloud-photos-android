# Data Model

この文書は Room データベース（`CloudPhotosDatabase`）の保存構造、domain モデルとの対応、DAO の契約、および DB version / migration の判断基準をまとめます。
`SyncStatus` の業務上の意味と同期フロー全体の順序は [`docs/media-sync.md`](media-sync.md) を正本とします。

## データベース構成と DI 分担

- **Database 定義**: [`CloudPhotosDatabase`](../feature/media/data/src/main/kotlin/com/appvoyager/cloudphotos/data/media/db/CloudPhotosDatabase.kt)（`:feature:media:data`）
  - `entities = [UploadRecordEntity::class]`、`version = 1`、`exportSchema = false`。
  - `@TypeConverters` や `Migration` は現時点で定義していない。
- **DI 提供**: [`DatabaseModule`](../app/src/main/kotlin/com/appvoyager/cloudphotos/di/DatabaseModule.kt)（`:app`）
  - `Room.databaseBuilder` で DB ファイル名 `cloud_photos.db` を構築し、`CloudPhotosDatabase` と `UploadRecordDao` を `@Singleton` で提供する。
  - `fallbackToDestructiveMigration` も `addMigrations` も設定していない。
  - DB 定義は `:feature:media:data`、Hilt provider は `:app` にあるため、DAO の追加や builder 設定の変更時は `:app` 側の `DatabaseModule` も更新する。

## `upload_records` テーブルと Domain 対応

[`UploadRecordEntity`](../feature/media/data/src/main/kotlin/com/appvoyager/cloudphotos/data/media/db/entity/UploadRecordEntity.kt) は `@Entity(tableName = "upload_records")` を持つ唯一つのテーブルで、列はすべて Room が直接扱えるプリミティブと `String` で構成する。

| Entity 列 | 型 | 制約 / 意味 | 対応する `UploadRecord`（domain） |
| --- | --- | --- | --- |
| `mediaId` | `String` | `@PrimaryKey`。端末メディアの識別子 | `mediaId: MediaId` |
| `cloudStoragePath` | `String?` | nullable。S3 アップロード完了前は `null` | `cloudStoragePath: CloudStoragePath?` |
| `isDeleted` | `Boolean` | 削除フラグ | `isDeleted: IsDeleted` |
| `syncStatus` | `String` | `SyncStatus` の enum `name` 文字列 | `syncStatus: SyncStatus` |
| `uploadedAt` | `Long` | アップロードまたは記録時刻（epoch millis） | `mediaUploadedAt: MediaUploadedAt` |

Entity の列に value object を直接使わないのは、DB 読み出し時に `require` を伴う生成が走ると過去の行が読み出せず復旧不能になるためである。列名のずれ（`uploadedAt` ↔ `mediaUploadedAt`）を含め、変換はすべて [`UploadRecordEntityMapper`](../feature/media/data/src/main/kotlin/com/appvoyager/cloudphotos/data/media/db/UploadRecordEntityMapper.kt) に集約する。

## 保存値の互換性と Entity Mapper の契約

- **未知の `syncStatus` の隔離**:
  - `UploadRecordEntityMapper.toDomain` は `SyncStatus.valueOf()` を使わず、`enumValues<SyncStatus>()` を名前で検索し、一致しなければ `SyncStatus.ERROR` へ落とす。
  - 過去バージョンや将来の enum 変更で未知の文字列が DB に残っていても、読み出し例外で同期処理全体が止まるのを防ぎ、`ERROR` として自動同期対象から隔離する意図的な設計である。
  - 新しい `SyncStatus` を追加する場合は、旧バージョンのアプリがその行を `ERROR` として扱うことを前提に移行・復旧経路を判断する。
- **非 null / 形式制約**:
  - `cloudStoragePath` が非 null の場合は `CloudStoragePath.of()` の検証を通るため、空文字列を DB に書き込まないことは書き込み側の責務とする。

## `UploadRecordDao` の契約

[`UploadRecordDao`](../feature/media/data/src/main/kotlin/com/appvoyager/cloudphotos/data/media/db/dao/UploadRecordDao.kt) はすべて `suspend` 関数で構成される。

- **書き込み（`upsertAll`）**:
  - `@Upsert` を使い、同一 `mediaId` の行は置き換える。
- **SQL 内の `SyncStatus` 文字列リテラル**:
  - `getPendingMediaIds()` は `WHERE syncStatus IN ('PENDING_UPLOAD', 'PENDING_DELETE')` のように enum 名を SQL 文字列リテラルとして直接埋め込んでいる。
  - enum 名を変更しても SQL はコンパイルエラーにならず静かに 0 件を返すようになるため、[`UploadRecordDaoSyncStatusTest`](../feature/media/data/src/test/kotlin/com/appvoyager/cloudphotos/data/media/db/dao/UploadRecordDaoSyncStatusTest.kt) で enum 名の存在をガードし、実クエリの動作は [`UploadRecordDaoInstrumentedTest`](../feature/media/data/src/androidTest/kotlin/com/appvoyager/cloudphotos/data/media/db/dao/UploadRecordDaoInstrumentedTest.kt) で検証する。
  - 引数で渡せるクエリ（`getByStatus(status: String)`）には `SyncStatus.Xxx.name` を渡し、不要な SQL リテラル埋め込みを増やさない。
- **空・不在・削除の扱い**:
  - `getByIds(mediaIds)` に空リストや存在しない ID を渡した場合は例外にならず空リストが返る。1 件取得も 1 要素リストを渡して行う。
  - `deleteById(mediaId)` は対象行が存在しなくてもエラーにしない（冪等）。
  - `IN (:mediaIds)` に大量の ID を渡す呼び出し側（[`PrepareUploadQueueUseCase`](../feature/media/domain/src/main/kotlin/com/appvoyager/cloudphotos/domain/media/usecase/PrepareUploadQueueUseCase.kt)）では、SQLite のバインド変数上限を超えないよう 900 件ずつ `chunked` して呼び出す。

## Schema 変更と Migration の判断

Entity の列・型・主キー・制約を変更する場合、DB `version` を据え置くと Room の schema 検証が実行時に失敗する。また、`DatabaseModule` には destructive migration のフォールバックがないため、`version` を上げただけでも migration を与えないと既存インストールが起動直後にクラッシュする（新規インストールでは再現しないため注意する）。

DB schema を変更する際は、実装前に次のいずれかを選択して報告する。

1. **既存の `upload_records` を保持する**:
   - `Migration` を実装して `DatabaseModule` の `addMigrations(...)` に登録する。
   - `MigrationTestHelper` による migration test を書く場合は、`CloudPhotosDatabase` の `exportSchema = true` と Room schema 出力先の設定を導入する。
2. **既存のローカル状態を破棄する**:
   - `DatabaseModule` に明示的な destructive migration 設定を追加する。
   - `upload_records` を消去するとアップロード済み判定や pending 状態が失われ、再同期・再アップロードが発生するため、利用者への影響を報告する。

DAO の `@Query` だけの変更は schema に影響しないため、DB `version` は据え置く。
