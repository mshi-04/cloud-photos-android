# AGENTS.md

This repository uses AI-assisted development under explicit architectural constraints.
Follow these rules when proposing or making changes.

Supplementary documents in `docs/`:
- `docs/ai-playbook.md` — how to work in this repo (workflow guide)
- `docs/architecture-decisions.md` — why the structure is what it is
- `docs/verification-policy.md` — when and how much to verify
- `docs/forbidden-patterns.md` — anti-patterns with reasoning
- `docs/media-upload-flow.md` — upload/delete flow sequence and SyncStatus transitions
- `docs/error-handling-guide.md` — CancellationException and error mapping patterns

## Objective

Make small, correct, testable changes that preserve the repository's modular Android architecture.
Do not optimize for large rewrites.
Prefer the smallest safe change that matches existing feature patterns.

## Source-of-truth order

When multiple guidance files exist, follow them in this order:
1. `AGENTS.md`
2. feature-local conventions and existing code patterns
3. `CLAUDE.md`
4. `.agent/skills/*`

If guidance appears to conflict, prefer the higher item in this list and keep the change conservative.

## Repository shape

- `app` = application wiring, entry points, navigation, top-level DI bootstrap, manifest, build flavors
- `core:common` = cross-cutting abstractions and domain-common contracts
- `core:data` = shared data implementations used across features
- `core:ui` = shared theme, strings, common UI resources/components
- `feature:<name>:domain` = business models, repository contracts, use cases, value objects
- `feature:<name>:data` = repository implementations, data sources, mappers, workers, persistence
- `feature:<name>:ui` = ViewModels, UI state/effect, Compose screens/components
- `build-logic` = shared Gradle convention plugins; avoid touching unless the task is about build structure

Current features include `auth`, `media`, and `settings`.
Note: `settings` has `domain` and `data` submodules only — there is no `:feature:settings:ui` module.

## Layer placement rules

### `app`
Put code here only when it is truly app-wide:
- application startup
- top-level navigation composition
- Hilt bootstrap/wiring modules that assemble feature implementations
- flavor/build configuration concerns

Do not place feature business logic here.
Do not reintroduce code that belongs in `feature/*` modules.

### `feature:<name>:domain`
Allowed here:
- use cases
- repository interfaces
- domain models
- value objects
- pure validation and business rules

Not allowed here:
- Android framework types
- Compose APIs
- Room APIs
- Amplify/Firebase/WorkManager implementations
- concrete data source or repository implementations

### `feature:<name>:data`
Allowed here:
- repository implementations
- DTO/entity mapping
- remote/local data sources
- Room DAOs/entities
- worker/scheduler implementations
- framework integration details

Rules:
- Business rules belong in domain; this layer handles data translation and framework integration.
- Error mapping belongs here via mapper objects.

### `feature:<name>:ui`
Allowed here:
- ViewModels
- UI state/effect models
- Compose screens/components
- input event handling and view-facing formatting

Rules:
- UI/ViewModels call use cases or domain-facing abstractions.
- UI must not call repository implementations, Room DAOs, Amplify clients, or WorkManager directly.
- Keep composables declarative and avoid embedding business rules in screens.

## Architectural rules

1. Respect module boundaries.
   - `ui` may depend on its `domain` module and shared UI/common modules.
   - `data` may depend on its own `domain` and shared modules.
   - `domain` must stay framework-light.

2. Preserve dependency direction.
   - Do not introduce reverse dependencies between layers.
   - Do not make one feature depend on an unrelated sibling feature.
   - Prefer extracting shared abstractions to `core:*` only when reuse is real and already justified by more than one feature.

3. Do not bypass use cases.
   - ViewModels should normally call use cases.
   - If a ViewModel interacts with a repository-facing abstraction directly, the reason must be explicit and local.

4. Keep data models separated by layer.
   - Do not expose DTOs, entities, DAO models, or network response models directly to UI.
   - Use mappers at data boundaries.

5. Prefer additive, local changes.
   - Avoid broad package moves or cross-feature rewrites unless the task explicitly requires them.
   - Touch the fewest files necessary.

6. Reuse existing patterns first.
   - Before introducing a new abstraction, inspect the same feature for an existing equivalent pattern.
   - Match naming, folder placement, and test style already present in that feature.

## Repo-specific implementation rules

### Use cases
- Prefer `suspend operator fun invoke()`.
- Keep each use case single-purpose.
- Use cases should orchestrate domain work, not hold Android/framework concerns.

### Repository implementations
- Repository implementations should delegate to data sources and mappers.
- Do not accumulate business logic in `RepositoryImpl` unless the logic is inherently about data composition/translation.

### Error handling
- Error mapping belongs in the data layer via dedicated mapper objects.
- In coroutine flows or `runCatching` usage, `CancellationException` must be re-thrown.
- Do not swallow cancellation.

### Value objects
- Prefer `@JvmInline value class` with `private constructor` for validated domain concepts.
- Instantiate through `companion object { fun of(raw: ...) }`.
- Validate in `of()` using `require()`.
- Trim string input before validation where appropriate.
- Do not use raw primitives for validated domain concepts when an established value object pattern exists.
- Place value objects in the `valueobject/` package under the corresponding `feature:<name>:domain` module (e.g., `feature/auth/domain/src/main/kotlin/.../auth/valueobject/`).

### Tests
- Use JUnit 5, MockK, and `kotlinx-coroutines-test`.
- Follow Arrange / Act / Assert structure.

#### Test function naming

All test function names must use exactly this format:

```text
`[tested function name] [expected outcome] when [condition]`
```

This format applies to all layers: value objects, use cases, repositories, mappers, workers, and ViewModels.

**Segment definitions:**

| Segment | Rule |
|---|---|
| `tested function name` | The exact Kotlin function, property, or event-handler name under test. Must appear first. camelCase is allowed only in this segment (e.g., `onSignIn`, `fetchMedia`). |
| `expected outcome` | A verb phrase using one of the allowed verbs only (see below). |
| `when [condition]` | The scenario or input state. Must always be present — never omit. |

**Allowed verbs:** `returns` / `throws` / `sets` / `emits` / `calls` / `rethrows` / `ignores`

**Naming restrictions:**

- `test`, `should`, `verify`, or similar prefixes are forbidden.
- snake_case is forbidden anywhere in the name.
- camelCase is forbidden outside the tested function name segment.
- Japanese characters are forbidden.
- Vague outcome words (`works`, `handles`, `correctly`, `properly`) are forbidden.
- Categorical labels (`success case`, `failure case`, `happy path`, `error case`) are forbidden.
- `success` or `failure` alone as the outcome is forbidden — write `returns Success` / `returns Error` instead.
- Multiple behaviors in one function name are forbidden.

**Examples:**

```kotlin
fun `invoke returns Success when repository succeeds`()
fun `of throws IllegalArgumentException when email is blank after trim`()
fun `onSignIn emits NavigateToHome when credentials are valid`()
fun `onSignIn sets passwordError when credentials are invalid`()
fun `invoke returns Error when network is unavailable`()
fun `of returns Email when input is valid`()
```

#### Annotations

Allowed: `@Test`, `@BeforeEach`, `@AfterEach`, `@OptIn(ExperimentalCoroutinesApi::class)`, `@ParameterizedTest` (with `@ValueSource` / `@CsvSource` / `@MethodSource`), `@ExtendWith` (only when a JUnit extension from an external library or a custom extension is required — see note below).

Forbidden: `@DisplayName` (backtick name is sufficient), `@Disabled` (fix or delete — do not commit disabled tests), `@Nested`, `@Tag`, `@Timeout`, `@RepeatedTest`.

Note on `@ExtendWith` and MockK: In standard `*Test.kt` files that use MockK, call `mockk<>()` directly — no `@ExtendWith(MockKExtension::class)` is needed or recommended. Reserve `@ExtendWith` for cases where a JUnit extension is genuinely required (e.g., a custom test lifecycle extension or a third-party library extension that has no MockK equivalent).

## Android-specific rules

### Compose
- Follow the existing pattern: screen = stateful entry, content = stateless/private rendering function when applicable.
- Keep screen state in ViewModels or dedicated UI state classes.
- Avoid scattered side effects inside composables.
- Use `LaunchedEffect(Unit)` and `rememberUpdatedState` consistently with existing patterns when handling one-shot effects.
- Use `stringResource()` for UI strings.
- When adding previews, wrap them in `CloudPhotosTheme` and mirror existing preview conventions.

### ViewModels
- ViewModels own screen state and trigger use cases.
- Keep them focused on state transitions, validation wiring, and UI-facing effects.
- Do not move domain logic from use cases into ViewModels.

### Hilt / DI
- Keep Hilt wiring in appropriate DI/bootstrap locations.
- Prefer feature-local implementation binding patterns that already exist.
- Avoid placing unrelated bindings into one large catch-all module.

### Logging
- Do not use `android.util.Log` directly in production code.
- Temporary debug log statements must not be committed.
- If a logging abstraction does not exist in this repository, prefer omitting the log over using `android.util.Log` directly.

### Navigation
- Keep navigation changes centralized and minimal.
- Prefer editing existing nav definitions instead of spreading route knowledge across many files.

### Background work
- `media` contains upload/delete flows using workers/schedulers.
- Prefer the existing worker/scheduler patterns for background execution.
- Be conservative when touching upload queue, sync status, record mapping, and worker retry behavior.

## Feature-specific guardrails

### Auth
- Keep auth errors and auth step translation in feature auth data-layer mappers.
- Preserve domain value object usage for `Email`, `Password`, `UserId`, and similar validated concepts.
- Avoid pushing Cognito-specific behavior into domain or UI types.

### Media
- Be extra careful with upload/delete scheduling, sync status, local/remote record consistency, and worker interactions.
- Preserve the split between local media access, upload record storage, remote upload record sync, and scheduler responsibilities.
- Do not collapse worker/data-source/repository responsibilities into one class.

### Settings
- Keep settings logic lightweight and isolated.
- Prefer existing repository/use-case/value-object patterns rather than direct preference handling from UI.

## Build and environment rules

This project uses build flavors and required environment properties.
Be careful not to break flavored builds.

Required flavor properties include values such as:
- `COGNITO_CLIENT_ID`
- `API_BASE_URL`
- `S3_BUCKET_NAME`

Rules:
- Do not hardcode secrets, endpoints, client IDs, or bucket names in Kotlin source.
- Do not commit environment-specific values outside the intended configuration mechanism.
- Be careful when touching `app/build.gradle.kts`, flavor logic, manifest configuration, or CI dummy-secret behavior.
- Avoid changing `build-logic` unless the task is explicitly about Gradle conventions.

## Dependency rules

- Prefer existing libraries and patterns already used in the repository.
- Do not add a new library unless clearly necessary.
- If adding a dependency is unavoidable, explain why and keep the scope minimal.
- Prefer module-local dependencies over broad app-level additions when possible.

## Testing and completion criteria

A change is not complete unless all relevant checks pass.

### Verification timing

Follow this three-stage approach based on change scope:

**During development** — run the smallest scope covering what you changed:
- Single module: `./gradlew :feature:<name>:<layer>:test`
- Quick lint: `./gradlew ktlintCheck`

**Before handing the change back to the user** — run lint and tests for all changed modules:
```bash
./gradlew ktlintFormat  # local auto-fix (not executed by CI)
./gradlew ktlintCheck detekt
# single module change:
./gradlew :feature:<name>:<layer>:test
# shared (core/*), app wiring, navigation, or Gradle changes:
./gradlew test
```

**CI gate (user's merge workflow)** — CI is the final gate before merging. The user must not merge if CI is red.
CI runs `bundle exec fastlane lint` and `bundle exec fastlane test`.

If tests are not run, explicitly state that they were not run.

### Module test targets

```bash
./gradlew :feature:auth:domain:test
./gradlew :feature:auth:data:test
./gradlew :feature:auth:ui:test
./gradlew :feature:media:domain:test
./gradlew :feature:media:data:test
./gradlew :feature:media:ui:test
./gradlew :feature:settings:domain:test
./gradlew :feature:settings:data:test
```

## Git operations ownership

Git operations are user-owned unless explicitly requested. Do not commit, push, create a branch, or open a pull request unless the user explicitly asks for it. Focus on code changes, verification, and reporting.

## Pull request and branch workflow

The following applies when the user explicitly requests Git operations:

- Branch from `develop` for feature work unless explicitly told otherwise.
- Do not push directly to protected branches.
- Keep PR scope tight and consistent with the branch purpose.
- Follow the repository PR template expectations, including test notes and Android-specific checks when applicable.

## Change strategy for AI agents

When asked to implement something:
1. Identify the smallest affected module.
2. Confirm the correct layer for the logic.
3. Reuse existing patterns in the same feature first.
4. Make the smallest safe change.
5. Run targeted verification.
6. Summarize exactly what changed, what was not changed, and remaining risks.

## Required reporting format for AI-generated changes

When making a code change in this repository, report back with:
- touched modules
- architectural reason for file placement
- summary of what changed and why
- tests run
- tests not run, if any
- known limitations or follow-up items
