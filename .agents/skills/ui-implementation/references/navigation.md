# Navigation

トップレベルの画面遷移と route 定義は `app` モジュールが所有し、各 feature の Screen は navigation callback だけを受け取る。

## 確認する対象

- [`app/src/main/kotlin/com/appvoyager/cloudphotos/ui/NavGraph.kt`](../../../../app/src/main/kotlin/com/appvoyager/cloudphotos/ui/NavGraph.kt)（`NavGraph`、`AuthRoute`、`MediaRoute`）
- [`app/src/main/kotlin/com/appvoyager/cloudphotos/ui/MainActivity.kt`](../../../../app/src/main/kotlin/com/appvoyager/cloudphotos/ui/MainActivity.kt) / [`MainViewModel.kt`](../../../../app/src/main/kotlin/com/appvoyager/cloudphotos/ui/MainViewModel.kt)
- [`docs/architecture.md`](../../../../docs/architecture.md) の「`:app`（Composition Root）」

## 実装時の注意

- **必須制約**:
  - トップレベルの `NavHost` と route 定義（`AuthRoute` / `MediaRoute`）は `app` モジュールの `NavGraph.kt` に置き、feature モジュールへ route 文字列や `NavController` の知識を広げない。
  - route パラメータに `Email` や `MediaId` などの値を埋め込む際は `Uri.encode(...)` を通し、受け取り側では空チェックのうえで value object（`MediaId.of(...)` 等）や `SavedStateHandle` で復元する。
  - ログイン成功時（`URI_LOGIN` → `HOME`）、確認コード完了時（`URI_VERIFICATION` → `HOME`）、サインアウト・退会時（`HOME` → `login()`）、パスワード再設定完了時（`URI_RESET_PASSWORD` → `login(message)`）は、`popUpTo(...) { inclusive = true }` で認証前後の back stack を適切にクリアする。
- **プロジェクト固有の契約**:
  - **起動時の startDestination 決定**: `MainActivity` はスプラッシュ保持中に `MainViewModel.uiState`（`GetCurrentAuthSessionUseCase` の結果）を監視し、`Authenticated` なら `AuthRoute.HOME`、`Unauthenticated` なら `AuthRoute.URI_LOGIN` を `NavGraph` の `startDestination` に渡す。`SessionCheckError` の場合は `NavGraph` を描画せず再試行画面を表示する。
  - **HOME と詳細画面の状態共有**: `MediaRoute.URI_DETAIL` は独自の ViewModel を持たず、`navController.getBackStackEntry(AuthRoute.HOME)` を親 entry として `MediaViewModel` を取得し、`MediaDetailScreen` に `mediaList` と `initialMediaId` を渡す。`HOME` が back stack に存在する前提を崩さない。
- **波及先**:
  - route 引数のキー名（`"email"`、`"message"`、`"mediaId"`）を変える場合は、`NavGraph.kt` の `navArgument` と対象 ViewModel の `SavedStateHandle` 読み取りキー（および ViewModel テストの `SavedStateHandle` 生成）を同時に更新する。

## テスト判断

- ViewModel が `SavedStateHandle` から route 引数を受け取る処理を変更した場合は、対応する `XxxViewModelTest` で `SavedStateHandle(mapOf(...))` を渡すテストを追加・更新する。

## 検証観点

- 認証完了後やサインアウト後にシステム Back 操作で前の認証状態の画面へ戻れないか。
- URL 特殊文字を含む引数（メールアドレスの `+` 等）が `Uri.encode` されて安全に渡るか。
- `startDestination` や back stack 構造を変更した場合、その影響範囲を報告しているか。
