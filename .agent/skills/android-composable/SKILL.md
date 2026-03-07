---
name: android-composable
description: "Use when creating or modifying Composable screens, components, or UI elements in an Android Jetpack Compose project"
---

# Jetpack Compose Guidelines

## Screen Structure
Screen（hiltViewModel付き）とContent（stateless）を必ず分離する
```text
HogeScreen        ← ViewModelを持つ、effectを収集する
└── HogeContent   ← stateless、全状態をパラメータで受け取る
```

## Rules
1. Screen Composable: `hiltViewModel()` でVM取得、`collectAsStateWithLifecycle()` でstate収集
2. Content Composable: `private` にする。状態は全てパラメータで受け取る
3. Effect収集: `LaunchedEffect(Unit)` + `rememberUpdatedState` でコールバックをラップする
4. ナビゲーションコールバック: `rememberUpdatedState` でラップする
5. ローディング中のバックハンドラ: `BackHandler(enabled = uiState.isLoading) {}` で無効化
6. Loading overlay: `Box` で `HogeContent` の上に重ねる
7. MaterialTheme: Material 3 (`androidx.compose.material3`) のみ使う
8. 文字列: `stringResource()` を使う。ハードコード禁止

## Preview Rules
1. Contentに対して複数Previewを作る（正常系・エラー系・ローディング系）
2. Previewは `private` にする
3. 必ず `CloudPhotosTheme` でラップする
4. 全コールバックは `{}` で渡す

## Screen Template
```kotlin
@Composable
fun HogeScreen(
    viewModel: HogeViewModel = hiltViewModel(),
    onNavigateToNext: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val latestResources = rememberUpdatedState(LocalResources.current)
    val latestOnNavigateToNext = rememberUpdatedState(onNavigateToNext)

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is HogeEffect.NavigateToNext -> latestOnNavigateToNext.value()
                is HogeEffect.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(latestResources.value.getString(effect.messageResId))
                }
            }
        }
    }

    BackHandler(enabled = uiState.isLoading) {}

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            HogeContent(
                uiState = uiState,
                onIntent = viewModel::onIntent
            )
            if (uiState.isLoading) LoadingOverlay()
        }
    }
}
```

## Content Template
```kotlin
@Composable
private fun HogeContent(
    // 全状態をパラメータで受け取る
    isLoading: Boolean,
    onHogeAction: () -> Unit
) {
    // UI実装
}
```

## Preview Template
```kotlin
@Preview(showBackground = true)
@Composable
private fun HogeContentPreview() {
    CloudPhotosTheme {
        HogeContent(
            isLoading = false,
            onHogeAction = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HogeContentLoadingPreview() {
    CloudPhotosTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            HogeContent(isLoading = true, onHogeAction = {})
            LoadingOverlay()
        }
    }
}
```