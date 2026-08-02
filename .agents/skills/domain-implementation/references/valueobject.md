# Value object

`feature/<name>/domain/.../valueobject/` に置く、検証済みであることを型で保証する 1 フィールドの wrapper。

## 構造

`@JvmInline value class` に `private constructor(val value: T)` を持たせ、`companion object` の `fun of(raw: T): X` だけを生成経路にする。`of` 以外から作れないため、その型の値は必ず検証を通っている。

`of` の中身は「正規化 → 検証 → 生成」の順で固定してある。`String` なら先頭で `trim()` し、`also` ブロック内で `require` を並べ、最後に `let(::X)` でコンストラクタへ渡す。この式形式を崩さない。

検証条件が複数あるときは `require` を条件ごとに分け、それぞれ独自のメッセージを持たせる。`Email` は空判定と正規表現一致を別の `require` にしており、`ConfirmationCode` は空判定と「6 桁の数字」判定を分けている。1 つの `require` に `&&` で条件を詰め込むと、どの条件で落ちたかが例外メッセージから判別できなくなる。

`Int` / `Long` / `Boolean` を包む場合も同じ形で、`trim()` がない分だけ短くなる。`GridColumnCount` は許容範囲の境界を `const val MIN` / `const val MAX` として公開し、`require` のメッセージにも埋め込んでいる。UI の設定画面が同じ定数を参照できる。

## 検証のない value object

`IsDeleted` の `of` は検証を持たず、受け取った `Boolean` をそのまま包むだけ。それでも型を作るのは、`UploadRecord` に生の `Boolean` が並ぶと引数順の取り違えがコンパイル時に検出できなくなるため。

同じ理由で `MediaCreatedAt` と `MediaUploadedAt` は中身が同一（`Long` + 非負チェック）でも別の型にしてある。epoch millis 同士の混同を型で止める。

## 例外的な形

- `UploadSuccessCount` だけが式形式ではなくブロック形式で、`require` の後に `return` している。既存はそのままでよいが、新規は式形式に合わせる。
- `MediaUrl` は `java.net.URI` と `URL` で妥当性を判定し、`content` scheme のときは `toURL()` を通さず許可する。`android.net.Uri` を domain に持ち込めないため JDK の API で代替している。catch するのは `URISyntaxException` / `MalformedURLException` / `IllegalArgumentException` の 3 種で、判定関数に `@Suppress("SwallowedException")` が付いている。

## 使いどころ

model と request のフィールドは原則 value object にする。`UploadRecord` は `mediaId` / `cloudStoragePath` / `isDeleted` / `mediaUploadedAt` がすべて value object で、`syncStatus` だけ enum。

nullable にしてよいのは「未確定」を表す場合だけ。`UploadRecord.cloudStoragePath` はアップロード完了まで確定しないので `CloudStoragePath?` になっている。

`.value` で生プリミティブへ戻すと、検証済みという保証はその時点で失われる。data / UI との境界（mapper、DTO 組み立て、DB 書き込み）まで value object のまま運ぶ。

- 配置と依存方向: `../../../../docs/implementation-rules.md`
