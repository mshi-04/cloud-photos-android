# app/AGENTS.md

Local guidance for the `app` module.
Read root `AGENTS.md` first, then apply the rules below.
If this file conflicts with root `AGENTS.md`, prefer the root file and keep the change conservative.

## Intent of the app module

`app` is the composition root of the application.
It should own app-wide startup, top-level navigation, Android application wiring, manifest-level concerns, build flavors, and dependency injection bootstrap.
It should not become a home for feature business logic.

## Put code here only when it is app-wide

Examples:
- `Application` setup
- top-level navigation graph composition
- Hilt bootstrap/wiring modules that connect feature implementations
- manifest declarations
- flavor/build configuration
- app-wide service initialization

Do not place here:
- feature-specific business rules
- feature-specific repository logic
- feature-specific UI state/effect models unless they are truly top-level app state
- code that belongs naturally inside `feature/*`

## Navigation rules

- Keep top-level navigation assembly here.
- Prefer feature modules to own their internal UI/state logic, while `app` coordinates the high-level graph.
- Do not spread route knowledge across unrelated files when a centralized nav definition can handle it.

## DI / bootstrap rules

- `app` may assemble bindings that wire feature implementations into the running application.
- Keep DI modules focused by responsibility.
- Avoid turning `app` DI into a dumping ground for unrelated bindings.
- If a binding is feature-local, prefer keeping its implementation pattern near the feature unless app-wide assembly is required.

## Build / flavor rules

This module contains high-risk configuration because it owns app-wide Android and flavor setup.
Be especially careful when touching:
- `app/build.gradle.kts`
- flavor configuration
- manifest entries
- Firebase / Amplify initialization
- environment property usage

Rules:
- Do not hardcode secrets or environment-specific values.
- Preserve existing flavor/property loading behavior.
- Be cautious with changes that affect CI dummy-secret behavior or release/dev build parity.

## UI rules

- Keep truly top-level app UI concerns here.
- Feature screens and feature-local state should remain in feature modules.
- When changing top-level entry flow, be explicit about how authentication and home/start routing behavior changes.

## Testing guidance

Changes in `app` often have broad impact.
Prefer broader verification:

```bash
./gradlew test
bundle exec fastlane test
```

If app wiring changes touch auth/media/settings integration points, call that out explicitly.

## Report back with

- which app-wide concern changed
- why the change belongs in `app`
- which features or startup/navigation flows are affected
- tests run
- remaining integration risks
