# Implementation Rules

Referenced from `AGENTS.md` (source of truth for all rules).
Source-of-truth order: `AGENTS.md` → feature-local patterns → `CLAUDE.md` → `.agent/skills/*`.
When this file and `AGENTS.md` conflict, prefer `AGENTS.md`.

---

## Use cases

- Prefer `suspend operator fun invoke()`.
- Keep each use case single-purpose.
- Use cases should orchestrate domain work, not hold Android/framework concerns.

## Repository implementations

- Repository implementations should delegate to data sources and mappers.
- Do not accumulate business logic in `RepositoryImpl` unless the logic is inherently about data
  composition/translation.

## Error handling

- Error mapping belongs in the data layer via dedicated mapper objects.
- In coroutine flows or `runCatching` usage, `CancellationException` must be re-thrown.
- Do not swallow cancellation.
- See `docs/error-handling-guide.md` for patterns and examples.

## Value objects

- Prefer `@JvmInline value class` with `private constructor` for validated domain concepts.
- Instantiate through `companion object { fun of(raw: ...) }`.
- Validate in `of()` using `require()`.
- Trim string input before validation where appropriate.
- Do not use raw primitives for validated domain concepts when an established value object pattern
  exists.
- Place value objects in the `valueobject/` package under the corresponding
  `feature:<name>:domain` module
  (e.g., `feature/auth/domain/src/main/kotlin/.../auth/valueobject/`).

## Feature-specific rules

Feature-local guardrails live in the respective `feature/<name>/AGENTS.md` files.
Read the relevant file before touching that feature:

- `feature/auth/AGENTS.md` — Cognito translation, auth value objects, auth step branching
- `feature/media/AGENTS.md` — upload/delete scheduling, sync status, worker/scheduler split
- `feature/settings/AGENTS.md` — lightweight settings, isolation, value object patterns

These files take precedence for feature-local decisions, but defer to root `AGENTS.md` on
conflicts.
