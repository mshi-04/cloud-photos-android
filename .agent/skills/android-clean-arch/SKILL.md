---
name: android-clean-arch
description: "Use when creating or modifying UseCase, Repository interface, RepositoryImpl, DataSource, mapper, or module boundaries in this Android Clean Architecture project"
---

# Clean Architecture Guidelines

## Primary references

Read these first before using this skill:
1. `AGENTS.md`
2. `core/AGENTS.md` or `app/AGENTS.md` when relevant
3. `feature/<target>/AGENTS.md`

This skill is a quick pattern guide.
If it conflicts with repository guidance, prefer the repository guidance.

## Current repository structure

```text
app/                    # app-wide startup, navigation, DI bootstrap, flavors
core/common/            # shared abstractions
core/data/              # shared data implementations
core/ui/                # shared theme/resources/ui
feature/<name>/domain/  # use cases, repository interfaces, models, value objects
feature/<name>/data/    # repository impls, data sources, mappers, workers, persistence
feature/<name>/ui/      # ViewModels, UI state/effect, Compose screens
```

## Placement rules

### Domain
Place here:
- use cases
- repository interfaces
- domain models
- request models
- value objects
- pure validation/business rules

Do not place here:
- Android framework types
- Compose APIs
- Room/DAO/entity code
- Amplify/Firebase/WorkManager implementation details
- concrete repository/data source implementations

### Data
Place here:
- repository implementations
- local/remote data sources
- mapper objects
- persistence code
- provider/framework integration code

Rules:
- RepositoryImpl should delegate to data sources and mappers.
- Keep business rules out of data unless they are purely translation/integration concerns.
- Error mapping belongs in data-layer mappers.
- Re-throw `CancellationException` in coroutine error handling.

### UI
Place here:
- ViewModels
- UI state/effect models
- Compose screens/components
- screen-level input handling

Rules:
- UI talks to use cases or domain-facing abstractions.
- UI must not talk directly to repository implementations, DAOs, or provider SDKs.

## UseCase rules

- Prefer `suspend operator fun invoke()`.
- Keep each use case single-purpose.
- Use cases orchestrate domain work and do not own Android/framework behavior.

## Repository rules

- Repository interfaces live in `feature/<name>/domain`.
- Repository implementations live in `feature/<name>/data`.
- RepositoryImpl should not become a second use case layer.

## Value object rules

- Prefer `@JvmInline value class` with `private constructor` for validated domain concepts.
- Create instances via `of(...)` in a companion object.
- Use `require()` for validation.
- Do not replace established value objects with raw primitives in domain APIs.

## Decision checklist

Before adding code, ask:
1. Which feature owns this concept?
2. Is this domain, data, or UI responsibility?
3. Is there already an equivalent pattern in the same feature?
4. Does this belong in `core` or only in one feature?
5. Am I leaking data/framework details upward?

## Output expectations

When using this skill, report:
- touched module(s)
- why the file belongs in that layer
- whether a new abstraction was introduced
- tests run
