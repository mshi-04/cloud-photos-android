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
5. Preserve readable test names and existing style in the target module.
6. Follow Arrange / Act / Assert structure.
7. If changing worker/scheduler behavior, prefer behavior-focused tests over shallow coverage.

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
