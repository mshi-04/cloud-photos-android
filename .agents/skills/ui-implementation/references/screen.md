# Screen

`screen/` パッケージに置く Compose の画面本体。1 ファイルに public な Screen と
private な Content、その画面専用の sub-component をまとめる。実物は
`feature/media/ui/src/main/kotlin/com/appvoyager/cloudphotos/ui/media/screen/MediaScreen.kt`、
`feature/media/ui/src/main/kotlin/com/appvoyager/cloudphotos/ui/media/screen/CameraScreen.kt`、
`feature/auth/ui/src/main/kotlin/com/appvoyager/cloudphotos/ui/auth/screen/LoginScreen.kt`。

## 2 段構成

public な `MediaScreen` / `LoginScreen` / `CameraScreen` は、第 1 引数にデフォルト値
`hiltViewModel()` を付けた `viewModel` を取り、残りは `onNavigateToCamera`、
`onNavigateToLogin`、`onMediaClick` のような navigation callback だけを取る。
NavController は受け取らない。state は `viewModel.uiState` を
`collectAsStateWithLifecycle()` で読み、`SnackbarHostState` は `remember` で持つ。
`Scaffold` を組み立てるのもこの層。

private な `MediaContent` / `LoginContent` は `viewModel` を一切参照せず、表示に必要な値と
`onX: () -> Unit` 形式の callback だけを受け取る。`MediaContent` は `MediaUiState` 全体では
なく `screenState` と `gridColumnCount` を個別に受け取り、`LoginContent` は `email`、
`password`、`isPasswordVisible`、`emailError`、`isFormValid`、`isLoading` と展開して受け取る。
Content が ViewModel から切れているため、Preview と将来の UI テストが同じ関数を直接呼べる。
Screen 側は `viewModel.onShowSettingsDialog()` のような呼び出しを lambda に包んで渡す。

ViewModel を持たない Screen もある。`MediaDetailScreen` は `mediaList`、`initialMediaId`、
`onNavigateBack` を直接受け取る。永続状態も通信もない画面には ViewModel を作らない。

## effect の購読

effect は `LaunchedEffect(Unit)` の中で `LocalLifecycleOwner.current` の
`repeatOnLifecycle(Lifecycle.State.STARTED)` に入り、`viewModel.effect.collect` で受ける。
分岐は `when` で全 branch を列挙し `else` を書かない。effect 型に branch を足したときに
Screen 側の対応漏れをコンパイルエラーにするため。

collect ブロックから呼ぶ callback と `LocalContext.current` は、事前に
`rememberUpdatedState` で包んだ `latestOnNavigateToLogin`、`latestContext` を経由して呼ぶ。
`LaunchedEffect(Unit)` は再コンポジションで再起動しないため、直接参照すると初回の
callback インスタンスを掴んだままになり、親が渡し直した新しい callback が呼ばれない。

`MediaScreen` は `NavigateToLogin` と `NavigateAfterAccountDeletion` の両方で
同じ `onNavigateToLogin` を呼ぶが、branch は分けたまま列挙している。

## Lifecycle と permission

画面復帰時の処理は `LifecycleResumeEffect(Unit)` に置き、末尾を `onPauseOrDispose` で閉じる。
`MediaScreen` はここで `viewModel.onScreenResumed()` と通知権限の要求を行い、
`CameraScreen` は `onPauseOrDispose` で `cameraPreviewManager.stopCamera()` を呼ぶ。

permission は `rememberLauncherForActivityResult` と
`ActivityResultContracts.RequestPermission` / `RequestMultiplePermissions` で扱う。
再確認のトリガーには `mutableIntStateOf(0)` の key を持ち、インクリメントで
`LaunchedEffect(key)` を再実行させる。`MediaScreen` の `permissionCheckKey`、
`CameraScreen` の `resumeKey` がこれにあたる。

## 表示文字列の解決

Snackbar と field error の文字列解決はファイル末尾の private 拡張関数が担う。
collect ブロックから呼ぶものは `Context.getString` を使う非 Composable 関数
(`MediaSnackbarMessage.toMessage(context)`、`AuthSnackbarMessage.toMessage(context)`)、
Composable から呼ぶものは `stringResource` を使う `@Composable` 関数
(`AuthFieldError.toEmailMessage()`、`toPasswordMessage()`) にする。
文字列 ID の参照元は常に `com.appvoyager.cloudphotos.core.ui.R`。

## sub-component と Preview

`MediaAppBar`、`MediaGridItem`、`EmptyContent`、`ErrorContent`、
`PermissionRequiredContent`、`LogoutConfirmDialog`、`WithdrawConfirmDialog` は
すべて同一ファイル内の private な Composable。複数 feature で使うものだけ
`ui/media/component/`、`ui/auth/component/` へ切り出す。

Preview は private Content を `CloudPhotosTheme` で包み、`ScreenState.Success` /
`Error` / `PermissionRequired` のように状態ごとに 1 つずつ用意する。
Screen ではなく Content を対象にすることで Hilt なしで描画できる。
