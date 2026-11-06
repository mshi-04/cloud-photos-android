# Authentication

この文書は認証フロー、アカウント操作（サインアウト・退会）、FCM デバイストークンの登録・解除、および認証エラー表現の契約をまとめます。

## 責任境界と Provider 隔離

- **UI 層（`:feature:auth:ui`、`:app`）**:
  - `:app` の [`MainActivity`](../app/src/main/kotlin/com/appvoyager/cloudphotos/ui/MainActivity.kt) / [`MainViewModel`](../app/src/main/kotlin/com/appvoyager/cloudphotos/ui/MainViewModel.kt) が起動時のセッション確認（`GetSessionUseCase`）と、認証済み状態への遷移に応じたトップレベル表示切り替えを担う。
  - `:feature:auth:ui` の各 Screen / ViewModel が入力状態、フォーム検証（`ValidationUtils`）、再送タイマー（`ResendTimer`）、画面遷移・Snackbar effect を担う。
- **Domain 層（`:feature:auth:domain`）**:
  - [`AuthRepository`](../feature/auth/domain/src/main/kotlin/com/appvoyager/cloudphotos/domain/auth/repository/AuthRepository.kt)、[`AuthResult`](../feature/auth/domain/src/main/kotlin/com/appvoyager/cloudphotos/domain/auth/model/AuthResult.kt)、[`AuthError`](../feature/auth/domain/src/main/kotlin/com/appvoyager/cloudphotos/domain/auth/model/AuthError.kt)、[`SignInState`](../feature/auth/domain/src/main/kotlin/com/appvoyager/cloudphotos/domain/auth/model/SignInState.kt)、および `Email` / `Password` / `ConfirmationCode` / `UserId` などの値オブジェクトを定義する。
- **Data 層（`:feature:auth:data`、`:core:data`）**:
  - [`AuthDataSourceImpl`](../feature/auth/data/src/main/kotlin/com/appvoyager/cloudphotos/data/auth/datasource/AuthDataSourceImpl.kt) が Amplify Cognito Auth、バックエンド `/users` API、Firebase Messaging、[`DeviceTokenDataSource`](../core/data/src/main/kotlin/com/appvoyager/cloudphotos/data/fcm/DeviceTokenDataSource.kt) を呼び出す。
  - Cognito / Amplify / Firebase の型や例外は [`AuthErrorMapper`](../feature/auth/data/src/main/kotlin/com/appvoyager/cloudphotos/data/auth/util/AuthErrorMapper.kt) と [`AuthSignInStepMapper`](../feature/auth/data/src/main/kotlin/com/appvoyager/cloudphotos/data/auth/util/AuthSignInStepMapper.kt) の内側で変換し、data の外へ出さない。

## 認証フローと画面間の引き継ぎ

- **サインインと追加ステップ**:
  - `Amplify.Auth.signIn` の結果が `isSignedIn == true` または `AuthSignInStep.DONE` なら `SignInState.SignedIn` を返す。
  - MFA、新パスワード要求、その他の追加ステップは `AuthSignInStepMapper` で domain の `SignInStep` に写す。未知のステップが SDK に追加された場合も `SignInStep.UNKNOWN` へ落として例外にしない。
- **確認コードとパスワードリセット**:
  - サインアップ後または未確認ユーザー（`AuthError.UserNotConfirmed`）は確認コード画面へ遷移する。
  - パスワード再設定完了後（`ResetPasswordScreen` → `LoginScreen`）の完了通知は、Navigation 引数 `login?message={message}` に `AuthSnackbarMessage.key` を渡す。[`LoginViewModel`](../feature/auth/ui/src/main/kotlin/com/appvoyager/cloudphotos/ui/auth/viewmodel/LoginViewModel.kt) は `init` で `SavedStateHandle` から key を読み取って Snackbar effect を発火し、画面復帰で再表示されないよう直後に `savedStateHandle.remove` する。

## アカウント列挙を避けるエラー表現

[`AuthErrorMapper`](../feature/auth/data/src/main/kotlin/com/appvoyager/cloudphotos/data/auth/util/AuthErrorMapper.kt) は、provider 例外を UI が分岐する `AuthError` に変換する。

- **認証失敗の統合**:
  - Cognito の `NotAuthorizedException`、`UserNotFoundException`、および Amplify の `SessionExpiredException`、`SignedOutException` はすべて `AuthError.InvalidCredentials` に統合する。
  - メールアドレスが登録済みかどうかをエラー表示の違いから推測（アカウント列挙）させないための契約であり、不用意に別エラーへ分解しない。
- **例外の剥がし方とフォールバック**:
  - Amplify は Cognito SDK の例外を `ServiceException` の `cause` に包むため、`ServiceException` では `cause` の型（`ExpiredCodeException`、`CodeMismatchException`、`InvalidPasswordException`、`LimitExceededException`、`TooManyRequestsException`、`UserNotConfirmedException`、`UsernameExistsException`、`NotAuthorizedException`、`UserNotFoundException`）を検査する。
  - `ValidationException` は型だけでは区別できないため、message に `"code"`（大文字小文字無視）を含む場合のみ `AuthError.CodeMismatch`、それ以外は `AuthError.Unknown` とする。
  - バックエンド REST 呼び出しの `AmplifyRestException` は、`isNonServerError`（4xx など 5xx 以外）なら `AuthError.Unknown`、サーバーエラー（5xx）なら `AuthError.Network` に分類する。

## サインアウトと退会（アカウント削除）の順序契約

サインアウト（`signOut`）と退会（`deleteUser`）は、複数外部サービスを跨ぐため処理順に厳密な契約がある。

### サインアウト（`AuthDataSourceImpl.signOut`）

1. `cleanUpFcmToken()` を実行する。`CancellationException` は再スローし、それ以外の失敗は握り潰してサインアウト本体へ進む（トークン解除失敗でサインアウト不能にしない）。
2. `Amplify.Auth.signOut` を呼び、`CompleteSignOut` と `PartialSignOut` は `AuthResult.Success(Unit)`、`FailedSignOut` は `AuthResult.Error` として返す。

### 退会（`AuthDataSourceImpl.deleteUser`）

1. `cleanUpFcmToken()` を実行する（`CancellationException` は再スローし、それ以外の失敗では退会処理を止めない）。
2. バックエンド API `DELETE /users` を `awaitAmplifyRestCall` で呼び出し、S3 / DynamoDB 上のユーザーデータを削除する。
   - **順序の制約**: 必ず Cognito アカウント削除より先に成功させる。先に Cognito アカウントを削除すると認証トークンが無効になり、バックエンド削除が失敗してクラウド上のデータが孤立する。
   - `DELETE /users` が失敗した場合は `CancellationException` を再スローしたうえで、Cognito 削除へ進まずに `AuthResult.Error(AuthErrorMapper.map(restFailure))` を返す。
3. バックエンド削除が成功した場合のみ、`Amplify.Auth.deleteUser` を呼び出して Cognito アカウントを削除する。

### `cleanUpFcmToken()` の内部順序

- `FirebaseMessaging.getInstance().token` を取得し、`DeviceToken.of` で検証できたら `DeviceTokenDataSource.unregister(token)`（`DELETE /devices/token`）を呼ぶ。
- `try / finally` の `finally` ブロックで必ず `FirebaseMessaging.getInstance().deleteToken()` を呼び、バックエンド解除の成否にかかわらず端末側の FCM トークンを破棄する。

## FCM Device Token の登録契約

FCM デバイストークンの登録は、`:app` の [`FcmTokenRegistrar`](../app/src/main/kotlin/com/appvoyager/cloudphotos/fcm/FcmTokenRegistrar.kt)、[`RegisterDeviceTokenWorker`](../app/src/main/kotlin/com/appvoyager/cloudphotos/fcm/RegisterDeviceTokenWorker.kt)、[`CloudPhotosFirebaseMessagingService`](../app/src/main/kotlin/com/appvoyager/cloudphotos/fcm/CloudPhotosFirebaseMessagingService.kt)、および `:core:data` の [`DeviceTokenDataSource`](../core/data/src/main/kotlin/com/appvoyager/cloudphotos/data/fcm/DeviceTokenDataSource.kt) が担う。

- **登録契機（3箇所）**:
  1. アプリ起動時（`CloudPhotosApp.onCreate()` からの `fcmTokenRegistrar.register()`）。
  2. 認証済み状態への遷移時（`MainActivity` で `MainUiState.Authenticated` になった際の `mainViewModel.registerFcmToken()`）。
  3. トークン更新時（`CloudPhotosFirebaseMessagingService.onNewToken(token)`）。
- **Enqueue 契約（`RegisterDeviceTokenWorker.enqueue`）**:
  - `WORK_NAME = "register_device_token_worker"`。
  - メディア系 Worker の `KEEP` とは異なり、最新のトークンを優先するため `ExistingWorkPolicy.REPLACE` で登録する。
  - input Data（`KEY_TOKEN = "token"`）に対象トークン文字列を渡し、指数 backoff（`BackoffPolicy.EXPONENTIAL`、初期 30 秒）を設定する。
- **Worker の失敗分類（`RegisterDeviceTokenWorker.doWork`）**:
  - input Data にトークンがない場合、または `DeviceToken.of` が `IllegalArgumentException` を投げた場合は `Result.failure()`（再試行しても回復しない）。
  - `CancellationException` は必ず再スローし、`retry` や `failure` に変換しない。
  - `AmplifyRestException` で `e.isNonServerError == true`（4xx など 5xx 以外の失敗応答）の場合は `Result.failure()`。
  - それ以外の通信失敗や 5xx エラーは `Result.retry()` を返す。

## SDK 操作のキャンセル伝播

- `Amplify.Auth.*` と `FirebaseMessaging` の `Task` はキャンセル用ハンドル（`Cancelable`）を返さないため、`suspendCancellableCoroutine` がキャンセルされた場合は呼び出し側の coroutine 再開のみが行われ、SDK 内部の通信自体は止まらない。
- `AuthDataSourceImpl` のサインアップ・確認・サインイン・ユーザー取得・セッション確認・コード再送・パスワード再設定・再設定確認の 8 経路は、private な `authResult` で結果変換を共通化する。`CancellationException` は通常エラーへ変換する前に再スローする。これらの SDK callback の橋渡しは data 内の internal な [`AmplifyAuthFacade`](../feature/auth/data/src/main/kotlin/com/appvoyager/cloudphotos/data/auth/datasource/AmplifyAuthFacade.kt) に置き、internal コンストラクタからテスト用に差し替えられるようにする。サインアウト・退会・FCM cleanup はこの facade の対象に含めない。
- 一方、バックエンド REST 呼び出し（`DELETE /users`、`PUT /devices/token`、`DELETE /devices/token`）に使う [`awaitAmplifyRestCall`](../core/data/src/main/kotlin/com/appvoyager/cloudphotos/data/common/AmplifyRestClient.kt) は Amplify の `Cancelable` operation を受け取り、`coroutine.invokeOnCancellation { operation?.cancel() }` でキャンセルを伝播させる。
- いずれの経路でも、`runCatching` で受けた `CancellationException` を `AuthError.Unknown` や Worker の `retry` に落とさないよう、mapper 適用前に再スローする。
