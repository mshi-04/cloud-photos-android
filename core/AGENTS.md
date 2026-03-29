# core/AGENTS.md

Local guidance for the `core` modules.
Read root `AGENTS.md` first, then apply the rules below.
If this file conflicts with root `AGENTS.md`, prefer the root file and keep the change conservative.

## Scope

This folder currently includes:

- `core/common`
- `core/data`
- `core/ui`

## Intent of core modules

`core/*` exists for truly shared concerns.
Code belongs here only when it is meaningfully shared across features or clearly app-wide.
Do not move code into `core` just because it feels generic.

## Placement rules

### `core/common`

Use for:

- cross-cutting abstractions
- common contracts used by multiple features
- domain-common concepts that are not owned by a single feature

Rules:

- Keep this framework-light whenever possible.
- Do not dump unrelated helpers here.
- If a concept is owned by a single feature, keep it in that feature.

### `core/data`

Use for:

- shared data-side implementations that are genuinely cross-feature
- infrastructure code needed by more than one feature

Rules:

- Keep shared data code focused and reusable.
- Do not move feature-specific repository logic here.
- If the code depends on one feature's business meaning, it probably does not belong in `core/data`.

### `core/ui`

Use for:

- shared theme
- shared strings/resources
- common UI elements used across multiple features

Rules:

- Keep feature-specific screens and effects out of `core/ui`.
- Do not move a component here unless reuse is real.
- Preserve repository-wide UI conventions such as `CloudPhotosTheme` usage and resource-driven
  strings.

## Guardrails

- Prefer feature-local ownership by default.
- Move code into `core` only when it is clearly shared by multiple features or app-wide.
- Avoid creating a vague "misc" shared layer.
- Be conservative when editing `core`, because changes here often affect multiple modules.

## Testing guidance

If `core` changes, prefer broader verification because multiple modules may be affected:

```bash
./gradlew test
bundle exec fastlane test
```

If the change is narrowly isolated, still call out which downstream modules may be impacted.

## Report back with

- which core module changed
- why the code belongs in `core` instead of a feature module
- downstream modules/features that may be affected
- tests run
- follow-up risks from shared impact
