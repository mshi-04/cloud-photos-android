# Entity

このリポジトリの Room Entity は `feature/media/data/src/main/kotlin/com/appvoyager/cloudphotos/data/media/db/entity/`
に置く。現存するのは `UploadRecordEntity` の 1 つだけで、これが記述の粒度の基準になる。

## 構造

`@Entity(tableName = "upload_records")` を付けた data class で、列は 5 つ。
`@PrimaryKey` を付けた `mediaId` の `String`、nullable な `cloudStoragePath` の `String?`、
削除フラグの `isDeleted` の `Boolean`、`SyncStatus` の enum NAME を保持する `syncStatus` の `String`、
epoch millis の `uploadedAt` の `Long`。

列はすべて Room がそのまま扱える primitive と `String` で、`TypeConverter` は 1 つも定義していない。
domain の `UploadRecord` は `MediaId` / `CloudStoragePath` / `IsDeleted` / `MediaUploadedAt` の
value object と `SyncStatus` enum を持つが、Entity 側はその表現を持ち込まない。
domain の `mediaUploadedAt` が Entity では `uploadedAt` になるように、名前が 1 対 1 で対応しない列がある。
対応関係は entity mapper が唯一の定義箇所になる。

value object をそのまま列にしないのは、`CloudStoragePath.of()` のような `require` を伴う生成が
DB 読み出し時に走ると、過去に書き込んだ行が読めなくなって復旧手段がなくなるため。
検証は domain へ寄せ、DB は素の値だけを持つ。

## null を許す列

`cloudStoragePath` だけが nullable。アップロード完了前のレコードにはクラウド側のパスが存在しない。
Entity に nullable 列を足すときは、mapper 側の null 分岐と、対応する domain フィールドの
nullability を同じ変更単位で合わせる。

## 変更時に一緒に動くもの

Entity の列・型・制約を変えた場合、以下が同時にずれる。

- `UploadRecordEntityMapper` の `toDomain` / `toEntity`
- `UploadRecordDao` の `@Query` 文字列（列名を直接書いている）
- `UploadRecordLocalDataSourceImpl` 経由の Repository 入出力
- テスト fixture（`UploadRecordDaoInstrumentedTest` の `createRecord` など）

さらに `@Database` の `version` を上げないと、Room の schema 検証が起動時に失敗する。
`DatabaseModule` は destructive migration のフォールバックを持たないため、version を上げるだけでも
既存インストールは migration がない限り起動時に落ちる。

## テスト fixture の作り方

instrumented test では列を直接組み立てる private ヘルパーを 1 つ置き、`mediaId` と `syncStatus` を必須引数、
`cloudStoragePath` と `uploadedAt` をデフォルト値付きにして、テスト本体からは差分だけ渡す。

fixture でも `syncStatus` は `SyncStatus.PENDING_UPLOAD.name` の形で enum から導出し、
`"PENDING_UPLOAD"` を直接書かない。DAO の SQL リテラルだけがハードコードの唯一の例外で、
それはガードテストで守っている。
