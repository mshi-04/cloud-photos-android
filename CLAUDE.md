# CloudPhotos (`com.appvoyager.cloudphotos`)

## Primary instruction source

`AGENTS.md` is the source of truth for repository-wide development rules.
When working in this repository, read and follow `AGENTS.md` first.
If this file and `AGENTS.md` overlap, prefer `AGENTS.md`.

## Purpose of this file

This file is intentionally lightweight.
It supplements `AGENTS.md` with Claude-oriented entry guidance and quick repo context.
Do not duplicate detailed architectural policy here unless there is a Claude-specific reason.

## Stack
- Kotlin
- Jetpack Compose (Material 3)
- Clean Architecture
- Hilt + KSP
- AWS Amplify (Cognito)
- Firebase (Analytics, FCM)
- Min SDK 29 / Compile SDK 36 / Java 17
- Flavors: `dev`, `prod`

## Skills
- `.agent/skills/android-clean-arch/SKILL.md`
- `.agent/skills/android-composable/SKILL.md`
- `.agent/skills/android-auth-error/SKILL.md`
- `.agent/skills/android-testing/SKILL.md`
- `.agent/skills/android-media-upload/SKILL.md`

## Subagents
- `.claude/agents/reviewer.md` — read-only code reviewer (architecture, security, conventions)
- `.claude/agents/test-writer.md` — unit test generator (JUnit 5 + MockK patterns)
- `.claude/agents/arch-checker.md` — architecture validator (module boundaries, dependency direction)

## Claude quick-start

Before making changes:
1. Read `AGENTS.md`.
2. Identify the smallest affected module.
3. Reuse patterns already present in the same feature.
4. Keep changes local and avoid silent refactors.
5. Run the smallest relevant test scope, then report what changed.

## Repo-specific reminders

- UseCase style: `suspend operator fun invoke()` with a single responsibility.
- Repository implementations should delegate to data sources and avoid business logic.
- Error mapping belongs in the data layer via mapper objects.
- `CancellationException` must be re-thrown in `runCatching` flows.
- Domain models should prefer value objects over raw primitives for validated concepts.
- Compose screens should stay declarative, with state owned by ViewModels/UI state classes.
- UI strings should come from `stringResource()`.
- Tests use JUnit 5 + MockK + `kotlinx-coroutines-test`.

## Testing entrypoint

For local verification, use the smallest relevant Gradle command (e.g.,
`./gradlew :feature:<name>:<layer>:test`).
For CI-aligned verification (DEV/Debug configuration unit tests), use `bundle exec fastlane test`. Note that this command is scoped to CI-aligned validation and does not necessarily cover all modules. In contrast, `./gradlew test` is responsible for building and testing each module (providing broader module-level coverage). Use `bundle exec fastlane test` for CI-synchronized checks and `./gradlew test` for comprehensive module-wide testing.
See `docs/verification-policy.md` for the overall verification policy including CI gates.
