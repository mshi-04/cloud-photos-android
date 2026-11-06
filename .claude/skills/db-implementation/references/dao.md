# DAO

Room の `@Dao` インターフェース（`UploadRecordDao`）。クエリの契約、空・不在時の振る舞い、`SyncStatus` 文字列リテラルの扱いは [`docs/data-model.md`](../../../../docs/data-model.md) の「UploadRecordDao の契約」を正本とする。

## 確認する対象

- [`UploadRecordDao.kt`](../../../../feature/media/data/src/main/kotlin/com/appvoyager/cloudphotos/data/media/db/dao/UploadRecordDao.kt)
- [`UploadRecordLocalDataSourceImpl.kt`](../../../../feature/media/data/src/main/kotlin/com/appvoyager/cloudphotos/data/media/datasource/UploadRecordLocalDataSourceImpl.kt)
- ガードテスト（JVM）: [`UploadRecordDaoSyncStatusTest.kt`](../../../../feature/media/data/src/test/kotlin/com/appvoyager/cloudphotos/data/media/db/dao/UploadRecordDaoSyncStatusTest.kt)
- Instrumented Test: [`UploadRecordDaoInstrumentedTest.kt`](../../../../feature/media/data/src/androidTest/kotlin/com/appvoyager/cloudphotos/data/media/db/dao/UploadRecordDaoInstrumentedTest.kt)

## 実装時の注意

- **必須制約**:
  - DAO の引数・戻り値は Entity（`UploadRecordEntity`）とプリミティブ / `String` のみとし、domain の value object や `SyncStatus` enum を直接受け取らない。
  - SQL 内に `SyncStatus` の文字列リテラル（`'PENDING_UPLOAD'`、`'PENDING_DELETE'` など）を埋め込むのは、複数 status を固定で絞るクエリ（`getPendingMediaIds`）のような必要な箇所だけに限定し、単一 status の検索（`getByStatus`）は呼び出し側の DataSource から `syncStatus.name` を引数で渡す。
  - SQL 内の status リテラルを変更・追加した場合は、必ず KDoc コメントと `UploadRecordDaoSyncStatusTest` の検証ケースを合わせて更新する。
- **呼び出し側との境界**:
  - `IN (:mediaIds)` クエリを大件数で呼ぶ経路では、SQLite のバインド変数上限を超えないよう呼び出し側（例: `PrepareUploadQueueUseCase` の 900 件 `chunked`）との分担を確認する。
  - 空リスト入力時の空リスト返却、不在 ID 指定時の空リスト返却、不在 ID 削除の冪等性を崩さない。
- **波及先**:
  - DAO のメソッドシグネチャや取得順を変更した場合は、`UploadRecordLocalDataSource` / `UploadRecordLocalDataSourceImpl`、`LocalUploadRecordsRepository`、および呼び出し元の UseCase / Worker への影響を確認する。

## テスト判断

- SQL の実行結果（`@Upsert` の挿入・更新、`IN` 句フィルタ、status フィルタ、削除の冪等性、空リスト入力）は `UploadRecordDaoInstrumentedTest`（`androidTest`）で検証する。
- SQL 内に埋め込んだ `SyncStatus` enum 名の存在保証は `UploadRecordDaoSyncStatusTest`（JVM Unit Test）で検証する（[`docs/testing.md`](../../../../docs/testing.md) を参照）。

## 検証観点

- SQL 内のテーブル名・カラム名・status リテラルにタイポがないか（`UploadRecordDaoInstrumentedTest` で実クエリを通しているか）。
- 新しい DAO を追加した場合、`CloudPhotosDatabase` と `app` の `DatabaseModule` に provider が追加されているか。
