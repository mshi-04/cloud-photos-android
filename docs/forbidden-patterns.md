# Forbidden Patterns

Patterns to avoid in this repository, with brief reasoning for each.
Rules are enforced by `AGENTS.md`; this file explains the *why* to aid judgment in edge cases.

---

## android.util.Log in production code

```kotlin
// forbidden
Log.d("MediaRepo", "Uploading file: $path")
```

**Why**: logcat is readable by any process with adb access on an unlocked device.
Log calls can expose tokens, file paths, and user identifiers.
This repository has no structured logging abstraction; omit the log or introduce one deliberately.

---

## Committing temporary debug logs

Even if the log uses a proper abstraction, temporary debug-only statements must not be committed.

**Why**: debug logs degrade signal-to-noise ratio in logcat, may expose internal state, and rarely
get cleaned up once merged.

---

## UI calling data implementations directly

```kotlin
// forbidden in ViewModel / Compose
@Inject lateinit var uploadRepositoryImpl: UploadRepositoryImpl
@Inject lateinit var mediaDao: MediaDao
```

**Why**: `ui` should depend only on `domain` interfaces and `core:ui` / `core:common`.
Direct data dependency bypasses use cases, breaks testability, and couples UI to persistence
or SDK details.

---

## Android / Compose / Room / WorkManager / provider SDK in domain

```kotlin
// forbidden in feature:*:domain
import androidx.room.Entity
import com.amplifyframework.auth.AuthException
import androidx.work.WorkManager
```

**Why**: `domain` must stay framework-free so business logic is testable with pure JVM tests.
Framework dependencies in domain make it impossible to test use cases without an Android device
or emulator.

---

## Returning DTOs, entities, or provider responses to UI

```kotlin
// forbidden
fun getItems(): List<MediaEntity>         // Room entity to UI
fun getAuthState(): AuthSignInResult      // Amplify type to UI
```

**Why**: data-layer models encode persistence or SDK concerns that should not leak across the
boundary. Changes to the schema or SDK response shape would break UI. Use mappers at the boundary.

---

## Business logic accumulating in RepositoryImpl

```kotlin
// forbidden
class UploadRepositoryImpl {
    fun upload(file: File) {
        if (file.size > MAX_SIZE) throw ...   // business rule — belongs in domain
        if (!networkAvailable()) return       // orchestration — belongs in use case
        ...
    }
}
```

**Why**: `RepositoryImpl` should delegate to data sources and mappers.
Business rules belong in use cases; they are easier to test there and don't get duplicated.

---

## Silent large refactors

**Why**: broad package moves, mass renames, and cross-feature restructuring during a focused task
make the diff unreadable and introduce merge conflicts. They also change things the task owner did
not intend or approve. Surface large structural changes as explicit proposals, not side effects.

---

## Unrelated cleanup

Fixing formatting, renaming variables, or reorganizing imports in files you are not otherwise
changing.

**Why**: every touched file is a potential merge conflict and a review burden.
Cleanup commits should be separate, explicit, and intentional.

---

## Hardcoded secrets, endpoints, bucket names, or client IDs

```kotlin
// forbidden
private const val API_URL = "https://api.example.com"
private const val COGNITO_CLIENT_ID = "abc123"
```

**Why**: hardcoded values end up in version history, can be exposed in public forks, and cannot
be rotated without a code change. Use the project's flavor-based configuration mechanism.

---

## Swallowing CancellationException

```kotlin
// forbidden
runCatching {
    someCoroutine()
}.onFailure { /* nothing */ }
```

**Why**: `CancellationException` is how Kotlin coroutines signal cooperative cancellation.
Swallowing it prevents coroutine scopes from cancelling cleanly, leading to leaked coroutines
and broken cancellation chains.

Always re-throw:
```kotlin
}.onFailure { e ->
    if (e is CancellationException) throw e
    // handle other errors
}
```

---

## Casual cross-feature dependencies

```kotlin
// forbidden in feature:media
import com.appvoyager.cloudphotos.feature.auth.domain.usecase.GetCurrentUserUseCase
```

**Why**: features should be independently deployable and testable. Direct feature-to-feature
imports create coupling that makes it hard to test, refactor, or remove a feature.
Shared contracts belong in `core:common`, not inside a sibling feature.
