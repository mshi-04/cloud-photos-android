# feature/settings/AGENTS.md

Local guidance for the `settings` feature.
Read root `AGENTS.md` first, then apply the rules below.
If this file conflicts with root `AGENTS.md`, prefer the root file and keep the change conservative.

## Scope

This feature is split into:

- `feature/settings/domain`
- `feature/settings/data`

## Intent of this feature

This feature owns lightweight user settings and their domain rules.
It should stay small, isolated, and easy to reason about.

## Domain rules

Keep `feature/settings/domain` focused on:

- settings repository contracts
- settings use cases
- settings value objects
- settings-specific validation rules

Rules:

- Prefer small, explicit use cases.
- Keep validated settings concepts in value objects when that pattern already exists.
- Do not place Android or persistence concerns in domain.

## Data rules

`feature/settings/data` is responsible for:

- reading/writing persisted settings values
- implementing repository contracts
- translating persistence models into domain-facing values

Rules:

- Keep persistence details in data.
- Do not let UI or unrelated features access storage details directly.
- Prefer small repository implementations and focused data source code.

## Guardrails

- Do not over-engineer this feature.
- Prefer the simplest implementation that matches existing repository/use-case/value-object
  patterns.
- If a setting starts being shared by multiple features, be explicit about whether it still belongs
  here or should move to a shared/core location.

## Testing guidance

Prefer targeted settings tests first:

```bash
./gradlew :feature:settings:domain:test
```

Run broader tests if changes affect app wiring or shared modules.

## Report back with

- which settings layer changed
- whether any setting semantics or validation changed
- tests run
- any follow-up if the setting may outgrow this feature
