# Error Handling Guide

This document covers the error handling patterns used in this repository.
Rules live in `AGENTS.md`. This file explains *how* to apply them correctly.

---

## CancellationException must always be re-thrown

Kotlin coroutines signal cooperative cancellation via `CancellationException`.
Swallowing it prevents coroutine scopes from cancelling cleanly, causes coroutine leaks,
and breaks structured concurrency.

**Every `catch` block that handles `Exception` or `Throwable` must re-throw `CancellationException`.**

### With try/catch

```kotlin
try {
    someCoroutine()
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    // handle
}
```

### With runCatching

`runCatching` catches all `Throwable` including `CancellationException`.
Always re-throw in `onFailure`:

```kotlin
runCatching {
    someCoroutine()
}.onFailure { e ->
    if (e is CancellationException) throw e
    // handle
}
```

### Nested runCatching

Each nested `runCatching` block must re-throw independently:

```kotlin
runCatching {
    outer()
}.onFailure { e ->
    if (e is CancellationException) throw e
    runCatching {
        cleanup()
    }.onFailure { inner ->
        if (inner is CancellationException) throw inner
    }
}
```

Forgetting the inner re-throw is a common mistake. The outer re-throw does not protect
against cancellation swallowed inside a nested block.

---

## Error mapping belongs in the data layer

Domain use cases and domain models must not reference provider-specific error types
(Amplify exceptions, Cognito errors, Room exceptions, etc.).

### Pattern

1. Data source catches provider-specific exceptions.
2. A mapper object translates them to domain-facing error types.
3. The repository implementation returns domain errors.

```kotlin
// data layer
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    UploadResult.Error(UploadErrorMapper.map(e))  // domain-facing error
}

// domain layer — no provider types visible
sealed class UploadError {
    data object NetworkError : UploadError()
    data object StorageError : UploadError()
    data object Unknown : UploadError()
}
```

---

## Sealed result types over exceptions

The `media` feature uses `UploadResult<T>` to represent operation outcomes at domain boundaries:

```kotlin
sealed class UploadResult<out T> {
    data class Success<T>(val value: T) : UploadResult<T>()
    data class Error(val error: UploadError) : UploadResult<Nothing>()
}
```

Prefer sealed result types over throwing exceptions when:
- the caller must handle both success and failure paths
- the error is a domain-level expected outcome (not a programming error)
- the operation crosses the data/domain boundary

Do not return sealed results *and* also throw exceptions for the same operation.

---

## Permanent vs temporary failures in workers

`UploadMediaWorker` and `DeleteMediaWorker` classify errors to decide retry behavior.
This classification lives in a private helper such as `isPermanentFailure()`.

| Condition | Behavior |
|---|---|
| `CancellationException` | Re-throw immediately |
| Permanent failure (e.g., HTTP 4xx) | Mark record as `ERROR`; do not retry |
| Temporary failure (e.g., network, HTTP 5xx) | Set retry flag; `Result.retry()` at the end |

When changing this classification, verify that the SyncStatus transitions described in
`docs/media-upload-flow.md` remain consistent.

---

## Auth error mapping

Auth provider exceptions (Cognito/Amplify) must be translated in `feature/auth/data` mappers.
See `.agent/skills/android-auth-error/SKILL.md` for auth-specific guidance.

Do not return Cognito-specific error types to domain or UI.
Preserve mappings that intentionally collapse provider errors to avoid account enumeration.