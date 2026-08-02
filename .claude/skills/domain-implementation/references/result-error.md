# Result と Error

domain の失敗契約は操作ごとに異なる。provider 由来の予期される失敗を呼び出し側が分岐する操作は
sealed class の Result を返し、ローカル DB 操作などは生の値や Unit を返して例外を伝播する。
Result / Error 型は feature ごとに独立している。

## Result 型の構造

`AuthResult<out T>` は sealed class で、`Success<T>(val value: T)` と `Error(val error: AuthError)` の 2 つの data class を持つ。`Error` が `AuthResult<Nothing>` を継承しているのが要点で、`out T` と組み合わさることで `map` / `flatMap` の Error 分岐が `this` をそのまま返せる。再ラップも `AuthResult.Error(error)` の書き直しも要らない。

`AuthResult.kt` は class の外、同じファイル内に 4 つの top-level 拡張を置いている。`getOrNull()` と `errorOrNull()` は安全キャストで値を取り出す形、`map()` と `flatMap()` は `inline fun` で `when` の網羅分岐を書く形。

`UploadResult<out T>` は `Success` / `Error` の構造だけ同じで、拡張関数を一切持たない。呼び出し側の `UploadMediaWorker` が `is UploadResult.Success` / `is UploadResult.Error` で分岐して WorkManager の `Result` へ変換するため、値の取り出しより網羅分岐が必要になるから。新しい Result 型を作るときも、実際に使う拡張だけを足す。

## Error 型の構造

`AuthError` と `UploadError` は同形。`open val message: String?` を primary constructor に持つ sealed class を親にし、各バリアントを `override val message: String? = null` を持つ data class として並べる。デフォルト値があるので、呼び出し側はメッセージなしでも生成できる。

`AuthError` のバリアントは `CodeExpired` / `CodeMismatch` / `InvalidCredentials` / `InvalidPassword` / `Network` / `TooManyRequests` / `UserNotConfirmed` / `UsernameAlreadyExists` / `Unknown`。`UploadError` は `AccessDenied` / `NotAuthenticated` / `StorageLimitExceeded` / `Network` / `FileNotFound` / `Unknown`。

分類は provider の例外名ではなく、UI が出し分ける単位で切る。`Unknown` は data 層で想定外の例外を受けたときの逃げ道で、`AuthError.Unknown` にはその旨のコメントが付いている。原因の `Throwable` は domain に持ち込まず data 側のログに留める。domain の型に `cause: Throwable` を持たせると、Amplify の例外型が domain へ漏れる。

## 変更時の追随箇所

Result / Error / `SyncStatus` は data の mapper と UI が対で依存している。片側だけ変えると対応がずれる。

`AuthError` を変更した場合は `feature/auth/data/.../util/AuthErrorMapper.kt` と、`feature/auth/ui/.../viewmodel/` の 4 つ（`LoginViewModel` / `ForgotPasswordViewModel` / `ResetPasswordViewModel` / `VerificationCodeViewModel`）。

`UploadError` を変更した場合は `feature/media/data/.../util/UploadErrorMapper.kt` と `feature/media/data/.../worker/UploadMediaWorker.kt`（retry と failure の判定）。

`SyncStatus`（`PENDING_UPLOAD` / `SYNCED` / `PENDING_DELETE` / `ERROR`）を変更した場合は data 側が `db/UploadRecordEntityMapper.kt`、`db/dao/UploadRecordDao.kt`、`util/RemoteUploadRecordMapper.kt`、`worker/UploadMediaWorker.kt`、`worker/DeleteMediaWorker.kt`。domain 側は `DeleteMediaUseCase` / `PrepareUploadQueueUseCase` / `RecordMediaUploadUseCase` が値を参照している。

`SyncStatus` の enum 名は DAO の query 文字列と DB 保存値そのものなので、リネームは migration を伴う。

- 例外と Result の扱い: `../../../../docs/error-handling-guide.md`
