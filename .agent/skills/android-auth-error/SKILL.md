---
name: android-auth-error
description: "Use when adding or modifying auth error types, auth error mappers, auth-step translation, or provider-specific auth exception handling"
---

# Auth Error Handling Guidelines

## Primary references

Read these first before using this skill:

1. `AGENTS.md`
2. `feature/auth/AGENTS.md`

This skill is a quick auth-error pattern guide.
If it conflicts with repository guidance, prefer the repository guidance.

## Ownership

Auth error handling belongs to the `auth` feature.
Provider-specific auth behavior must stay inside the auth data layer.

## Structure

Typical ownership in this repository:

- `feature/auth/domain/...` = domain-facing auth error/result models
- `feature/auth/data/...` = Cognito/Amplify exception mapping and provider translation
- `feature/auth/ui/...` = UI-facing rendering/effect decisions based on domain auth errors

## Rules

1. Domain auth error models must not depend on AWS/Amplify SDK types.
2. Provider-specific exceptions must be translated in auth data-layer mapper objects.
3. Do not leak Cognito/Amplify exception types to domain or UI.
4. Keep account-enumeration-safe mappings intact.
5. `Unknown` is an escape hatch, not a place to leak provider internals upward.
6. Re-throw `CancellationException` in coroutine error handling.

## Mapping guidance

- Add new provider exception mappings in the auth data layer.
- Prefer existing mapper objects instead of scattering translation logic across data sources.
- Keep auth-step and auth-error translation readable and localized.
- If a mapping changes user-visible behavior, call that out explicitly.

## Security guidance

Be careful with mappings that reveal whether a user/account exists.
Preserve existing protections that intentionally collapse provider errors into safer domain-level
errors.

## Output expectations

When using this skill, report:

- which auth mapper/model changed
- whether user-visible auth error behavior changed
- whether security-sensitive mapping behavior changed
- tests run
