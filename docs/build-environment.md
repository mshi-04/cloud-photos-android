# Build and Environment

Referenced from `AGENTS.md` (source of truth for all rules).
Source-of-truth order: `AGENTS.md` → feature-local patterns → `CLAUDE.md` → `.agent/skills/*`.
When this file and `AGENTS.md` conflict, prefer `AGENTS.md`.

---

## Build and environment rules

This project uses build flavors and required environment properties.
Be careful not to break flavored builds.

Each flavor must define the following required properties:
- `COGNITO_CLIENT_ID`
- `API_BASE_URL`
- `S3_BUCKET_NAME`

Rules:
- Do not hardcode secrets, endpoints, client IDs, or bucket names in Kotlin source.
- Do not commit environment-specific values outside the intended configuration mechanism.
- Be careful when touching `app/build.gradle.kts`, flavor logic, manifest configuration, or CI
  dummy-secret behavior.
- Flavor-specific values must be placed in the intended property or configuration flow — do not
  scatter them into Kotlin source.
- Avoid changing `build-logic` unless the task is explicitly about Gradle conventions.

## Dependency rules

- Prefer existing libraries and patterns already used in the repository.
- Do not add a new library unless clearly necessary.
- If adding a dependency is unavoidable, explain why and keep the scope minimal.
- Prefer module-local dependencies over broad app-level additions when possible.
- If a dependency change spans `app`, `core:*`, or multiple feature modules, treat it as a wider
  verification scope (run `./gradlew test`).
