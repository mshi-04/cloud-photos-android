---
name: android-testing
description: "Use when creating or modifying unit tests, ViewModel tests, use case tests, mapper tests, or test utilities in this Android project"
---

# Android Testing Guidelines

## Primary references

Read these first before using this skill:
1. `AGENTS.md`
2. local module guidance such as `feature/<target>/AGENTS.md`, `core/AGENTS.md`, or `app/AGENTS.md`

This skill is a quick testing pattern guide.
If it conflicts with repository guidance, prefer the repository guidance.

## Current test stack

- JUnit 5
- MockK
- `kotlinx-coroutines-test`
- Gradle module tests
- CI-aligned entrypoint via `bundle exec fastlane test`

## Rules

1. Prefer targeted tests for the changed module first.
2. Prioritize testing use cases, ViewModels, mappers, repositories, and worker behavior where risk is highest.
3. Use `mockk<>()` directly; do not introduce unnecessary test framework ceremony.
4. For coroutine tests, use repository-consistent dispatcher setup patterns.
5. Follow Arrange / Act / Assert structure.
6. If changing worker/scheduler behavior, prefer behavior-focused tests over shallow coverage.
7. All test function names must follow the format in `## Test function naming` below.

## Test function naming

All test function names must use exactly this format:

```text
`[tested function name] [expected outcome] when [condition]`
```

| Element | Rule |
|---------|------|
| `tested function name` | The exact function, property, or event handler under test. Always placed first. |
| `expected outcome` | One observable verb phrase. Allowed verbs: `returns`, `throws`, `sets`, `emits`, `calls`, `rethrows`, `ignores`. |
| `when [condition]` | The scenario or input state. Never omit. |

This format applies to every layer — value objects, use cases, repositories, mappers, workers, and ViewModels.

### Examples

```kotlin
fun `of returns Email when input is valid`()
fun `of throws when email is blank after trim`()
fun `invoke returns Success when repository succeeds`()
fun `invoke returns Error when network is unavailable`()
fun `onSignIn emits NavigateToHome when credentials are valid`()
fun `onSignIn sets passwordError when credentials are invalid`()
```

### Forbidden naming patterns

- Starting with `test`, `should`, `verify`, or similar prefixes
- Using `success case`, `failure case`, `happy path`, `error case`, or other categorical labels
- Omitting `when [condition]`
- Using `success` or `failure` as the outcome — write `returns Success` / `returns Error` instead
- Describing multiple behaviors in one function name
- Using `works`, `handles`, `correctly`, `properly`, or other vague outcome words
- Using snake_case or camelCase inside backticks
- Using Japanese characters
- Using any naming style other than the required format

### Annotations

Allowed: `@Test`, `@BeforeEach`, `@AfterEach`, `@OptIn(ExperimentalCoroutinesApi::class)`, `@ParameterizedTest` (with `@ValueSource` / `@CsvSource` / `@MethodSource`), `@ExtendWith`.

Forbidden: `@DisplayName` (backtick name is sufficient), `@Disabled` (fix or delete — do not commit disabled tests), `@Nested`, `@Tag`, `@Timeout`, `@RepeatedTest`.

## ViewModel test guidance

- Own state transitions in the ViewModel.
- Mock use cases, not repository implementations, unless the tested class is itself part of data.
- Validate state/effect behavior in terms that match the target feature's UI contract.

## UseCase test guidance

- Mock repository interfaces.
- Keep tests focused on one domain behavior at a time.
- Prefer testing domain behavior, validation, and orchestration rather than implementation trivia.

## Mapper / data test guidance

- Test provider/data translation at the mapper boundary.
- When changing auth or media translation, verify domain-facing output rather than SDK-internal behavior.
- For persistence-related code, test the semantics that matter to the feature, not just field copying.

## Verification guidance

Use the smallest relevant scope first, for example:

```bash
./gradlew :feature:auth:domain:test
./gradlew :feature:auth:data:test
./gradlew :feature:auth:ui:test
./gradlew :feature:media:domain:test
./gradlew :feature:media:data:test
./gradlew :feature:media:ui:test
./gradlew :feature:settings:domain:test
```

For broader impact changes, prefer:

```bash
./gradlew test
bundle exec fastlane test
```

## Output expectations

When using this skill, report:
- which test scope was run
- which layers were covered
- tests not run, if any
- remaining gaps or risky untested paths
