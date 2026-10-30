# UiState

画面が表示に使う状態を `uistate/` パッケージ（`app` は `ui/`）の `<Screen>UiState` にまとめる。

## 確認する対象

- [`feature/media/ui/src/main/kotlin/com/appvoyager/cloudphotos/ui/media/uistate/`](../../../../feature/media/ui/src/main/kotlin/com/appvoyager/cloudphotos/ui/media/uistate/)（`MediaUiState.kt`、`CameraUiState.kt`）
- [`feature/auth/ui/src/main/kotlin/com/appvoyager/cloudphotos/ui/auth/uistate/`](../../../../feature/auth/ui/src/main/kotlin/com/appvoyager/cloudphotos/ui/auth/uistate/)（`LoginUiState.kt`、`AuthFieldError.kt` ほか）
- [`app/src/main/kotlin/com/appvoyager/cloudphotos/ui/MainUiState.kt`](../../../../app/src/main/kotlin/com/appvoyager/cloudphotos/ui/MainUiState.kt)

## 実装時の注意

- **選択可能な 3 パターン**:
  1. **`data class` + nested `sealed class ScreenState`**（例: `MediaUiState`）: 排他的な画面状態（`None` / `PermissionRequired` / `Success` / `Error`）と、それと同時に成立する直交フラグ（`gridColumnCount`、`isSettingsDialogVisible`、`isSigningOut`、`isDeletingUser`）の両方がある画面に使う。ダイアログ表示やサインアウト中を `ScreenState` に入れると背景のグリッドが消えるため分離する。
  2. **`sealed class` / `sealed interface` 単体**（例: `CameraUiState`、`MainUiState`）: 画面全体が排他状態だけで表せる画面に使う。
  3. **フラットな `data class`**（例: `LoginUiState`）: 排他状態を持たないフォーム画面に使う。不要な `ScreenState` は作らない。
- **必須制約**:
  - `data class` 形式のフィールドにはすべてデフォルト値を持たせ、テストで検証対象フィールドだけを `copy` で差し替えられるようにする。
  - domain の model や value object（`List<Media>`、`GridColumnCount`）は UI 専用 DTO へ詰め替えずそのまま保持する。
  - 表示文字列（`String`）は UiState に入れず、`AuthFieldError` や `CameraUiState.ErrorType` のような型で保持して Screen 側でリソース解決する。
  - 入力値から一意に決まる派生値（例: `LoginViewModel.isFormValid`）は更新漏れを防ぐため UiState のフィールドに持たせず、ViewModel の computed property 等の派生値として公開する。
- **波及先**:
  - UiState のプロパティや `ScreenState` の branch を変更した場合は、購読している `XxxScreen` / `XxxContent`、Preview、および `XxxViewModelTest` の private Snapshot 型を合わせて更新する。

## テスト判断

- UiState のフィールドや初期値を変更した場合は、対応する `XxxViewModelTest` の初期状態検証と Snapshot 比較を更新する（[`docs/testing.md`](../../../../docs/testing.md) の「対象別のテストの書き方」の ViewModel 節を参照）。

## 検証観点

- 同時に成立する状態と排他的な状態が適切に分離されているか。
- 表示文言の `String` や Android リソース ID が UiState に入り込んでいないか。
- 派生値が UiState 内の重複フィールドになっていないか。
