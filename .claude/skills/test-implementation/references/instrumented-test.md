# InstrumentedTest

`feature/media/data/src/androidTest` にある 3 本が全てです。
`UploadRecordDaoInstrumentedTest`（Room DAO）、`MediaSchedulerInstrumentedTest`（WorkManager Scheduler）、
`SettingsDataSourceImplInstrumentedTest`（DataStore）。
class 名は対象 + `InstrumentedTest` です。

## JUnit 4 であること

`AndroidJUnitRunner` 上で動くため JUnit 5 は使えません。import は `org.junit.Test`、`org.junit.Before`、
`org.junit.After`、`org.junit.Assert.assertEquals` です。JUnit 5 の annotation を書いても実行されません。

`kotlinx.coroutines.test.runTest` はそのまま使えます。MockK は使わず、依存は全て実体を組み立てます。
テスト名は D8/DEX の制約で backtick を使えないため、同じ意味を保った snake_case にします。
AAA ラベルと観点コメントは UnitTest と同じ形式です。

Kover の集計対象は UnitTest だけなので、ここにテストを足してもカバレッジ数値は動きません。

## Room DAO

`@Before` で `ApplicationProvider.getApplicationContext<Context>()` を取り、
`Room.inMemoryDatabaseBuilder(context, CloudPhotosDatabase::class.java)` に `.allowMainThreadQueries()` を付けて build し、
そこから DAO を取り出します。`@After` で `database.close()` を呼びます。
database と dao は `lateinit var` の private field です。

`SyncStatus` の enum 名は Room の `@Query` に文字列で直書きされています。
そのため絞り込み系のテスト（`getPendingMediaIds`、`getByStatus`）では全 status の record を投入し、
どれが返るかを見ます。enum をリネームすると SQL 側が追従せず、このテストだけが落ちます。

順序を保証しないクエリの比較は `sortedBy { it.mediaId }` を両辺に掛けるか、`toSet()` にしてから `assertEquals` します。

境界として、空の id list を渡すクエリ、存在しない id を渡すクエリ、存在しない id に対する delete を必ず入れます。
upsert は同じ primary key で 2 回呼び、行が置き換わることを確認します。

record 生成は private `createRecord()` を file 末尾に置きます。Entity なので `syncStatus` は
`SyncStatus.PENDING_DELETE.name` のように `String` へ変換して渡します。

## WorkManager Scheduler

`@Before` で `WorkManagerTestInitHelper.initializeTestWorkManager(context, Configuration)` を呼び、
`WorkManager.getInstance(context)` を保持します。`@After` で `workManager.cancelAllWork().result.get()` を呼びます。

検証する契約は、unique work 名で 1 件だけ enqueue されていること、state が `WorkInfo.State.ENQUEUED` であること、
`workerClassName` が対象 Worker であること、`requiredNetworkType` が `NetworkType.CONNECTED` であること、
input Data が空であること、の 5 項目です。

これらは private data class `WorkSpecSnapshot` にまとめ、1 回の `assertEquals` で比較します。

`WorkInfo` だけでは constraints と workerClassName が取れないため、
`WorkManagerImpl.getInstance(context).workDatabase.workSpecDao().getWorkSpec(id)` から `WorkSpec` を取ります。
これは restricted API なので、取得を行う private helper に `@Suppress("RestrictedApi")` を付けます。
unique work 名は production の定数（`UploadMediaWorker.WORK_NAME`）を参照し、テスト側に文字列を書きません。

`ExistingWorkPolicy.KEEP` の確認は、schedule 関数を 2 回呼んでから
`workManager.getWorkInfosForUniqueWork(name).get().size` が 1 であることで行います。
件数だけでは `REPLACE` と区別できません。最初の work が保たれたことまで見るなら、
1 回目の work id を控え、2 回目の後も同じ id が残ることを確認します。

## DataStore

`@Before` で `context.preferencesDataStoreFile("settings-test-${UUID.randomUUID()}")` として一時ファイル名を作り、
`CoroutineScope(SupervisorJob() + Dispatchers.IO)` を用意して `PreferenceDataStoreFactory.create(scope, produceFile)` で
DataStore を組み立て、対象 DataSource に渡します。
`@After` で scope を `cancel()` し、ファイルを `delete()` します。

ファイル名に UUID を付けるのは、DataStore が同一プロセス内で同じファイルを二重に開けないためです。
テスト間でファイルを共有すると後続が失敗します。

読み出しは Flow に対して `first()` で 1 件だけ取ります。
未設定時の default 値、正常な書き込み、不正値（0 や負値）が既存値を上書きしないこと、
複数回書いたときに最後の値が残ることを見ています。

## 実行

`./gradlew :feature:media:data:connectedDebugAndroidTest` にエミュレータまたは実機が必要です。
ローカルで実行できない場合は、CI の `instrumented-test` job に委ねたことと未検証範囲を報告します。
