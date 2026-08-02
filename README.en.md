# CloudPhotos

[日本語](README.md)

An Android app for uploading and managing photos and videos in cloud storage.

## Features

### Authentication

Provides an authentication flow backed by AWS Cognito.

- Sign in / sign up with email and password
- Email verification with a 6-digit code (includes a resend timer)
- Forgot password → enter reset code → set a new password
- Account deletion — remove the FCM token → delete all data from S3 / DynamoDB → delete the Cognito account

### Media grid

Lists photos and videos on the device in a grid.

- Switchable column count (2–4 columns, persisted with DataStore)
- Sync status indicators (`PENDING_UPLOAD` / `SYNCED` / `PENDING_DELETE` / `ERROR`)
- Pull to refresh

### Camera

An in-app camera built with CameraX.

- Photo capture (low-latency mode)
- Front / back camera switching
- Pinch to zoom and tap to focus
- Saves to MediaStore after capture and uploads automatically
- Error handling for insufficient storage and unavailable cameras

### Media detail

Views photos and videos in full screen.

- Swipe between items with a horizontal pager
- Pinch to zoom on images (`ZoomableContainer`)
- Video playback with Media3 ExoPlayer
- Toggle toolbar visibility

### Background sync

An offline-first upload and delete queue built on WorkManager.

- `UploadMediaWorker` — uploads pending records to S3 and syncs them with the API
- `DeleteMediaWorker` — deletes items scheduled for removal from the remote
- One-time work constrained to network connectivity, with deduplication
- Upload records (`UploadRecordEntity`) persisted in Room

### Push notifications

Receives push notifications through Firebase Cloud Messaging (FCM).

## Tech stack

| Category | Library / version |
|---|---|
| Language | Kotlin 2.3 |
| UI | Jetpack Compose BOM 2026.02 + Material 3 |
| Architecture | Clean Architecture (domain / data / ui) |
| DI | Hilt 2.59 + KSP |
| Navigation | Navigation Compose 2.9 |
| Camera | CameraX 1.5 |
| Image loading | Coil 3.4 (with Compose integration) |
| Video playback | Media3 ExoPlayer 1.9 |
| Authentication | AWS Amplify Cognito 2.33 |
| Storage | AWS Amplify S3 2.33 |
| Analytics | Firebase Analytics (BOM 34.9) |
| Push notifications | Firebase Cloud Messaging |
| Background work | WorkManager 2.11 |
| Local DB | Room 2.8 |
| Preferences | DataStore Preferences 1.2 |
| Lint | ktlint 14.2 + detekt 2.0-alpha |
| Testing | JUnit 5 + MockK + Turbine + kotlinx-coroutines-test + Kover |
| Min SDK | 29 (Android 10) |
| Compile SDK | 36 |
| JDK | 17 |

## Screen flow

```text
Splash (session check)
 ├─ Session valid   → Media grid (home)
 └─ Session invalid → Login
                       ├─ Sign up        → Email verification
                       └─ Forgot password → Reset

Media grid (home)
 ├─ Tap media       → Media detail (image / video)
 ├─ FAB             → Camera
 ├─ Sign out        → Login
 └─ Delete account  → Login
```

## Module structure

```text
app/                          # Activity, NavGraph, DI bootstrap, Amplify / Firebase initialization
core/
  common/                     # Cross-cutting abstractions (Clock and similar)
  data/                       # Amplify REST client, FCM device token management
  ui/                         # Material 3 theme, colors, shared components, string resources
feature/
  auth/
    domain/                   # Use cases, repository interfaces, value objects (Email, Password, UserId, JwtToken, ClientId)
    data/                     # Amplify Cognito integration, auth error mappers
    ui/                       # Login / VerificationCode / ForgotPassword / ResetPassword screens and ViewModels
  media/
    domain/                   # Media models (Media, MediaType), SyncStatus, upload/delete use cases, settings use cases (GridColumnCount and similar)
    data/                     # S3 upload, Room DB (UploadRecordDao), Workers, Schedulers, DataStore (settings persistence)
    ui/                       # MediaScreen / CameraScreen / MediaDetailScreen, CameraPreviewManager, ViewModels
build-logic/                  # Shared Gradle convention plugins (cloudphotos.lint and others)
```

## Setup

### Prerequisites

- Android Studio Hedgehog or later
- JDK 17
- Ruby 4.0 + Bundler 4.0 (for Fastlane)

### Local configuration

Copy the sample file and fill in the values.

```bash
cp local.properties.sample local.properties
```

```properties
sdk.dir=/path/to/android/sdk

# Dev environment
DEV_API_BASE_URL=https://your-dev-api.example.com/
DEV_COGNITO_CLIENT_ID=your_dev_cognito_client_id
DEV_S3_BUCKET_NAME=your-dev-bucket

# Prod environment
PROD_API_BASE_URL=https://your-api.example.com/
PROD_COGNITO_CLIENT_ID=your_prod_cognito_client_id
PROD_S3_BUCKET_NAME=your-prod-bucket
```

> **Never commit `local.properties`.** It is already listed in `.gitignore`.

### Build

```bash
# Dev debug APK
./gradlew assembleDevDebug

# Dev release APK (through Fastlane)
bundle exec fastlane build_dev

# Prod release APK + AAB (through Fastlane)
bundle exec fastlane build_prod
```

### Build flavors

| Flavor | Purpose | applicationId suffix |
|---|---|---|
| `dev` | Development and testing | `.dev` |
| `prod` | Production | none |

Each flavor reads the `DEV_*` / `PROD_*` prefixed properties from `local.properties`.

## Development

### Branching

- Branch off `develop` for feature work.
- Direct pushes to `main` and `develop` are not allowed.

### Lint

```bash
./gradlew ktlintFormat   # auto-fix (local only)
./gradlew ktlintCheck detekt
```

### Testing

```bash
# All modules
./gradlew test

# A single module
./gradlew :feature:auth:domain:test
./gradlew :feature:auth:data:test
./gradlew :feature:auth:ui:test
./gradlew :feature:media:domain:test
./gradlew :feature:media:data:test
./gradlew :feature:media:ui:test

# Through Fastlane (same environment as CI)
bundle exec fastlane test

# Instrumented tests (require an emulator or a physical device)
./gradlew :feature:media:data:connectedDebugAndroidTest
```

Unit tests use **JUnit 5 + MockK + Turbine + kotlinx-coroutines-test** and follow the Arrange / Act / Assert structure.
Instrumented tests run on `AndroidJUnitRunner`, so they use **JUnit 4** and verify Room, DataStore and WorkManager against real instances.
Coverage is measured with Kover (unit tests only).

### CI (continuous integration)

GitHub Actions runs on pushes to `main` and `develop`, and on every pull request.

| Step | Command | Description |
|---|---|---|
| Lint | `bundle exec fastlane lint` | ktlint + detekt |
| Test | `bundle exec fastlane test` | Unit tests for the dev flavor |
| Coverage | Kover XML + PR comment | Reports unit test coverage on the pull request |
| Instrumented Test | `./gradlew :feature:media:data:connectedDebugAndroidTest` | Verifies Room / DataStore / WorkManager on an API 36 emulator |

A pull request cannot be merged until CI passes. Fork pull requests get dummy secrets automatically.
The coverage comment exists for visibility; no threshold currently fails CI.

### CD (continuous delivery)

Pushing to `main` triggers a Prod build, and pushing to `develop` triggers a Dev build.
Build artifacts (APK / AAB) are uploaded as GitHub Actions artifacts.

## Architecture

This project follows Clean Architecture, splitting each feature into three layers: `domain`, `data` and `ui`.

- **domain** — framework independent. Use cases, repository interfaces, value objects and business rules
- **data** — framework implementations such as SDKs, databases and networking. Error mapping happens in this layer
- **ui** — declarative UI with Jetpack Compose. ViewModels hold state and call use cases

Dependencies flow `ui → domain ← data`; the reverse direction is not allowed.

See the following documents for details. They are written in Japanese.

- [docs/architecture-decisions.md](docs/architecture-decisions.md) — architectural decisions and the reasoning behind them
- [docs/media-upload-flow.md](docs/media-upload-flow.md) — upload / delete flow and SyncStatus transitions
- [docs/error-handling-guide.md](docs/error-handling-guide.md) — CancellationException and error mapping patterns
- [AGENTS.md](AGENTS.md) — development rules shared by humans and AI agents. [CLAUDE.md](CLAUDE.md) is an identical copy that differs only in the skill paths

## Security

See [SECURITY.md](SECURITY.md) for how to report a vulnerability.
