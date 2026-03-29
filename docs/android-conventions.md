# Android Conventions

Referenced from `AGENTS.md` (source of truth for all rules).
Source-of-truth order: `AGENTS.md` → feature-local patterns → `CLAUDE.md` → `.agent/skills/*`.
When this file and `AGENTS.md` conflict, prefer `AGENTS.md`.

---

## Compose

- Follow the existing pattern: screen = stateful entry, content = stateless/private rendering
  function when applicable.
- Keep screen state in ViewModels or dedicated UI state classes.
- Avoid scattered side effects inside composables.
- Use `LaunchedEffect(Unit)` and `rememberUpdatedState` consistently with existing patterns when
  handling one-shot effects.
- Use `stringResource()` for UI strings.
- When adding previews, wrap them in `CloudPhotosTheme` and mirror existing preview conventions.

## ViewModels

- ViewModels own screen state and trigger use cases.
- Keep them focused on state transitions, validation wiring, and UI-facing effects.
- Do not move domain logic from use cases into ViewModels.

## Hilt / DI

- Keep Hilt wiring in appropriate DI/bootstrap locations.
- Prefer feature-local implementation binding patterns that already exist.
- Avoid placing unrelated bindings into one large catch-all module.

## Logging

- Do not use `android.util.Log` directly in production code.
- Temporary debug log statements must not be committed.
- If a logging abstraction does not exist in this repository, prefer omitting the log over using
  `android.util.Log` directly.

## Navigation

- Keep navigation changes centralized and minimal.
- Prefer editing existing nav definitions instead of spreading route knowledge across many files.

## Background work

- `media` contains upload/delete flows using workers/schedulers.
- Prefer the existing worker/scheduler patterns for background execution.
- Be conservative when touching upload queue, sync status, record mapping, and worker retry
  behavior.
