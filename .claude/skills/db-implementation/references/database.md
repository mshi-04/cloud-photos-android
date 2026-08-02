# Database と DI

DB クラスは `feature/media/data` にあり、その提供は `app` モジュールの Hilt module が行う。
この 2 箇所の分担と、migration フォールバックが無いことがこのファイルの要点。

## CloudPhotosDatabase

`feature/media/data/src/main/kotlin/com/appvoyager/cloudphotos/data/media/db/CloudPhotosDatabase.kt`

`@Database` を付けた `RoomDatabase` の abstract class で、`entities` は `UploadRecordEntity` の 1 つ、
`version = 1`、`exportSchema = false`。DAO は abstract 関数 1 つで公開する。
`Migration` は 1 つも定義しておらず、`@TypeConverters` も無い。
Entity を追加する場合は `entities` 配列と DAO の abstract 関数を両方足す。

## DatabaseModule

`app/src/main/kotlin/com/appvoyager/cloudphotos/di/DatabaseModule.kt`

`@InstallIn(SingletonComponent::class)` の object module が、`@Singleton` で 2 つを提供する。
1 つは `Room.databaseBuilder` に application context・`CloudPhotosDatabase`・DB ファイル名
`cloud_photos.db` を渡してそのまま `build` した database。もう 1 つは、その database から
DAO を取り出す provider。DataSource は database ではなく DAO を注入される。
DB ファイル名の定義箇所はここだけ。

builder には `fallbackToDestructiveMigration` も `addMigrations` も付いていない。

DB クラスは data モジュール、provider は app モジュールという分かれ方をしているため、
DB 関連の変更でも app 側の module を開く必要がある。DAO を足したら同じ形の provider をここへ追加する。

## version を上げるときの判断

destructive migration のフォールバックが無いので、version を上げて migration も与えないと、
既存インストールは DB を開いた時点で例外になり、起動直後にクラッシュする。
新規インストールとエミュレータの初回起動では再現せず、手元の動作確認では気付けない。

version を上げる変更では、実装前に次を決める。

1. 既存の `upload_records` を保持するのか、捨ててよいのか。
2. 保持するなら `Migration` を書いて `addMigrations` へ渡す。
   捨ててよいなら `fallbackToDestructiveMigration` を明示的に足す（現状は未設定なので追加が必要）。
3. `exportSchema = false` のままでは schema JSON が出力されず `MigrationTestHelper` を使う
   migration test が書けない。migration test を書くなら `exportSchema = true` と
   schema 出力先の設定から始める。要否は `../../../../docs/testing-conventions.md` に従う。

捨てる判断をする場合、`upload_records` は端末側の同期状態そのものなので、消すと
アップロード済み判定が失われ再アップロードや再同期が走る。影響範囲を先に確認する。

## Gradle 依存の分担

`feature/media/data/build.gradle.kts` は `room.runtime` / `room.ktx` に加えて
room compiler を `ksp` で持つ。`app/build.gradle.kts` は DI から参照するために `room.runtime` だけを持つ。
Entity/DAO を別モジュールへ足す場合は、そのモジュールに `ksp` の room compiler が必要。

## テストでの組み立て

instrumented test は Hilt を経由せず、`Room.inMemoryDatabaseBuilder` に `allowMainThreadQueries` を
付けたメモリ DB をその場で組み、そこから DAO を取り出す。`androidx.room.testing` は data モジュールの
`testImplementation` と `androidTestImplementation` の両方に入っているため、
`MigrationTestHelper` を使う migration test も同モジュールへ置ける。
