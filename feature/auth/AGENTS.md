# feature/auth/AGENTS.md

Local guidance for the `auth` feature.
Read root `AGENTS.md` first, then apply the rules below.
If this file conflicts with root `AGENTS.md`, prefer the root file and keep the change conservative.

## Scope

This feature is split into:
- `feature/auth/domain`
- `feature/auth/data`
- `feature/auth/ui`

## Intent of this feature

This feature owns authentication-related domain concepts and auth user flows.
It should contain auth-specific models, value objects, use cases, data translation, and UI flows.

## Domain rules

Keep `feature/auth/domain` pure Kotlin.
Allowed here:
- auth use cases
- auth repository interfaces
- auth domain models
- auth value objects such as validated user-facing credentials and identifiers

Do not place here:
- Cognito SDK specifics
- Android framework types
- Compose/UI types
- data source implementations

## Value object rules

Preserve value object usage for validated auth concepts.
Prefer existing patterns for:
- `Email`
- `Password`
- `UserId`
- confirmation/auth tokens or codes when they represent validated domain inputs

Do not replace established value objects with raw `String` values in domain APIs.
If a new validated auth concept is introduced, prefer a value object over a primitive.

## Data rules

`feature/auth/data` is responsible for:
- integrating with Cognito/Amplify auth behavior
- translating SDK errors and states into domain-friendly models
- implementing repository contracts
- mapping provider-specific auth steps/errors into domain results

Rules:
- Keep Cognito-specific translation in data mappers and data source implementations.
- Do not leak provider-specific error/state models to domain or UI.
- Repository implementations should delegate to data sources and mappers.
- Error mapping belongs in data-layer mapper objects.
- Re-throw `CancellationException` in coroutine error handling.

## UI rules

`feature/auth/ui` is responsible for:
- auth screens
- auth ViewModels
- UI state/effect classes
- user input handling and screen-level validation wiring

Rules:
- ViewModels should call auth use cases.
- Keep screen state in UI state classes / ViewModels.
- Keep one-shot navigation/snackbar/etc. in effect models following existing patterns.
- Do not move Cognito-specific translation into UI.
- Do not move business rules from use cases into composables.

## Flow-specific guardrails

When editing login, signup, forgot-password, reset-password, or verification flows:
- preserve the separation between input validation, domain execution, and UI effects
- keep auth-step branching readable and localized
- prefer updating existing effect/state models instead of inventing parallel ones
- be careful not to break existing screen-to-screen transition assumptions

## Testing guidance

Prefer targeted auth tests first:

```bash
./gradlew :feature:auth:domain:test
./gradlew :feature:auth:data:test
./gradlew :feature:auth:ui:test
```

Run broader tests if changes cross app wiring or shared modules.

## Report back with

- which auth layer changed
- whether provider-specific behavior changed
- whether any value object or validation rule changed
- tests run
- remaining risks in auth flow transitions
