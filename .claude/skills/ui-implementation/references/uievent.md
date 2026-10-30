# UI Event（Callback / Effect / Snackbar Message）

画面操作の伝達（UI callback）と、ViewModel から 1 回限り発火する動作（Effect・Snackbar・画面遷移）の契約。

## 確認する対象

- [`feature/media/ui/src/main/kotlin/com/appvoyager/cloudphotos/ui/media/effect/`](../../../../feature/media/ui/src/main/kotlin/com/appvoyager/cloudphotos/ui/media/effect/)（`MediaEffect.kt`、`MediaSnackbarMessage.kt`、`CameraEffect.kt`、`CameraSnackbarMessage.kt`）
- [`feature/auth/ui/src/main/kotlin/com/appvoyager/cloudphotos/ui/auth/effect/`](../../../../feature/auth/ui/src/main/kotlin/com/appvoyager/cloudphotos/ui/auth/effect/)（`LoginEffect.kt`、`AuthSnackbarMessage.kt` ほか）
- 購読側の Screen: [`MediaScreen.kt`](../../../../feature/media/ui/src/main/kotlin/com/appvoyager/cloudphotos/ui/media/screen/MediaScreen.kt)、[`LoginScreen.kt`](../../../../feature/auth/ui/src/main/kotlin/com/appvoyager/cloudphotos/ui/auth/screen/LoginScreen.kt)

## 実装時の注意

- **必須制約**:
  - **Effect の定義**: `effect/` パッケージに `<Screen>Effect` の `sealed class` として定義する。画面遷移の引数は `Email` などの domain value object のまま保持し、生の `String` に落とさない。
  - **Snackbar message 型**: `ShowSnackbar` には生の `String` やリソース ID ではなく、`MediaSnackbarMessage` / `CameraSnackbarMessage` / `AuthSnackbarMessage` のような専用 `sealed class` を持たせる。文字列への変換は Screen ファイル末尾の `toMessage(context)` 拡張関数で行う。
  - **SavedStateHandle 経由の受け渡し**: 画面遷移を跨いで Snackbar を表示する `AuthSnackbarMessage` は `key` と `AuthSnackbarMessage.fromKey(key)` で受け渡し、`LoginViewModel` の `init` で読み取った直後に必ず `savedStateHandle.remove` する（消さないと画面復帰のたびに再表示される）。
  - **Screen 側での購読契約**: `LaunchedEffect(Unit)` 内の `repeatOnLifecycle(Lifecycle.State.STARTED)` で `viewModel.effect.collect` し、`when` で全 branch を網羅して `else` を書かない。collect ブロック内で参照する navigation callback や `Context` は、再コンポジション後の最新値を参照できるよう `rememberUpdatedState` で包んでから呼ぶ。
- **選択可能な公開方式と既存コードの扱い**:
  - `StateFlow` は再購読時に最新値を再配信して二重遷移を起こすため Effect には使わない。
  - 新規 ViewModel（および既存の `MediaViewModel` / `CameraViewModel`）では `Channel<XxxEffect>(Channel.BUFFERED)` + `receiveAsFlow()` を使う。
  - 既存の auth 系 4 ViewModel は `MutableSharedFlow(extraBufferCapacity = 1)` + `asSharedFlow()` で実装されている。既存 auth ViewModel の小規模修正ではそのファイルの方式に合わせ、無関係な全面書き換えを混ぜない。
- **波及先**:
  - Effect や `XxxSnackbarMessage` に branch を追加した場合は、対応する Screen の `when` 分岐、`toMessage(context)`、`core/ui/src/main/res/values/strings.xml`（日英）、および `XxxViewModelTest` を更新する。

## テスト判断

- Effect の発火条件や送出順序を変更した場合は、`XxxViewModelTest` で collector を起動して受信リストを検証する（[`docs/testing.md`](../../../../docs/testing.md) の「対象別のテストの書き方」の ViewModel 節を参照）。

## 検証観点

- 画面回転や再コンポジションで同じ遷移・Snackbar が再実行されないか。
- `LaunchedEffect(Unit)` 内で古い callback インスタンスを掴んだままになっていないか（`rememberUpdatedState` を使っているか）。
- `when` に `else` を置いて追加 branch のコンパイルエラー検知を潰していないか。
