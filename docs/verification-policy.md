# Verification Policy

This document defines when and how much verification is required.
The goal is to avoid both under-verification (missing real problems) and over-verification
(Gradle sync and full test suite on every single-line change).

## Principle

Run the smallest scope that can actually catch the class of error introduced by your change.
Escalate scope only when the change crosses module or layer boundaries.

## Gradle sync

Gradle sync is **not required** after every source-only change.
Run Gradle sync only when you have changed:
- `build.gradle.kts` files
- `libs.versions.toml`
- `settings.gradle.kts`
- `build-logic` convention plugins
- Added or removed a module

For Kotlin source changes inside an existing module, sync is unnecessary.

## During development

Run the smallest scope covering what you changed:

```bash
# single module (preferred during active development)
./gradlew :feature:<name>:<layer>:test

# quick lint check
./gradlew ktlintCheck
```

Do not run `bundle exec fastlane test` on every iteration — it is the CI-level gate, not a
local development loop.

## Before opening a PR

Run lint and tests for all changed modules:

```bash
# auto-fix formatting first
./gradlew ktlintFormat

# lint gates
./gradlew ktlintCheck detekt

# tests — scope depends on what changed (see below)
```

### Test scope by change type

| Change type | Test command |
|---|---|
| Single feature layer (`feature:<name>:<layer>`) | `./gradlew :feature:<name>:<layer>:test` |
| Multiple layers in one feature | `./gradlew :feature:<name>:domain:test :feature:<name>:data:test :feature:<name>:ui:test` |
| `core:*` module | `./gradlew test` (all modules) |
| `app` wiring, navigation, top-level DI | `./gradlew test` |
| Gradle / `build-logic` / dependency changes | `./gradlew test` |
| Documentation only | No test run required |

When in doubt about scope, run `./gradlew test`.

## Before merging

CI is the final gate. Do not merge if CI is red.

CI runs:
```bash
bundle exec fastlane lint   # ktlintCheck + detekt
bundle exec fastlane test   # all unit tests
```

A passing local run does not substitute for a passing CI run.
If CI fails after your PR is opened, investigate and fix before merging.

## Fastlane lanes

| Lane | What it runs |
|---|---|
| `bundle exec fastlane lint` | ktlintCheck + detekt across all modules |
| `bundle exec fastlane test` | all Gradle unit tests |

## Module test targets reference

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

## Reporting

If you did not run tests, state it explicitly in the report:
```
tests not run: <command> — reason: <why>
```

Omitting this is not acceptable. CI being the gate does not justify skipping the report.
