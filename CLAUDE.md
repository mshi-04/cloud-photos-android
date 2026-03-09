# CloudPhotos (`com.appvoyager.cloudphotos`)

## Stack
Kotlin / Jetpack Compose (Material 3) / Clean Architecture / Hilt+KSP
AWS Amplify (Cognito) / Firebase (Analytics, FCM)
Min SDK: 29 / Compile SDK: 36 / Java: 17 / Flavors: dev, prod

## Skills
- `.agent/skills/android-clean-arch/SKILL.md`
- `.agent/skills/android-composable/SKILL.md`
- `.agent/skills/android-auth-error/SKILL.md`
- `.agent/skills/android-testing/SKILL.md`

## Key Conventions
- UseCase: `suspend operator fun invoke()`, single responsibility
- RepositoryImpl: delegates to DataSource only, no business logic
- Domain layer: pure Kotlin, no Android dependencies
- Error mapping: done in Data layer via internal mapper objects
- CancellationException must be re-thrown in runCatching blocks

## UI Conventions
- Screen = stateful / Content = stateless private Composable
- Effects: `LaunchedEffect(Unit)` + `rememberUpdatedState`
- Loading: `BackHandler(enabled = isLoading) {}` + `LoadingOverlay()`
- Strings: `stringResource()` only
- Preview: normal / loading / error, wrapped in `CloudPhotosTheme`

## Testing
- JUnit 5 + MockK + kotlinx-coroutines-test
- MockK: `mockk<>()` directly, no `@ExtendWith`
- Dispatcher: `StandardTestDispatcher` + `setMain`/`resetMain`
- Test names: backtick English (`initial state has empty email`)
- Structure: AAA, 1 assertion per test