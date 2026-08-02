# UseCase

`feature/<name>/domain/.../usecase/` に置く、1 操作 1 class の呼び出し単位。

## 構造

class 名は `<動詞><対象>UseCase`。依存は `@Inject constructor` で受け、フィールドはすべて `private val` の Repository または Scheduler。公開メンバーは `operator fun invoke` ひとつだけで、他の public メソッドは持たない。

戻り値は 3 種類に分かれる。

- provider 越しの操作は `AuthResult<T>` / `UploadResult<T>` をそのまま返す。UseCase 側で成功・失敗を解釈しない。
- 継続監視する読み出しは `Flow<T>` を返し、`suspend` を付けない。`GetMediaListUseCase` と `GetGridColumnCountUseCase` がこれ。
- ローカル DB 更新や scheduler 起動は `suspend` で `Unit` を返す。

引数が 2 つ以上になるなら `request/` の data class ひとつにまとめる。value object 2 つで完結する場合だけ直接並べてよく、`RecordMediaUploadUseCase` が `mediaId` と `mediaUploadedAt` を取るのがその例。引数なしの操作は `invoke()` のみ。

依存フィールド名は、Repository が 1 つなら `repository`、複数を取り分ける必要があるときは `localRepository` / `remoteRepository` のように役割を付ける。

## 薄い委譲と調整型

`feature/auth/domain/.../usecase/` の 10 個はすべて Repository の同名メソッドへ 1 行で委譲するだけで、分岐も検証も持たない。auth だけを読むと「UseCase とは委譲層」という誤った像を得るので、新規追加の参考にはしない。

実際の調整は `feature/media/domain/.../usecase/` にある。模倣すべきはこちら。

- `DeleteMediaUseCase` — `LocalUploadRecordsRepository` と `DeleteScheduler` を取る。`syncStatus` が `PENDING_UPLOAD` ならレコードを物理削除して早期 return し、それ以外は `isDeleted` と `syncStatus` を差し替えた copy を保存してから scheduler を起動する。未アップロードのレコードはクラウド側に存在せず、`PENDING_DELETE` にしても消す相手がないための分岐。
- `PrepareUploadQueueUseCase` — `LocalMediaRepository` / `LocalUploadRecordsRepository` / `UploadScheduler` / `Clock` の 4 依存。既存レコードとの差分だけを新規 `UploadRecord` として組み立てる。media が空のとき、差分が空のときにそれぞれ早期 return し、無駄な scheduler 起動を避ける。
- `SyncUploadRecordsUseCase` — リモート取得結果から `getPendingRecordMediaIds()` に含まれる分を除外してから保存する。リモートの状態でローカルの未送信操作を上書きしないための保護。
- `GetPendingMediaUseCase` — media 一覧と upload record を突き合わせ、記録のないものだけを返す。ID リストを 900 件ずつ `chunked` して `getUploadRecords` を呼ぶ。SQLite のバインド変数上限を超えないための分割で、data 側ではなく domain に置かれている。
- `RecordMediaUploadUseCase` — `UploadRecord` を組み立てて保存し、続けて `scheduleUpload()` を呼ぶ。保存と起動が必ず対になる。

`ScheduleUploadUseCase` / `ScheduleDeleteUseCase` も薄い委譲だが、Worker と UI の双方から同じ操作を呼ぶ入口を揃えるために残してある。

## 注意点

現在時刻は `core/common` の `Clock` interface を注入して `getCurrentTime()` で取る。`System.currentTimeMillis()` を直接呼ぶと、テストで時刻を固定できなくなる。

Repository のメソッドを 1 対 1 でラップするだけの UseCase は、名前が増えるだけで domain の意味が増えない。新規追加は「複数 Repository の調整」「状態遷移」「validation」のいずれかを持つときに限る。

保存と scheduler 起動のように順序が意味を持つ処理は、呼び出し側に分解させず UseCase 内にまとめる。片方だけ呼ばれる経路ができると同期状態が壊れる。

- 配置と依存方向: `../../../../docs/implementation-rules.md`
