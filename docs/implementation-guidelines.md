# Implementation Guidelines

この文書は Kotlin、Compose、ViewModel、coroutines、DI、resource、コメント、ログ、エラー処理の基本方針をまとめます。
モジュール境界と責務配置は [`docs/architecture.md`](architecture.md) を正本とします。

## Kotlin

- ガード条件は早期リターンで扱い、ネストを深くしない。
- ドメインの意味を持つ値は生プリミティブで広げず、`@JvmInline value class` の値オブジェクトを使う。`of()` 内では複数の制約を別々の `require` に分け、失敗理由が判別できるメッセージを持たせる。
- 状態や依存を持たない純粋な変換は `internal object` の関数としてまとめる。
- 現在時刻の取得などテストで固定したい外部値は、[`Clock`](../core/common/src/main/kotlin/com/appvoyager/cloudphotos/domain/common/Clock.kt) の注入または関数引数で受け取り、`System.currentTimeMillis()` を直接呼ばない。

## Compose

- 画面は ViewModel と接続する public な `XxxScreen` と、表示値と callback のみを受け取る private な `XxxContent` の 2 段構成を基本とする（永続状態も通信も持たない [`MediaDetailScreen`](../feature/media/ui/src/main/kotlin/com/appvoyager/cloudphotos/ui/media/screen/MediaDetailScreen.kt) のような画面には ViewModel を作らない）。
- `XxxScreen` は `NavController` を受け取らず、`onNavigateToXxx` のような遷移 callback を受け取る。
- `remember` は `SnackbarHostState`、ダイアログ開閉、権限再確認キーなどの画面限定の一時状態に限定する。永続状態や通信状態は ViewModel / domain / data で管理する。
- 画面復帰時の処理やカメラ停止などのライフサイクル連動は `LifecycleResumeEffect` と `onPauseOrDispose` で扱う。
- Preview は Hilt なしで描画できるよう `XxxContent` を対象にし、`CloudPhotosTheme` で包んで状態ごとに用意する。
- Lazy 系リストや画像・動画表示では、Composable 内に重い変換や不要な state read を置かない。

## ViewModel と UI State / Effect

- **UI State**:
  - ViewModel は `MutableStateFlow` を `asStateFlow()` で公開し、更新は `_uiState.update { it.copy(...) }` を使う。
  - 画面の状態構造に合わせ、「`data class` + nested `sealed class ScreenState`」（排他状態とダイアログ等の直交フラグが共存する [`MediaUiState`](../feature/media/ui/src/main/kotlin/com/appvoyager/cloudphotos/ui/media/uistate/MediaUiState.kt)）、「`sealed class` / `sealed interface` 単体」（排他状態のみの [`CameraUiState`](../feature/media/ui/src/main/kotlin/com/appvoyager/cloudphotos/ui/media/uistate/CameraUiState.kt)、[`MainUiState`](../app/src/main/kotlin/com/appvoyager/cloudphotos/ui/MainUiState.kt)）、「フラットな `data class`」（フォーム画面の [`LoginUiState`](../feature/auth/ui/src/main/kotlin/com/appvoyager/cloudphotos/ui/auth/uistate/LoginUiState.kt) など）を使い分ける。
  - 入力値から一意に決まる派生値（`isFormValid` など）は UiState のフィールドに持たせず、ViewModel の computed property として公開する。
- **UI Event（入力）**:
  - 画面からの操作は ViewModel の public 関数（`onEmailChanged`、`onScreenResumed` など）として受け取り、単一の `onEvent(event)` ディスパッチャは作らない。
- **UI Effect（一回限りの動作）**:
  - 画面遷移、Snackbar 表示、ダイアログ起動トリガーなどの一回限りの動作は `<Screen>Effect` の `sealed class` で表す。`StateFlow` に入れると再収集時に二重実行されるため使わない。
  - 新規の ViewModel は `Channel<XxxEffect>(Channel.BUFFERED)` を `receiveAsFlow()` で公開する。先行実装である `feature:auth:ui` の 4 つの ViewModel は `MutableSharedFlow(extraBufferCapacity = 1)` を使っており、既存ファイルを触る場合はそのファイルの形に合わせる。
  - Snackbar の内容は ViewModel で文字列に解決せず、`MediaSnackbarMessage` / `CameraSnackbarMessage` / `AuthSnackbarMessage` などの型で送る。
  - Screen 側は `LaunchedEffect(Unit)` 内の `repeatOnLifecycle(Lifecycle.State.STARTED)` で `effect.collect` し、`when` で全分岐を網羅する（`else` を書かない）。collect ブロック内から呼ぶ親 callback や `LocalContext.current` は `rememberUpdatedState` で包んで参照する。
- **Job 管理と多重実行ガード**:
  - 張り直す Flow 購読は `Job` を保持して前の Job を `cancel()` する。Room や WorkManager を触る同期処理の再開（`MediaViewModel.onScreenResumed`）では、前の Job を `cancelAndJoin()` してから次の処理を始める。
  - サインアウトや送信などの非同期操作は、`isLoading` / `isSigningOut` 等のフラグで多重実行を防ぎ、`finally` で確実に解除する。

## Coroutines / Flow

- 公開する suspend 関数は main-safe にする。
- `Dispatchers.IO` への切り替えは、ContentResolver や MediaStore 書き込みなど実際にブロッキング I/O を持つ層で行う。SDK 側が非同期で動く Amplify 呼び出しには不要な dispatcher 切り替えを入れない。
- Flow は収集側のライフサイクルを意識し、ViewModel の外で無期限の収集を作らない。

## エラー処理と CancellationException

- **`CancellationException` の再スロー**:
  - coroutine のキャンセルは `CancellationException` で伝播するため、決して握り潰さない。
  - `try / catch (e: Exception)` では先に `catch (e: CancellationException) { throw e }`（または `if (e is CancellationException) throw e`）を置く。
  - `runCatching` は `CancellationException` も捕捉するため、`onFailure`、`fold` の失敗側、`exceptionOrNull()` の直後、およびネストした cleanup の `runCatching` でも必ず `CancellationException` を再スローする。
- **Result 型と例外の使い分け**:
  - 認証やクラウドアップロードなど、provider 由来の予期される失敗を呼び出し側（ViewModel や Worker）が分岐して扱う操作は、domain の Result / Error 型（`AuthResult`、`UploadResult`、`SavePhotoResult`）で表す。
  - ローカル DB 操作（`LocalUploadRecordsRepository`）などは Result 型で包まず、生の値や `Unit` を返して例外を伝播させる。同じ API で Result を返しつつ通常失敗を例外でも投げる形にしない。
  - プログラミングエラー、引数制約違反（`require`）、回復不能な初期化失敗は例外で表す。
- **Provider 例外の隔離**:
  - Amplify、Cognito、Firebase、Room、HTTP の例外型やレスポンス型を domain / UI へ漏らさず、data 層の Mapper で domain error へ変換する。`AuthError` や `UploadError` に原因の `Throwable` を持たせない。

## Hilt / DI

- クラスは `@Inject constructor`（Worker は `@HiltWorker` + `@AssistedInject`）を基本とする。
- interface の `@Binds` や外部インスタンスの `@Provides` は `:app` の `di/` パッケージの Module に置き、適切なコンポーネントとスコープ（`@Singleton` など）を合わせる。
- テストから差し替えたい DataStore 等は、`internal` コンストラクタで実体を受け取り、`@Inject` コンストラクタから委譲する形をとってよい（例: [`SettingsDataSourceImpl`](../feature/media/data/src/main/kotlin/com/appvoyager/cloudphotos/data/media/datasource/SettingsDataSourceImpl.kt)）。

## Resource / Theme

- 表示文字列は直書きせず、[`core/ui/src/main/res/values/strings.xml`](../core/ui/src/main/res/values/strings.xml) などの string resource に置く。
- Composable 内では `stringResource()`、effect の collect ブロック内では `Context.getString()` を使う拡張関数で解決する。
- 色、タイポグラフィ、シェイプは `:core:ui` の Material 3 テーマを優先する。

## コメント

テストコードを除くすべてのファイル（Kotlin 本番コード、Gradle スクリプト、GitHub Actions workflow、Fastlane、設定ファイル）で、字面から読み取れる説明をコメントに書かない。意図は名前、値オブジェクト、早期リターン、関数分割、job 名・step 名で表す。

字面から読み取れない以下の事情に限りコメントを書く。

- 呼び出し側から見えない並行性・実行順序・冪等性の制約（例: 退会時に Cognito より先に backend 削除を行う理由）。
- 一見不具合に見える意図的な挙動や、外部仕様に対する workaround の理由（例: 削除時に S3 孤立オブジェクトを許容する理由、DAO SQL に enum 名リテラルを埋め込む注意）。
- `@Suppress` の理由やライセンス表記など、ツール・法的要件が求めるもの。
- 依存や設定の pin に添える注記。

背景、トレードオフ、採用しなかった案は PR の説明か `docs/` に書き、コードへ残さない。
テストコードの AAA ラベルと観点コメントは [`docs/testing.md`](testing.md) を正本とする。
既存ファイル全体の一括コメント整理は、意図した作業でない限り行わない。

## ログ

- プロダクションコードに `android.util.Log` や一時デバッグ出力を残さない。
- デバイストークン、JWT、userId、ファイルパス、バケット名、URL、署名付き URL をログへ出力しない。
