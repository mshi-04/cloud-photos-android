# ViewModel

`viewmodel/` パッケージに置く `@HiltViewModel` 付きの `ViewModel`。UseCase を
constructor injection で受け取り、公開するのは `uiState` と `effect` の 2 つだけ。実物は
`feature/media/ui/src/main/kotlin/com/appvoyager/cloudphotos/ui/media/viewmodel/MediaViewModel.kt`、
`feature/media/ui/src/main/kotlin/com/appvoyager/cloudphotos/ui/media/viewmodel/CameraViewModel.kt`、
`feature/auth/ui/src/main/kotlin/com/appvoyager/cloudphotos/ui/auth/viewmodel/LoginViewModel.kt`。

## 骨格

state は private な `MutableStateFlow` を `asStateFlow()` で `StateFlow` として公開する。
更新は必ず `_uiState.update { it.copy(...) }` を使い、`_uiState.value` への直接代入はしない。
UiState が sealed class そのものの場合 (`CameraViewModel`) は
`MutableStateFlow<CameraUiState>(CameraUiState.CheckingPermission)` と型引数を明示する。

effect の公開方法は `effect` 側の規約に従う。

イベントの受け口は `onScreenResumed()`、`loadMediaList()`、`onEmailChanged(value)` のような
public 関数。UI event を表す sealed class や `onEvent(event)` のディスパッチャは持たない。
1 行で終わる更新は `fun onShowSettingsDialog() = _uiState.update { ... }` の式形式にする。

依存は UseCase か domain の abstraction まで。DAO、SDK client、Repository 実装は注入しない。

## エラーハンドリング

Flow の購読は `.catch { }` を挟んでから `.collect { }` する。`MediaViewModel.loadMediaList()`
は `catch` の中で `SecurityException` を `ScreenState.PermissionRequired` に、それ以外を
`ScreenState.Error(cause)` と `ShowSnackbar` に振り分ける。

suspend の 1 回呼び出しは `runCatching { }.onFailure { }` か `try` / `catch (e: Exception)` /
`finally`。いずれの形でも `CancellationException` は握り潰さず、判定して rethrow する。
握り潰すと ViewModel の破棄や job の cancel まで「失敗」として扱われ、画面を離れた直後に
snackbar が出る。分類の詳細は `../../../../docs/error-handling-guide.md`。

`AuthResult` を返す UseCase は `when` で `AuthResult.Success` / `AuthResult.Error` を分ける。
`LoginViewModel` は `AuthError` の branch ごとに、field error として UiState に載せるか
snackbar effect として送るかを決めている。

## Job の管理

張り直す購読は Job を property に保持し、前の Job を明示的に止める。
`loadMediaList()` は `mediaListJob?.cancel()` してから `viewModelScope.launch` し直す。

`onScreenResumed()` は `cancel()` ではなく、新しい `launch` の内側で前の Job を
`cancelAndJoin()` してから同期処理を順に呼ぶ。同期は Room と WorkManager を触るため、
前回の完了を待たずに次を始めると重複登録になる。

`onScreenResumed()` にはもう 1 つ、`MIN_RESUME_INTERVAL_MS` 未満の連続呼び出しを
早期 return で捨てる間引きがある。`LifecycleResumeEffect` は permission ダイアログの
開閉でも発火するため、これがないと画面を開いただけで同期が何度も走る。

## 進行中フラグ

サインアウトのように多重実行を防ぎたい処理は、UiState の boolean で自分を守る。
`MediaViewModel.signOut()` / `deleteUser()` は `launch` の内側で `isSigningOut` /
`isDeletingUser` を見て `return@launch` し、`true` を立ててから処理し、`finally` で戻す。
`LoginViewModel.onSignIn()` / `onSignUp()` は `launch` の外側で `isLoading` と
`validateForm()` を判定してから `launch` する。どちらの形でも解除は `finally` に置く。

## テスト用の差し替え口

`MediaViewModel` は `internal var elapsedRealtimeProvider: () -> Long` を持ち、既定値で
`SystemClock.elapsedRealtime()` を返す。`SystemClock` は JVM テストで動かないが、
constructor 引数にすると Hilt module と本番の呼び出し側にテスト都合が漏れる。
`internal var` なら `MediaViewModelTest` が代入で置き換えられる。
閾値の `MIN_RESUME_INTERVAL_MS` も `companion object` の `internal const` にして、
テストが同じ定数を参照できるようにしてある。
