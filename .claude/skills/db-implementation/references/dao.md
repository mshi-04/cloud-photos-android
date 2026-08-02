# DAO

DAO は `feature/media/data/src/main/kotlin/com/appvoyager/cloudphotos/data/media/db/dao/` に置く。
`UploadRecordDao` が唯一の実例で、`@Dao` を付けた interface に `suspend` 関数だけを並べる。
Flow を返す query は今のところ無い。

## 構造

query は `@Query` に SQL を直書きし、書き込みは `@Insert` + `@Update` ではなく `@Upsert` 一本。
`upsertAll` は Entity の `List` を受け、同じ `mediaId` の行は置き換わる。
読み出しは `mediaId` の `IN` 句で複数件を引くもの、`syncStatus` の完全一致で絞るもの、
未同期の `mediaId` だけを返すものの 3 つ。削除は `mediaId` 指定の `DELETE` 文。

引数・戻り値は Entity と primitive のみで、domain の value object や enum は受け取らない。
単体取得の query は用意せず、1 件だけ欲しい場合も `IN` 句の query に 1 要素の `List` を渡す。

## SyncStatus 文字列のハードコード

未同期一覧を返す query だけは、`syncStatus` の `IN` 句に `'PENDING_UPLOAD'` と `'PENDING_DELETE'` を
SQL の文字列リテラルとして直接埋め込んでいる。これは `SyncStatus` の enum NAME のコピーで、
その旨と「enum をリネームしたらこの query も更新する」ことが関数の KDoc に書いてある。

enum 名を変えても SQL はコンパイルエラーにならず、単に 0 件を返して通る。
未同期の media が永久に拾われなくなる形で静かに壊れるため、
`feature/media/data/src/test/kotlin/com/appvoyager/cloudphotos/data/media/db/dao/UploadRecordDaoSyncStatusTest.kt`
がガードになっている。このテストは Room を起動せず、`SyncStatus.entries` の名前一覧に
`PENDING_UPLOAD` と `PENDING_DELETE` が含まれることを 1 ケースずつ検証し、
失敗メッセージで「DAO の SQL を直せ」と指示する。

新しく status リテラルを SQL へ埋め込む query を足すときは、このガードテストへ同じ形のケースを追加する。
逆に、リテラルを避けられるなら埋め込まない。`UploadRecordLocalDataSourceImpl` は
`SyncStatus.PENDING_UPLOAD.name` を引数として渡すことで、status 一致の query をハードコードなしで使っている。

## 呼び出し側との契約（空・不在の扱い）

- `IN` 句に空リストを渡しても例外にはならず、空リストが返る。
- 存在しない `mediaId` を渡しても空リストで、プレースホルダ行は返らない。
- 存在しない `mediaId` の削除は既存行を変えない（冪等）。

DAO は「見つからない」を型で表現しないので、要素の存在を前提にした処理は `single()` などで落ちる。
件数の期待は呼び出し側で明示する。

## テストの置き場所

- SQL の実挙動（`IN` 句、status フィルタ、`@Upsert` の置換、削除の冪等性）は androidTest 側の
  `UploadRecordDaoInstrumentedTest` で検証する。`Room.inMemoryDatabaseBuilder` に
  `allowMainThreadQueries` を付けて組み立て、`@After` で `close` する。
- SQL を実行せずに済む契約（enum 名の存在など）は unit test 側に置き、instrumented test を増やさない。
- テストの書式と検証範囲は `../../../../docs/testing-conventions.md` に従う。
