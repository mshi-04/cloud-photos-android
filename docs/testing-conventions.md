# Testing Conventions

Referenced from `AGENTS.md` (source of truth for all rules).
Source-of-truth order: `AGENTS.md` → feature-local patterns → `CLAUDE.md` → `.agent/skills/*`.
When this file and `AGENTS.md` conflict, prefer `AGENTS.md`.
See also: `.agent/skills/android-testing/SKILL.md` for testing pattern guidance.
Where this file and the Skill conflict on annotation rules or naming details, prefer this file.

---

## Test stack

- JUnit 5
- MockK
- `kotlinx-coroutines-test`
- Follow Arrange / Act / Assert structure.

## Test function naming

All test function names must use exactly this format:

```text
`[tested function name] [expected outcome] when [condition]`
```

This format applies to all layers: value objects, use cases, repositories, mappers, workers, and
ViewModels.

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
- `success` or `failure` alone as the outcome is forbidden — write `returns Success` /
  `returns Error` instead.
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

## Annotations

Allowed: `@Test`, `@BeforeEach`, `@AfterEach`, `@OptIn(ExperimentalCoroutinesApi::class)`,
`@ParameterizedTest` (with `@ValueSource` / `@CsvSource` / `@MethodSource`), `@ExtendWith`
(only when a JUnit extension from an external library or a custom extension is required — see
note below).

Forbidden: `@DisplayName` (backtick name is sufficient), `@Disabled` (fix or delete — do not
commit disabled tests), `@Nested`, `@Tag`, `@Timeout`, `@RepeatedTest`.

Note on `@ExtendWith` and MockK: In standard `*Test.kt` files that use MockK, call `mockk<>()`
directly — no `@ExtendWith(MockKExtension::class)` is needed or recommended. Reserve
`@ExtendWith` for cases where a JUnit extension is genuinely required (e.g., a custom test
lifecycle extension or a third-party library extension that has no MockK equivalent).

## Verification timing

| Timing | Executor | Content |
|---|---|---|
| On task completion | AI agent | `./gradlew ktlintCheck detekt` → affected module tests |
| Push / PR creation | Human | push, PR creation, merge decision |
| PR / merge gate | CI | `bundle exec fastlane lint` + `bundle exec fastlane test` |

See `docs/verification-policy.md` for the full policy including Gradle sync rules and test scope
by change type.

If tests are not run, explicitly state that they were not run.

## Module test targets

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
