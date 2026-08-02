# Worker

media 同期の Worker は `feature/media/data/src/main/kotlin/com/appvoyager/cloudphotos/data/media/worker/` に置く。既存は `UploadMediaWorker` と `DeleteMediaWorker` の 2 つで、新規追加もこの骨格に合わせる。

## クラス定義と DI

`@HiltWorker` を付け、コンストラクタは `@AssistedInject`、`CoroutineWorker(context, workerParams)` を継承する。`@Assisted` を付けるのは `context: Context` と `workerParams: WorkerParameters` の 2 つだけで、Repository、DataSource、Resolver、NotificationHelper といった依存は通常の constructor injection として並べ、Hilt が解決する。

この形が成立する前提は `app/src/main/kotlin/com/appvoyager/cloudphotos/CloudPhotosApp.kt` にある。`Configuration.Provider` を実装して `HiltWorkerFactory` を設定し、`app/src/main/AndroidManifest.xml` で `androidx.work.WorkManagerInitializer` を `tools:node="remove"` している。ここを崩すと Worker の生成自体が失敗する。

`CoroutineWorker` なので `doWork()` は suspend として書き、Dispatcher は指定しない。ループ内の分岐が多いため、両 Worker とも `@Suppress("ThrowsCount", "LoopWithTooManyJumpStatements")` を付けている。

## doWork の処理順

`UploadMediaWorker` は次の順で進める。

1. `LocalUploadRecordsRepository` から pending なレコード一覧を取得し、空なら通知を出す前に `Result.success()` を返す。
2. 件数を渡して通知を表示する。実際に処理する対象があるときだけ通知を出すため、必ず早期 return の後に置く。
3. 一時失敗フラグと成功件数のローカル変数を用意し、一覧をループする。
4. ループを抜けたら通知を cancel する。
5. 一時失敗がなく成功件数が 1 以上のときだけ `completeUpload()` をループの外で 1 回呼ぶ。レコードごとに呼ぶとリモート側の完了扱いが重複する。
6. 一時失敗があれば `Result.retry()`、なければ `Result.success()` を返す。

## ループ 1 件あたりの処理

先頭でレコードを `mediaId` から取り直し、取得できない場合と `SyncStatus.PENDING_UPLOAD` でなくなっている場合は `continue` する。一覧取得から処理が回ってくるまでの間に別経路が状態を変えている可能性があるため、判断は必ず取り直した値で行う。

次に content type を解決し、その後に `cloudStoragePath` を見る。既に値があるレコードは S3 送信を飛ばしてリモート登録から再開する。retry のたびに同じバイト列を送り直さないための分岐で、消すと再送のたびに重複オブジェクトが増える。

S3 送信が成功したら `cloudStoragePath` だけを先にローカルへ保存し、その後リモートへ登録レコードを作成し、返ってきたレコードをローカルへ保存して成功件数を進める。

## 永続失敗と一時失敗の分岐

retry しても解消しない失敗は `SyncStatus.ERROR` を保存して `continue`、retry で解消し得る失敗は状態を変えずに一時失敗フラグだけ立てて `continue` する。一時エラーで `ERROR` に落とすと自動 retry から復帰できなくなるため、この振り分けが Worker の中心になる。

永続扱いにするのは、content type が解決できない、`ContentType` の生成に失敗する、localUri が取得できない、の 3 ケース。S3 側は `UploadError` の型で判定し、`AccessDenied` / `NotAuthenticated` / `StorageLimitExceeded` / `FileNotFound` が永続、`Network` と `Unknown` が一時失敗。

リモート API 側は private な `isPermanentFailure(e: Throwable)` が例外 message から `Unexpected response code (\d+)` を正規表現で拾い、404 と 429 を除いた 400..499 を永続失敗と判断する。message が無い場合とコードが取れない場合は一時失敗として扱う。この判定は `UploadMediaWorker` と `DeleteMediaWorker` に同じ内容で重複しているので、片方だけ直すとアップロードと削除で retry 挙動がずれる。

リモート登録が永続失敗したときは、先に送った S3 オブジェクトの削除を試みる。削除できたら `cloudStoragePath` を null に戻して `ERROR`、削除できなかったら `SyncStatus.PENDING_DELETE` にして削除フローへ引き継ぐ。

## CancellationException

`runCatching { ... }.onFailure` の中では最初に `if (e is CancellationException) throw e` を書く。握り潰すと Worker のキャンセルが一時失敗として扱われ、`Result.retry()` に化ける。`completeUpload()` や S3 クリーンアップのように結果を無視する `runCatching` でも同じ再 throw を入れる。

## DeleteMediaWorker との差分

通知を持たず、pending が空でも早期 return しない。`cloudStoragePath` が null のレコードはリモート削除を飛ばしてローカルレコードだけ消す。未アップロードのオブジェクトを削除しに行かないための分岐。

リモート削除の失敗は `isPermanentFailure` で `ERROR` と retry に振り分けるが、その後の S3 削除失敗だけは `CancellationException` の再 throw のみ行って握り潰し、ローカルレコード削除へ進む。孤立オブジェクトを許容する既存仕様なので、ここを変えると retry 回数とローカル/リモートの整合に同時に影響する。

## companion object

unique work name は Worker の companion object に `const val WORK_NAME` として置き、Scheduler から参照させる。値は `upload_media_worker` と `delete_media_worker`。Scheduler 側にリテラルを書くと名前がずれても気付けない。

フロー全体と `SyncStatus` の遷移は [docs/media-upload-flow.md](../../../../docs/media-upload-flow.md) を参照する。
