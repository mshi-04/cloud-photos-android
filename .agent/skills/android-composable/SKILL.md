---
name: android-composable
description: "Use when creating or modifying Compose screens, components, ViewModels, UI state/effect, or screen structure in this project"
---

# Jetpack Compose Guidelines

## Primary references

Read these first before using this skill:
1. `AGENTS.md`
2. `app/AGENTS.md` when changing top-level navigation/start flow
3. `feature/<target>/AGENTS.md`

This skill is a quick Compose pattern guide.
If it conflicts with repository guidance or local feature guidance, prefer those files.

## Screen structure

Prefer the repository pattern:
- Screen = stateful entry point
- Content = stateless/private rendering function when applicable

```text
HogeScreen   ← ViewModel acquisition, state/effect collection, top-level orchestration
└─ HogeContent ← rendering-only, receives state and callbacks as parameters
```

## Rules

1. Keep screen state in ViewModels or dedicated UI state classes.
2. Keep composables declarative; avoid embedding business rules in UI.
3. ViewModels should call use cases, not repository implementations or SDKs.
4. Use `stringResource()` for user-visible strings.
5. Follow existing effect handling patterns such as `LaunchedEffect(Unit)` and `rememberUpdatedState` where applicable.
6. Keep navigation callbacks localized and consistent with existing feature patterns.
7. Keep loading and one-shot effects aligned with established screen structure in the same feature.
8. Use Material 3 and existing shared UI patterns from `core/ui`.

## When to split Screen and Content

Prefer splitting Screen and Content when the screen:
- collects state/effects from a ViewModel
- needs previews for multiple UI states
- contains enough rendering logic to benefit from a stateless rendering function

If the UI is very small, keep it simple, but still preserve clear separation of state ownership.

## Preview rules

- Add previews to rendering-focused composables when useful.
- Wrap previews in `CloudPhotosTheme`.
- Prefer multiple previews for representative states.
- Keep preview callbacks empty (`{}`) unless a more meaningful stub is necessary.

## ViewModel interaction rules

- Acquire ViewModel using `hiltViewModel()` from `androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel`. Do **not** use the deprecated `androidx.hilt.navigation.compose.hiltViewModel`.
- Keep state/effect collection in the screen layer.
- Do not move domain logic into composables.
- Do not add provider/framework translation logic to UI.

## Navigation rules

- `app` owns top-level navigation composition.
- Feature modules own feature-local state/UI behavior.
- If a route or start flow changes, check whether the change belongs in `app` rather than the feature screen itself.

## Output expectations

When using this skill, report:
- touched UI module(s)
- whether screen structure changed
- whether navigation/effect behavior changed
- tests run
