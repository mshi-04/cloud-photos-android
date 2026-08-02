# Effect

`effect/` パッケージに置く、一回限りの動作を表す `sealed class`。navigation、snackbar、
ダイアログ起動など、状態として保持すると再実行されてしまうものを流す。命名は
`<Screen>Effect`。実物は
`feature/media/ui/src/main/kotlin/com/appvoyager/cloudphotos/ui/media/effect/MediaEffect.kt`、
`feature/media/ui/src/main/kotlin/com/appvoyager/cloudphotos/ui/media/effect/CameraEffect.kt`、
`feature/auth/ui/src/main/kotlin/com/appvoyager/cloudphotos/ui/auth/effect/LoginEffect.kt`。

## 定義

値を持つ branch は `data class`、持たない branch は `data object`。`MediaEffect` は
`ShowSnackbar(message)`、`NavigateToLogin`、`NavigateAfterAccountDeletion` の 3 つ。
`LoginEffect` は `NavigateToVerification(email)`、`NavigateToHome`、
`NavigateToForgotPassword`、`ShowSnackbar(message)`。navigation 引数は `Email` のような
domain の値オブジェクトのまま持たせ、`String` に落とさない。

`NavigateToLogin` と `NavigateAfterAccountDeletion` は Screen 側では同じ callback を呼ぶが、
branch は分けたままにしてある。発生元がサインアウトか退会かで、遷移先の指定が後から変わりうる。

`CameraEffect` は `ShowStorageFullDialog` のようにダイアログの起動も effect にする。
表示中フラグ自体は Screen のローカルな `remember` + `mutableStateOf` が持ち、
effect はそれを `true` にするきっかけだけを運ぶ。

## Snackbar は専用の message 型

`ShowSnackbar` が持つのは `String` ではなく `MediaSnackbarMessage`、`CameraSnackbarMessage`、
`AuthSnackbarMessage` といった専用の `sealed class`。branch は `Unknown`、`MediaLoadFailed`、
`SignOutFailed`、`DeleteUserFailed` のように「何が起きたか」を表す `data object` にする。

ViewModel が文字列を組むと `Context` と `R.string` が ViewModel へ入り込み、
ローカライズ経路と JVM テストの両方を壊す。message 型なら ViewModel テストは
値の等値比較で検証でき、リソース解決は Screen 側の拡張関数に留まる。

`AuthSnackbarMessage` は複数の auth 画面が共有し、さらに `key` property
(`javaClass.simpleName`) と `companion object` の `fromKey(key)` で `SavedStateHandle` 経由の
受け渡しにも対応している。`LoginViewModel` の `init` はこの key を読んで effect に変換し、
直後に `savedStateHandle.remove` する。消さないと画面復帰のたびに同じ snackbar が出る。

## 公開方法

新規の ViewModel は `Channel` を使う。private な `Channel<MediaEffect>(Channel.BUFFERED)` を
`receiveAsFlow()` で `Flow` として公開し、送信は `viewModelScope.launch` の中から
suspend の `send` で行う。`MediaViewModel` と `CameraViewModel` がこの形。

`StateFlow` にしない。最新値を保持するため、画面回転で collect が張り直された瞬間に
同じ navigation effect が再配信され、二重遷移になる。`Channel` は受信済みの要素を replay しない。

auth の 4 つの ViewModel (`LoginViewModel`、`ForgotPasswordViewModel`、
`ResetPasswordViewModel`、`VerificationCodeViewModel`) は先行実装で
`MutableSharedFlow(extraBufferCapacity = 1)` を `asSharedFlow()` で公開し、送信は `emit`。
auth 側を触るときはそのファイルの形に合わせ、新しい画面では `Channel` を選ぶ。

## 購読側との契約

Screen は `LaunchedEffect(Unit)` と `repeatOnLifecycle(Lifecycle.State.STARTED)` の中で
collect し、`when` で全 branch を列挙して `else` を書かない。
effect に branch を足したら、その effect を購読している Screen もコンパイルエラーになるので、
そこを手がかりに対応を追加する。
