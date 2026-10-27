# Screen / Content

ViewModel と接続する public な `XxxScreen` と、ViewModel を参照しない private な `XxxContent` の 2 段構成で画面を組み立てる。

## 確認する対象

- [`feature/media/ui/src/main/kotlin/com/appvoyager/cloudphotos/ui/media/screen/`](../../../../feature/media/ui/src/main/kotlin/com/appvoyager/cloudphotos/ui/media/screen/)（`MediaScreen.kt`、`CameraScreen.kt`、`MediaDetailScreen.kt`）
- [`feature/auth/ui/src/main/kotlin/com/appvoyager/cloudphotos/ui/auth/screen/`](../../../../feature/auth/ui/src/main/kotlin/com/appvoyager/cloudphotos/ui/auth/screen/)（`LoginScreen.kt` ほか）
- 文字列リソースとテーマ: [`core/ui/src/main/res/values/`](../../../../core/ui/src/main/res/values/)、[`docs/implementation-guidelines.md`](../../../../docs/implementation-guidelines.md) の「Compose」「Resource / Theme」
- effect 購読の契約: [`ui-event.md`](ui-event.md)

## 実装時の注意

- **必須制約**:
  - public な `XxxScreen` は `viewModel: XxxViewModel = hiltViewModel()` と遷移 callback（`onNavigateToXxx`）を受け取り、`NavController` は直接受け取らない。永続状態や通信を持たない画面（例: `MediaDetailScreen`）には ViewModel を作らず、必要な値と callback を直接受け取る。
  - private な `XxxContent` は ViewModel を参照せず、表示に必要な状態と `onXxx: () -> Unit` 形式の callback のみを受け取る。
  - 画面復帰時の処理は `LifecycleResumeEffect(Unit)` に置き、末尾を `onPauseOrDispose` で閉じる（例: `MediaScreen` の `onScreenResumed()` と通知権限確認、`CameraScreen` の `cameraPreviewManager.stopCamera()`）。
- **既存実装のパターン**:
  - 権限要求（`rememberLauncherForActivityResult`）や復帰時の再確認トリガーには `mutableIntStateOf(0)` の key（`permissionCheckKey` / `resumeKey`）をインクリメントして `LaunchedEffect(key)` を再実行させている。
  - 画面固有の sub-component は同一ファイル内の private Composable に置き、複数画面・複数 feature で共有する部品のみ `component/` や `core:ui` に切り出す。
  - Preview は Hilt なしで描画できるよう private な `XxxContent` を `CloudPhotosTheme` で包み、主要な状態（`Success` / `Error` / `PermissionRequired` など）ごとに用意する。
- **波及先**:
  - Screen の引数（navigation callback）を変更した場合は [`app/src/main/kotlin/com/appvoyager/cloudphotos/ui/NavGraph.kt`](../../../../app/src/main/kotlin/com/appvoyager/cloudphotos/ui/NavGraph.kt) の呼び出し箇所も更新する。

## テスト判断

- Screen / Content 単体の見た目調整のみであれば ViewModel テストの追加は不要。
- 画面操作に伴って ViewModel の状態遷移や effect 発火条件が変わる場合は、対応する `XxxViewModelTest` を追加・更新する。

## 検証観点

- 状態ごと（ローディング、空、エラー、権限未付与、コンテンツ表示）の分岐が漏れなく描画されるか。
- `LifecycleResumeEffect` の `onPauseOrDispose` でカメラ等のリソース停止が漏れていないか。
- 文字列解決が `core.ui.R` のリソース経由になっており、ハードコード文字列がないか。
