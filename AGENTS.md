# AGENTS.md

This repository uses AI-assisted development under explicit architectural constraints.
Follow these rules when proposing or making changes.

## Supplementary documents

Detailed guidance in `docs/`:

- `docs/ai-playbook.md` — workflow guide: how to approach changes, change strategy, PR workflow
- `docs/architecture-decisions.md` — why the structure is what it is
- `docs/verification-policy.md` — when and how much to verify
- `docs/forbidden-patterns.md` — anti-patterns with reasoning
- `docs/implementation-rules.md` — UseCase, Repository, error handling, value object rules
- `docs/android-conventions.md` — Compose, ViewModel, Hilt, Logging, Navigation, background work
- `docs/testing-conventions.md` — test naming, annotations, verification timing, module targets
- `docs/build-environment.md` — build flavors, environment properties, dependency rules
- `docs/media-upload-flow.md` — upload/delete flow sequence and SyncStatus transitions
- `docs/error-handling-guide.md` — CancellationException and error mapping patterns

Sub-agent policy:

- `docs/ai-playbook.md` § 13 — sub-agent usage rules, delegation scope, parent agent ownership

Feature-local guidance:

- `feature/auth/AGENTS.md` — auth-specific rules and guardrails
- `feature/media/AGENTS.md` — media-specific rules and guardrails (includes former settings rules)

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

If guidance appears to conflict, prefer the higher item in this list and keep the change
conservative.

## Repository shape

- `app` = application wiring, entry points, navigation, top-level DI bootstrap, manifest, build
  flavors
- `core:common` = cross-cutting abstractions and domain-common contracts
- `core:data` = shared data implementations used across features
- `core:ui` = shared theme, strings, common UI resources/components
- `feature:<name>:domain` = business models, repository contracts, use cases, value objects
- `feature:<name>:data` = repository implementations, data sources, mappers, workers, persistence
- `feature:<name>:ui` = ViewModels, UI state/effect, Compose screens/components
- `build-logic` = shared Gradle convention plugins; avoid touching unless the task is about build
  structure

Current features include `auth` and `media`.
Note: the former `settings` feature has been merged into `media`.
`media` has `domain`, `data`, and `ui` submodules.

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
- UI must not call repository implementations, Room DAOs, Amplify clients, or WorkManager
  directly.
- Keep composables declarative and avoid embedding business rules in screens.

## Architectural rules

1. Respect module boundaries.
    - `ui` may depend on its `domain` module and shared UI/common modules.
    - `data` may depend on its own `domain` and shared modules.
    - `domain` must stay framework-light.

2. Preserve dependency direction.
    - Do not introduce reverse dependencies between layers.
    - Do not make one feature depend on an unrelated sibling feature.
    - Prefer extracting shared abstractions to `core:*` only when reuse is real and already
      justified by more than one feature.

3. Do not bypass use cases.
    - ViewModels should normally call use cases.
    - If a ViewModel interacts with a repository-facing abstraction directly, the reason must be
      explicit and local.

4. Keep data models separated by layer.
    - Do not expose DTOs, entities, DAO models, or network response models directly to UI.
    - Use mappers at data boundaries.

5. Prefer additive, local changes.
    - Avoid broad package moves or cross-feature rewrites unless the task explicitly requires them.
    - Touch the fewest files necessary.

6. Reuse existing patterns first.
    - Before introducing a new abstraction, inspect the same feature for an existing equivalent
      pattern.
    - Match naming, folder placement, and test style already present in that feature.

## Git rules for AI agents

- Commit only after all lint checks (`ktlintCheck detekt`) and relevant module tests pass.
- Write commit messages in Japanese, short and concise.
- Push, PR creation, and merge decisions are out of scope for AI agents — leave them to the user.

## Required reporting format for AI-generated changes

When making a code change in this repository, report back with:

- touched modules
- architectural reason for file placement
- summary of what changed and why
- tests run
- tests not run, if any
- known limitations or follow-up items
