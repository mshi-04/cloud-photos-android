# Entity Mapper

Entity と domain model の変換は `feature/media/data/src/main/kotlin/com/appvoyager/cloudphotos/data/media/db/`
直下の mapper に集約する。`UploadRecordEntityMapper` が唯一の実例。

## 構造

`internal object` で、`toDomain` と `toEntity` の 2 関数を対で持つ。状態も依存も持たないため
DI では注入せず、`UploadRecordLocalDataSourceImpl` が関数参照として `map` に渡して使う。

`toDomain` は Entity の 5 列を domain の `UploadRecord` へ組み替える。値の生成は必ず value object の
`of()` を通す。`MediaId` / `CloudStoragePath` / `IsDeleted` / `MediaUploadedAt` はいずれも
`@JvmInline value class` で constructor が private のため、`of()` が唯一の入口になる。
nullable な `cloudStoragePath` だけは null 安全呼び出しで包み、null をそのまま domain へ渡す。
`toEntity` は逆向きに value object の `value` を取り出し、`SyncStatus` は `name` を保存する。
domain の `mediaUploadedAt` と Entity の `uploadedAt` のように名前がずれる対応も、ここだけが定義箇所。

## 不明な status を ERROR へ落とす

`toDomain` の status 復元は `SyncStatus.valueOf` ではなく、`enumValues<SyncStatus>()` を名前で検索し、
見つからなければ `SyncStatus.ERROR` を返すエルビス演算子の形になっている。

`valueOf` だと未知の文字列で `IllegalArgumentException` が飛ぶ。DB には過去バージョンのアプリが
書いた行やリネーム前の enum 名が残り得るため、読み出しが例外で止まると同期処理全体が回復不能になる。
ここは握り潰しではなく「未知の状態は ERROR として隔離する」という意図的な劣化。
`ERROR` は `getPendingMediaIds()` の `IN ('PENDING_UPLOAD', 'PENDING_DELETE')` に含まれないため、
自動では再同期されない。復帰させるには別の経路で status を戻す必要がある。

新しい `SyncStatus` を追加するときは、旧バージョンのアプリがその行を `ERROR` として読み、
そのまま同期対象から外れることを前提に、復旧経路が要るかどうかを判断する。

なお `toDomain` は status 以外の列に対しては寛容ではない。`cloudStoragePath` が空文字なら
`CloudStoragePath.of()` の `require` で落ちる。空文字を書き込まないことは書き込み側の責務。

## 変更時に一緒に動くもの

- Entity の列を足す / 型を変えると、`toDomain` と `toEntity` の両方が同時にずれる。
  片側だけ直すとコンパイルは通っても往復が非可逆になる。
- domain の value object を差し替えた場合も mapper が唯一の接点になる。
- mapper のテストは `feature/media/data/src/test/kotlin/` 側の unit test で足りる。
  Room を起動する必要はない。往復（`toEntity` の結果を `toDomain` で戻すと元に一致する）と、
  不明 status が `ERROR` になる境界を押さえる。
