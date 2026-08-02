# UiState

`uistate/` パッケージに置く、画面が表示に使う状態のまとまり。命名は `<Screen>UiState`。
形は 1 つではなく、排他状態の有無で 3 通りある。実物は
`feature/media/ui/src/main/kotlin/com/appvoyager/cloudphotos/ui/media/uistate/MediaUiState.kt`、
`feature/media/ui/src/main/kotlin/com/appvoyager/cloudphotos/ui/media/uistate/CameraUiState.kt`、
`feature/auth/ui/src/main/kotlin/com/appvoyager/cloudphotos/ui/auth/uistate/LoginUiState.kt`。

## data class + nested ScreenState

排他的な表示状態と、それに直交するフラグが両方ある画面はこの形。`MediaUiState` は
`data class` で、`screenState`、`gridColumnCount`、`isSettingsDialogVisible`、
`isSigningOut`、`isDeletingUser` を持つ。`screenState` の型は同じ `data class` の内側に
nested した `sealed class ScreenState` で、branch は `None`、`PermissionRequired`、
`Success(mediaList)`、`Error(error)`。値を持たないものは `data object`、持つものは `data class`。

同時に成立しない表示は `ScreenState` の branch にする。Screen 側の `when` に `else` を
書かずに済み、branch 追加が対応漏れをコンパイルエラーにする。

ダイアログ表示や処理中は `ScreenState` と同時に成立するため boolean フィールドに残す。
`isSigningOut` を `ScreenState` の branch にすると、サインアウト中はグリッドが消える。

`ScreenState` を nested にしているのは、外から参照されると他画面へ流用され、
branch 追加の影響範囲が読めなくなるため。

## sealed class 単体

画面全体が排他状態だけで表せるならフィールドを持たせない。`CameraUiState` は
`sealed class` で `CheckingPermission`、`PermissionRequired`、`Ready`、`Capturing`、
`Error(type)` を持ち、`Error` の中身は nested の `enum class ErrorType`
(`STORAGE_FULL` / `CAMERA_UNAVAILABLE` / `CAPTURE_FAILED`)。
`app/src/main/kotlin/com/appvoyager/cloudphotos/ui/MainUiState.kt` は同じ発想で
`sealed interface` を使い、`None` / `Authenticated` / `Unauthenticated` / `SessionCheckError` を並べる。

## フラットな data class

排他状態を持たないフォーム画面はネストなしの `data class` にする。`LoginUiState` は
`email`、`password`、`isPasswordVisible`、`isLoading`、`emailError`、`passwordError` の
6 フィールドだけで、`ScreenState` に相当するものを持たない。
排他状態がないのに `ScreenState` を足さない。

## デフォルト値

`data class` 形式のフィールドはすべてデフォルト値を持つ。`MediaUiState` の
`gridColumnCount` は `GridColumnCount.of(3)`、boolean は `false`、`screenState` は
`ScreenState.None`。テストが引数なしのコンストラクタから始めて、検証したいフィールドだけ
`copy` で差し替えられる。

## 保持してよい値

domain の model と値オブジェクトはそのまま入れる (`List<Media>`、`GridColumnCount`)。
UI 専用の DTO へ詰め替えない。

表示文字列は入れない。field error は `AuthFieldError` のような `sealed class` で持ち、
文字列解決は Screen 側の拡張関数が行う。`AuthFieldError` は Login / Verification /
ResetPassword が共有するため `uistate/AuthFieldError.kt` に独立して置かれている。
単一画面でしか使わないものは UiState の nested にする。

## 派生値は持たせない

`LoginViewModel` の `isFormValid` は UiState のフィールドではなく ViewModel の
computed property で、Screen が `viewModel.isFormValid` として Content へ渡す。
UiState に持たせると入力フィールドと同期して更新する義務が生まれ、更新漏れが不整合になる。
