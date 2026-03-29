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

### Where to set properties

Properties are resolved in this priority order:

1. **`local.properties`** (local development — never commit this file):
   ```properties
   DEV_COGNITO_CLIENT_ID=xxxxx
   DEV_API_BASE_URL=https://dev.example.com/
   DEV_S3_BUCKET_NAME=my-dev-bucket
   PROD_COGNITO_CLIENT_ID=yyyyy
   PROD_API_BASE_URL=https://api.example.com/
   PROD_S3_BUCKET_NAME=my-prod-bucket
   ```

2. **Gradle project properties** (e.g., `-PDEV_COGNITO_CLIENT_ID=xxxxx` on the command line
   or via `gradle.properties`).

3. **CI environment**: Pass `DEV_*` / `PROD_*` values **as Gradle project properties**,
   which is what `findProperty` resolves. Two equivalent ways:
    - Command-line: `-PDEV_COGNITO_CLIENT_ID=xxxxx`
    - Environment variable mapped to Gradle property:
      `ORG_GRADLE_PROJECT_DEV_COGNITO_CLIENT_ID=xxxxx`
      (Gradle automatically maps `ORG_GRADLE_PROJECT_*`-prefixed env vars to project properties)

   The root `build.gradle.kts` reads `local.properties` first, then falls back to
   `findProperty` (Gradle project properties), so either method above works in CI.

> **Key naming**: prefix the base name with the flavor prefix — `DEV_` or `PROD_`.
> Example: `DEV_COGNITO_CLIENT_ID`, `PROD_API_BASE_URL`.

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
