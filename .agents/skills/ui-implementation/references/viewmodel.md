# ViewModel

`@HiltViewModel` で UseCase または domain abstraction を受け取り、`uiState` と `effect` を公開する。

## 確認する対象

- [`feature/media/ui/src/main/kotlin/com/appvoyager/cloudphotos/ui/media/viewmodel/`](../../../../feature/media/ui/src/main/kotlin/com/appvoyager/cloudphotos/ui/media/viewmodel/)（`MediaViewModel.kt`、`CameraViewModel.kt`）
- [`feature/auth/ui/src/main/kotlin/com/appvoyager/cloudphotos/ui/auth/viewmodel/`](../../../../feature/auth/ui/src/main/kotlin/com/appvoyager/cloudphotos/ui/auth/viewmodel/)（`LoginViewModel.kt` ほか）
- [`app/src/main/kotlin/com/appvoyager/cloudphotos/ui/MainViewModel.kt`](../../../../app/src/main/kotlin/com/appvoyager/cloudphotos/ui/MainViewModel.kt)
- 基本規約: [`docs/implementation-guidelines.md`](../../../../docs/implementation-guidelines.md) の「ViewModel と UI State / Effect」「Coroutines / Flow」「エラー処理と CancellationException」

## 実装時の注意

- **必須制約**:
  - 依存先は UseCase または domain abstraction（例: `CameraViewModel` が注入する `CapturedPhotoWriter`）までに限り、Repository 実装、DataSource、DAO、SDK client、`Context` は注入しない。判断基準は [`docs/architecture.md`](../../../../docs/architecture.md) の「UseCase と Domain Abstraction」に従う。
  - `uiState` は `_uiState.update { it.copy(...) }`（または sealed state の置換）で更新し、`_uiState.value` への直接代入は行わない。
  - UI 操作の受け口は `onScreenResumed()`、`onEmailChanged(value)` のような個別 public 関数とし、単一の `onEvent(event)` ディスパッチャは導入しない。
  - `CancellationException` は catch 節や `runCatching` で握り潰さず再スローする。
- **プロジェクト固有の契約**:
  - **起動時のセッション確認**: `MainViewModel` は `uiState` / `isCheckingSession` / `isRetrying` をそれぞれ private MutableStateFlow / public read-only StateFlow で公開する。初期値は None / true / false。確認中の Job が active なら重複呼び出しを無視し、再試行は SessionCheckError から開始するときだけフラグを立てる。成功・失敗・キャンセル後のフラグ解除は finally に置く。feature の認証状態監視や navigation の状態機械を追加しない。
  - **認証 Effect の配送**: 4 つの auth ViewModel は `Channel<XxxEffect>(Channel.BUFFERED)` / `receiveAsFlow()` を使い、未受信の Effect を ViewModel 存続中に保持する。collector は各 ViewModel に 1 つとし、Screen は STARTED 中に収集する。Login の初期 message と Verification の email 欠落時 NavigateBack も初回収集まで保持する。受信と停止の競合・受信済み Snackbar の中断・プロセス終了後の配送は保証しない。詳細は [`uievent.md`](uievent.md) に従う。
  - **認証フォームの派生値**: ForgotPassword の `isFormValid`、ResetPassword の `isCodeComplete` / `isFormValid` / `isResendEnabled`、VerificationCode の `isCodeComplete` / `isResendEnabled` は、所有 ViewModel の computed property として最新の `_uiState.value` から計算する。UiState に Boolean を保存したり、別の StateFlow で二重管理したりしない。Screen に渡す派生値には、UiState を明示的に受け取る ViewModel の判定関数を用意し、getter も同じ判定関数へ委譲する。Screen は購読済みの `uiState` を判定関数に渡し、入力値・timer・loading と派生値を同じ snapshot から Content へ渡す。
  - **購読の張り直しと同期 Job の直列化**: `MediaViewModel.loadMediaList()` のように購読を張り直す場合は前の `Job` を `cancel()` してから `launch` する。一方、`MediaViewModel.onScreenResumed()` は Room と WorkManager を触るため、新しい `launch` 内で前の `syncJob?.cancelAndJoin()` を待ってから同期処理を順に実行する。
  - **画面復帰の間引きとテスト差し替え口**: `LifecycleResumeEffect` は権限ダイアログの開閉でも発火するため、`MediaViewModel.onScreenResumed()` は `MIN_RESUME_INTERVAL_MS` 未満の連続呼び出しを無視する。時刻取得は Hilt へ漏らさず JVM テストで差し替えられるよう `internal var elapsedRealtimeProvider: () -> Long` と `internal const val MIN_RESUME_INTERVAL_MS` を使う。
  - **多重実行防止フラグ**: サインアウト・退会・認証送信などの非同期処理は `isSigningOut` / `isDeletingUser` / `isLoading` で二重起動を防ぎ、解除は必ず `finally` に置く。
- **波及先**:
  - `AuthError` の分岐を変える場合は [`docs/authentication.md`](../../../../docs/authentication.md) のエラー表現（アカウント列挙防止を含む）と、4 つの auth ViewModel（`Login` / `ForgotPassword` / `ResetPassword` / `VerificationCode`）の整合を確認する。

## テスト判断

- 状態遷移、effect 送信、エラー分岐、二重実行ガード、間引き処理を変更した場合は `src/test/.../viewmodel/XxxViewModelTest` を追加・更新する。書き方は [`docs/testing.md`](../../../../docs/testing.md) の「対象別のテストの書き方」の ViewModel 節に従う。

## 検証観点

- `CancellationException` が失敗状態や Snackbar に化けていないか。
- 進行中フラグの解除が `finally` で保証されているか。
- 連続タップや画面復帰の連発で同期・送信が重複実行されないか。
