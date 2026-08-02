# DataSource

`data/<name>/datasource/` に interface と `XxxDataSourceImpl` を並べて置き、provider（Amplify、DataStore、MediaStore、Room）を閉じ込める class。

## interface の宣言

interface の関数は domain の型だけで宣言する。`UploadDataSource` は `UploadResult<CloudStoragePath>` を返し、
`AuthDataSource` の全関数は `AuthResult<...>` を返す。Amplify の `AuthSignInResult` や `StorageUploadResult` は
シグネチャに出さず、impl の内側で domain model へ落とす。impl は public class に `@Inject constructor` を付ける。

## Amplify callback の橋渡し

Amplify は成功 `Consumer` と失敗 `Consumer` を取る callback API なので、`AuthDataSourceImpl` と
`UploadDataSourceImpl` は `suspendCancellableCoroutine` で suspend 関数に変換する。成功 callback では
`CancellableContinuation.resume` の onCancellation handler 付き overload を使い、失敗 callback では
`resumeWithException` を呼ぶ。onCancellation handler は空実装にしている（kotlinx-coroutines 1.11 では引数 3 つ）。
Amplify の成功値は解放の要る資源ではないため、ここで返却処理をする必要が無い。

キャンセルの伝播は SDK が `Cancelable` を返すかどうかで変わる。`Amplify.Storage.uploadInputStream` と
`Amplify.API.*` は operation を返すので、`invokeOnCancellation` で `cancel()` を呼ぶよう繋ぐ。繋がないと
coroutine のキャンセル後も S3 転送や HTTP 呼び出しが走り続ける。一方 `Amplify.Auth.*` は `Cancelable` を返さないため
`invokeOnCancellation` を書く対象が無く、キャンセルは待ち側の再開のみで、SDK 側の処理は止まらない。

Firebase の `Task`（`AuthDataSourceImpl.cleanUpFcmToken` の token 取得と `deleteToken`）も同じ橋渡しで、
`addOnSuccessListener` / `addOnFailureListener` を continuation へ繋ぐ。

## REST 呼び出し

`core/data` の `awaitAmplifyRestCall` が、API 名の付与、非 2xx レスポンスの `AmplifyRestException` 化、
`invokeOnCancellation` による operation cancel をまとめている。呼び出し側は `RestOptions.builder()` で path、
header、body を組み、method（`Amplify.API.get` / `post` / `delete` / `put`）を lambda で渡すだけ。
`UploadRecordRemoteDataSourceImpl` と `DeviceTokenDataSource` がこの形。
リクエスト body の `JSONObject` 組み立ては DataSource に置き、レスポンスの domain 変換は mapper へ渡す。
`AmplifyRestException.isNonServerError` は 2xx でも 5xx でもない応答（主に 4xx）の判定に使い、
再試行しない分岐や error mapping の切り分けに用いる。

## CancellationException の扱い

`runCatching` と `catch (e: Exception)` は `CancellationException` も掴む。掴んだまま error へ倒すと、
キャンセル済みの呼び出し元へ成功／失敗の結果が返り処理が続く。書き方は 2 通りが混在している。

`UploadDataSourceImpl.uploadMedia` は try/catch 形式で、`catch (e: CancellationException) { throw e }` を
一般の `catch (e: Exception)` より前に置く。`AuthDataSourceImpl` は `runCatching { }.fold(...)` 形式が基本で、
`Result` を検査する箇所では private 拡張 `rethrowIfCancellation()` を、`runCatching {}.onFailure {}`、
`fold` の `onFailure` 引数、`exceptionOrNull()` で取り出した直後の 3 形態で先に呼ぶ。
どちらでも「mapper へ渡す前に再スローする」という順序が要点。

`AuthDataSourceImpl.deleteUser` は、backend の REST 削除が成功した場合のみ Cognito 削除へ進む二段構成で、
REST 失敗時は `rethrowIfCancellation()` の後に `AuthResult.Error` を返す。順序の理由はコード内コメントにある。

## DataStore と ContentResolver

`SettingsDataSourceImpl` は file top の property delegate で `DataStore<Preferences>` を作りつつ、
primary constructor を `internal` にして `DataStore` を直接受け取り、`@Inject constructor` で `@ApplicationContext`
の `Context` を取る secondary constructor から委譲する。テストから任意の `DataStore` を差し込むための形で、
key と既定値は `companion object` に置く。読み出しは `Flow` を `map` する property、書き込みは suspend 関数。

`LocalMediaDataSourceImpl` は `withContext(Dispatchers.IO)` の中で `contentResolver.query(...)` を `use` で回し、
cursor の列を domain の `Media` へ組み立てる。Amplify 系は SDK 側が非同期なので dispatcher の切り替えを入れない。

`UploadRecordLocalDataSourceImpl` は DAO 呼び出しと Entity mapper の適用だけを行う
（DAO と Entity 自体は db-implementation の領域）。

## DI

binding は `app/.../di/` の `@Module @InstallIn(SingletonComponent::class)` に `@Binds @Singleton` で書く。
`AuthDataSourceModule`、`UploadDataSourceModule`、`UploadRecordModule`、`MediaDataModule` が対応する。
