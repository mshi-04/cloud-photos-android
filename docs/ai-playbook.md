# AI Playbook

This document describes how AI agents should approach work in this repository.
It is a workflow guide, not a rule set. Rules live in `AGENTS.md`.

## 1. Read before you act

Read files in this order before making any change:

1. `AGENTS.md` — source of truth for all rules
2. `app/AGENTS.md`, `core/AGENTS.md`, `feature/<name>/AGENTS.md` — local refinements for the area you are touching
3. `CLAUDE.md` — Claude-specific entry guidance and stack overview
4. `.agent/skills/*` — skill files for the pattern category you need

If guidance conflicts, prefer the higher item in the list and stay conservative.

## 2. Before you start coding

- Identify the single smallest module affected.
- Confirm the correct layer (`domain`, `data`, or `ui`).
- Read at least one existing file in the same feature to understand naming and patterns.
- Do not invent a new abstraction when an equivalent already exists in the same feature.

## 3. Deciding where code goes

Use this decision tree:

| What it is | Where it goes |
|---|---|
| Business rule, use case, value object | `feature:<name>:domain` |
| Repository implementation, data source, mapper, DAO, worker | `feature:<name>:data` |
| ViewModel, UI state, Compose screen | `feature:<name>:ui` |
| Cross-feature shared abstraction (reuse already justified) | `core:*` |
| App entry, navigation, Hilt wiring | `app` |

When in doubt, keep the change in the feature layer you are already in.

## 4. Keep changes small

- Touch the fewest files necessary.
- Do not clean up unrelated code while implementing a feature.
- Do not move packages unless the task explicitly requires it.
- If a broader change seems necessary, surface it as a follow-up item.

## 5. Verification: what to run and when

See `docs/verification-policy.md` for the full policy.

Short version:
- **During development**: run the single affected module's tests.
- **Before PR**: run `ktlintCheck detekt` and affected module tests.
- **CI is the final gate**: `bundle exec fastlane lint` and `bundle exec fastlane test` must pass before merge.

Gradle sync is not required after every source-only change.

## 6. Before opening a PR

- [ ] Lint passes: `./gradlew ktlintFormat ktlintCheck detekt`
- [ ] Tests pass for changed modules
- [ ] No hardcoded secrets, endpoints, or bucket names
- [ ] No committed debug logs or `android.util.Log` calls
- [ ] No cross-layer dependency violations introduced
- [ ] No `CancellationException` swallowed

## 7. Report format

Report every change with:

```text
touched modules: <list>
placement reason: <why each file lives in its layer>
summary: <what changed and why>
tests run: <commands and result>
tests not run: <commands and reason>
follow-up items: <known limitations or next steps>
```

If you did not run tests, say so explicitly.

## 8. Supplementary references

Consult these when relevant:

| Document | When to read |
|---|---|
| `docs/media-upload-flow.md` | Before changing workers, schedulers, SyncStatus, or upload/delete use cases |
| `docs/error-handling-guide.md` | Before writing any `catch` block or `runCatching` in this repo |
| `docs/architecture-decisions.md` | When uncertain why the structure is as it is |

## 9. Things not to do

Avoid these — see `docs/forbidden-patterns.md` for the full list with reasoning:

- `android.util.Log` in production code
- Committing temporary debug logs
- UI calling `data` implementations directly
- Android/Compose/Room in `domain`
- DTOs or entities returned to UI
- Business logic in `RepositoryImpl`
- Silent large refactors
- Hardcoded secrets or endpoints
- Swallowing `CancellationException`
- Casual cross-feature dependencies

## 10. Git operations ownership

Git operations are user-owned unless explicitly requested.
Do not commit, push, create a branch, or open a pull request unless the user explicitly asks.
Focus on code changes, verification, and reporting.

## 11. Pull request and branch workflow

The following applies when the user explicitly requests Git operations:

- Branch from `develop` for feature work unless explicitly told otherwise.
- Do not push directly to protected branches.
- Keep PR scope tight and consistent with the branch purpose.
- Follow the repository PR template expectations, including test notes and Android-specific
  checks when applicable.

## 12. Change strategy (quick reference)

When asked to implement something, follow this order:
1. Identify the smallest affected module.
2. Confirm the correct layer for the logic.
3. Reuse existing patterns in the same feature first.
4. Make the smallest safe change.
5. Run targeted verification.
6. Summarize exactly what changed, what was not changed, and remaining risks.

For detail on each step, see sections 1–7 above.
