# Architecture Decisions

This document explains why the repository is structured the way it is.
It is not a rule file — rules live in `AGENTS.md`.
The goal is to make the reasoning visible so that both humans and AI agents can make consistent
decisions.

## Why modular? Why `app / core / feature`?

**Decision**: separate modules for `app`, `core:*`, and each `feature`.

**Reason**: large Android apps built in a single module become difficult to scale and test.
Module boundaries enforce dependency direction at compile time, not just by convention.
They also allow faster incremental builds and more targeted testing.

**What we avoid**: a flat structure where anything can call anything, leading to tangled
dependencies
that are expensive to untangle later.

## Why split each feature into `domain / data / ui`?

**Decision**: each feature has three layers with explicit allowed-dependency directions.

**Reason**: this is the clean architecture principle applied to Android features.

- `domain` stays framework-free, making business rules testable without device or Android context.
- `data` isolates all SDK and persistence concerns so they can change without touching business
  logic.
- `ui` remains declarative and state-driven, making it easier to test and swap renderers.

**What we avoid**: ViewModels that call Room DAOs directly, or use cases that import Amplify SDKs.
Mixing layers makes it impossible to test business logic without an Android environment.

## Why value objects instead of raw primitives?

**Decision**: validated domain concepts such as `Email`, `Password`, and `UserId` are expressed as
`@JvmInline value class` with private constructors and factory validation.

**Reason**: raw `String` parameters cannot encode their constraints.
A function that takes `(email: String, userId: String)` can be called with arguments swapped at the
call site with no compile-time protection.
Value objects move validation to the construction site, make domain APIs self-documenting, and
prevent invalid states from propagating through the system.

**What we avoid**: scattered `if (email.contains("@"))` checks at multiple call sites, or domain
models that accept unvalidated input.

## Why keep provider-specific code inside `data`?

**Decision**: Cognito, Amplify, Firebase, Room, and WorkManager details must not cross out of
`data`.

**Reason**: provider SDKs change. Auth providers are replaced. Storage backends evolve.
Confining SDK details to `data` means a provider migration requires only `data` changes — domain
and UI remain stable.

**What we avoid**: Cognito error types appearing in use cases, or Firebase types referenced in
Compose screens. When that happens, a provider change becomes a cross-repo refactor.

## Why separate worker / scheduler / repository / datasource?

**Decision**: background work in the `media` feature uses separate classes for workers,
schedulers, repositories, and data sources rather than a single coordinating class.

**Reason**: each responsibility has a different lifecycle and testability profile.

- Workers manage WorkManager execution context.
- Schedulers decide when to enqueue and with what constraints.
- Repositories expose domain contracts.
- Data sources handle raw reads/writes to local or remote storage.

Collapsing these makes it impossible to test scheduling logic independently of persistence or
vice versa. It also makes retry behavior and state transitions harder to reason about.

**What we avoid**: a single `UploadManager` class that enqueues work, tracks records, calls the
network, and maps errors — making every bug fix a surgery.

## Why is `android.util.Log` forbidden in production code?

**Decision**: `android.util.Log` must not appear in committed production code.

**Reason**: log calls with no severity/sampling control can leak sensitive data (tokens, user IDs,
file paths) into logcat in production. They also add noise that makes real diagnostics harder.
This repository currently has no structured logging abstraction; where logging is genuinely needed,
it should be designed intentionally rather than added ad hoc.

**What we avoid**: debug-level token dumps or upload path logs that survive into release builds,
visible to anyone with adb access on an unlocked device.

## Why treat lint / test / CI as a harness?

**Decision**: lint, unit tests, and CI are treated as gates, not suggestions.

**Reason**: a change that breaks lint or tests leaves the repository in a state where the next
change cannot be verified. CI is the shared, authoritative environment. A passing local run that
fails CI is not a passing run.
Treating verification as part of the definition of "done" keeps the main branch releasable.

**What we avoid**: a culture of "it works on my machine" where CI red is normalized, and the test
suite degrades into an ignored checkbox.
