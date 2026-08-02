# Mapper

provider の例外・JSON・Entity を domain model へ変換する `object`。`data/<name>/util/` に置く（Entity 変換のみ `data/media/db/`）。

## 形

mapper は `internal object` で、injected dependency も state も持たない純粋関数だけを公開する。
`AuthErrorMapper`、`UploadErrorMapper`、`RemoteUploadRecordMapper`、`UploadRecordEntityMapper` がこれにあたる
（`AuthSignInStepMapper` だけが public object）。DI へは載せず、呼び出し側は object 名で直接参照するか、
`UploadRecordEntityMapper::toDomain` のように関数参照を `map` へ渡す。

関数名は用途で固定する。例外変換は `map`、enum 変換は `mapXxx`、Entity との双方向変換は `toDomain` / `toEntity`、
API レスポンス変換は `fromFetchResponse` / `fromCreateResponse`。戻り値は domain の model / error / value object で、
値オブジェクトの生成（`MediaId.of` などの `of` factory）は mapper 内で行い、生の `String` や `Long` を domain へ渡さない。

## 例外 mapping

`AuthErrorMapper.map` と `UploadErrorMapper.map` は `Throwable` を受け取り、`when` の型分岐で
domain の `AuthError` / `UploadError` の sealed subtype を返す。分岐から漏れたものは `Unknown` に落ちる。

Amplify は Cognito SDK の例外を `ServiceException` で包むため、最上位の型だけでは `CodeMismatch` と `Unknown` を
区別できない。`AuthErrorMapper` は `ServiceException` を private 関数へ渡し、その中で `cause` を見て
`ExpiredCodeException` / `CodeMismatchException` / `InvalidPasswordException` などへ分岐する。
`AmplifyRestException` は `isNonServerError` で 4xx 系と 5xx 系を分け、前者を `Unknown`、後者を `Network` にする。

型で判別できない場合のみ message 検査へ落とす（`AuthErrorMapper` の `ValidationException` 処理、
`UploadErrorMapper` の access denied / not authenticated / storage limit 判定）。SDK の文言変更で静かに `Unknown` へ
倒れるため、まず型を探し、message 判定は最後の手段にする。

mapper は `CancellationException` の分岐を持たない。呼び出し側の DataSource が mapper へ渡す前に再スローする前提で、
ここへ到達した時点で `Unknown` になってしまう（詳細は `../../../../docs/error-handling-guide.md`）。

## enum mapping

`AuthSignInStepMapper.mapSignInStep` は Amplify の `AuthSignInStep` を domain の `SignInStep` へ 1 対 1 で写し、
`else` で `SignInStep.UNKNOWN` へ落とす。domain 側に `UNKNOWN` を用意してあるので、SDK が enum を増やしても
data 層で例外にならない。文字列で保存された `syncStatus` を戻す `UploadRecordEntityMapper.toDomain` も同様に、
未知の値は `SyncStatus.ERROR` へ倒す。

## JSON レスポンスの変換

`RemoteUploadRecordMapper.fromFetchResponse` は `org.json.JSONObject` でレスポンス文字列を解析し、
`records` 配列の各要素を `UploadRecord` へ組み立てる。ここで要素ごとに `runCatching { ... }.getOrNull()` を
`mapNotNull` の中で使っており、必須 field の欠落や値オブジェクトの検証失敗を起こした 1 件だけを捨てて残りを返す。
同期一覧が丸ごと空になるより、壊れた 1 件が欠ける方を選んだ意図的なトレードオフ。

代償として欠落は呼び出し側から見えず、件数の差としてしか現れない。取得件数の一致が意味を持つ処理を
この結果の上に作らないこと。なお外側の `JSONObject` 生成と `records` 配列の取り出しは包んでいないので、
レスポンス全体の形が違う場合は従来どおり例外が呼び出し側へ伝わる。

## 外部値は引数で受ける

`RemoteUploadRecordMapper.fromCreateResponse` は、レスポンスに `uploadedAt` が無い場合の代替値を
`fallbackUploadedAt` 引数で受け取る。mapper 自身は `Clock` を持たず、`UploadRecordRemoteDataSourceImpl` が
`clock.getCurrentTime()` を渡す。mapper を引数だけで結果が決まる関数に保ち、テストで時刻を固定できるようにするため。
レスポンスから `uploadedAt` を取り出す `JSONObject` 操作自体は DataSource 側にある。

## テスト

`src/test/.../util/XxxMapperTest` に直接テストを書く（`RemoteUploadRecordMapperTest`、`AuthErrorMapperTest`、
`AuthSignInStepMapperTest`、`UploadErrorMapperTest`）。依存が無いので mock は不要で、例外 instance や
JSON 文字列をそのまま渡し、返った domain 値を `assertEquals` で確認する。
