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
  - **Screen 側での購読契約**: ViewModel と LifecycleOwner を key とする `LaunchedEffect` 内の `repeatOnLifecycle(Lifecycle.State.STARTED)` で `viewModel.effect.collect` し、`when` で全 branch を網羅して `else` を書かない。collect ブロック内で参照する navigation callback や `Context` は、再コンポジション後の最新値を参照できるよう `rememberUpdatedState` で包んでから呼ぶ。認証コードの timer 開始は repeat ブロックの外に置き、再収集で cooldown をリセットしない。
- **公開方式と配送保証**:
  - `StateFlow` は再購読時に最新値を再配信して二重遷移を起こすため Effect には使わない。
  - auth の 4 ViewModel を含め、`Channel<XxxEffect>(Channel.BUFFERED)` + `receiveAsFlow()` を使う。各 ViewModel の collector は 1 つとする。
  - collector 不在時の未受信 Effect は ViewModel 存続中にキューで待たせ、受信済み Effect は replay しない。プロセス終了後の復元や exactly-once は保証しない。受信と Lifecycle 停止が競合すると、handler 実行前にキャンセルされる可能性があり、表示中の Snackbar も中断し得る。
  - Login の SavedStateHandle message と Verification の email 欠落時 NavigateBack は、初回 collector が起動する前に送信されても保持する。Login の message key は読み取り直後に削除し、同じ Effect を再生成しない。
- **波及先**:
  - Effect や `XxxSnackbarMessage` に branch を追加した場合は、対応する Screen の `when` 分岐、`toMessage(context)`、`core/ui/src/main/res/values/strings.xml`（日英）、および `XxxViewModelTest` を更新する。

## テスト判断

- Effect の発火条件や送出順序を変更した場合は、`XxxViewModelTest` で collector 不在時の送信から初回収集・再収集まで検証する（[`docs/testing.md`](../../../../docs/testing.md) の「対象別のテストの書き方」の ViewModel 節を参照）。Lifecycle の停止復帰・callback 更新・timer 継続は Compose UI テストまたは手動確認で別に検証する。

## 検証観点

- 画面回転や再コンポジションで同じ遷移・Snackbar が再実行されないか。
- `LaunchedEffect` 内で古い callback インスタンスを掴んだままになっていないか（`rememberUpdatedState` を使っているか）。
- `when` に `else` を置いて追加 branch のコンパイルエラー検知を潰していないか。
