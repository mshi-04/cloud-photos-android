# Architecture

Cloud Photos は、`app / core / feature` のマルチモジュール構成と、`ui -> domain <- data` の Clean Architecture + MVVM を採用します。

## モジュール境界と依存方向

| 層・領域 | モジュール | 役割 |
| --- | --- | --- |
| App composition | `:app` | アプリ初期化、トップレベル Navigation、Hilt 配線、FCM 起動連携、flavor 設定 |
| Feature UI | `:feature:<name>:ui` | Compose screen / component、ViewModel、UI state / effect |
| Feature Domain | `:feature:<name>:domain` | UseCase、Repository / Scheduler interface、domain model、value object、Result / Error |
| Feature Data | `:feature:<name>:data` | Repository 実装、DataSource、Mapper、Room Entity / DAO、DataStore、Worker / Scheduler 実装 |
| Shared concerns | `:core:common` / `:core:data` / `:core:ui` | 複数フィーチャーで実際に共有する最小限の契約・基盤・テーマ・文字列リソース |
| Build conventions | `build-logic` | 複数モジュール共通の Gradle convention plugin |

- 機能モジュールの依存方向は `ui -> domain <- data` とし、逆方向の依存や `ui` から `data` への直接依存を禁止する。
- `domain` モジュールは `javax.inject` と `kotlinx-coroutines-core`（および必要に応じた `:core:common`）のみに依存し、Android framework、Compose、Room、WorkManager、Amplify、Firebase の型を持ち込まない。これにより JVM 単体で高速に検証でき、provider 差し替えの影響を `data` に閉じ込められる。
- `:core:*` は複数フィーチャーで実利用がある要素だけを置く。「汎用的そう」「将来使うかもしれない」という理由で `:core:*` に移動しない。
- 旧 `settings` 機能は `feature:media` に統合済みであり、列数設定などの契約・永続化・UI はそれぞれ `feature:media:{domain,data,ui}` に置く。

## `:app`（Composition Root）

`:app` は全モジュールを束ねる組み立て役であり、フィーチャー固有のビジネスルールや DAO / DataSource の実装本体を持たない。

- [`CloudPhotosApp`](../app/src/main/kotlin/com/appvoyager/cloudphotos/CloudPhotosApp.kt):
  - Amplify plugin（Cognito / S3 / API）の初期化。
  - `Configuration.Provider` の実装と `HiltWorkerFactory` の提供（[`AndroidManifest.xml`](../app/src/main/AndroidManifest.xml) で既定の `WorkManagerInitializer` を無効化している前提）。
  - アップロード進行中通知（`UploadNotificationHelper`）とアップロード完了通知（`CloudPhotosFirebaseMessagingService.CHANNEL_ID`）の 2 つの通知チャネル作成。
  - アプリ起動時の `FcmTokenRegistrar.register()` 呼び出し。
- [`MainActivity`](../app/src/main/kotlin/com/appvoyager/cloudphotos/ui/MainActivity.kt) / [`MainViewModel`](../app/src/main/kotlin/com/appvoyager/cloudphotos/ui/MainViewModel.kt) / [`NavGraph`](../app/src/main/kotlin/com/appvoyager/cloudphotos/ui/NavGraph.kt):
  - スプラッシュ表示中のセッション確認（`GetSessionUseCase`）と、`Authenticated` 遷移時の FCM token 登録。
  - 認証状態に応じた start destination の決定と、フィーチャー間を跨ぐ画面遷移の配線。
- `app/src/main/kotlin/com/appvoyager/cloudphotos/di/`:
  - 各フィーチャーの Repository / DataSource / Scheduler interface と実装の `@Binds`、および `CloudPhotosDatabase` / DAO や `Clock` の `@Provides` を行う。新しい Repository や DataSource、DAO を追加した際は対応する Module も更新する。

## UI 層と単方向データフロー

```text
UI event (関数呼び出し) -> ViewModel -> UseCase / domain abstraction -> UI state / effect -> Screen / Content
```

- Screen は ViewModel と接続する入口とし、Content は状態と callback のみを受け取る stateless な Composable に分ける。
- ViewModel は UseCase または domain abstraction を呼び、DAO、SDK client、Repository 実装、Worker を直接参照しない。
- UI state には domain model や value object を保持してよく、DTO、Room Entity、SDK model は入れない。

## UseCase と Domain Abstraction

UseCase は、ビジネスルールや複数依存の調整を名前付き操作として表現し、ViewModel を画面状態管理に集中させるために置く。

UseCase を作成・維持する基準:

- 複数の Repository や Scheduler を調整する（例: [`PrepareUploadQueueUseCase`](../feature/media/domain/src/main/kotlin/com/appvoyager/cloudphotos/domain/media/usecase/PrepareUploadQueueUseCase.kt)、[`DeleteMediaUseCase`](../feature/media/domain/src/main/kotlin/com/appvoyager/cloudphotos/domain/media/usecase/DeleteMediaUseCase.kt)、[`RecordMediaUploadUseCase`](../feature/media/domain/src/main/kotlin/com/appvoyager/cloudphotos/domain/media/usecase/RecordMediaUploadUseCase.kt)）。
- 複数画面・複数 ViewModel・Worker から同じ入口で再利用する（例: [`ScheduleUploadUseCase`](../feature/media/domain/src/main/kotlin/com/appvoyager/cloudphotos/domain/media/usecase/ScheduleUploadUseCase.kt)）。
- data 操作の前後に入力 validation、保護フィルタ、チャンク分割、状態遷移を伴う（例: [`SyncUploadRecordsUseCase`](../feature/media/domain/src/main/kotlin/com/appvoyager/cloudphotos/domain/media/usecase/SyncUploadRecordsUseCase.kt)、[`GetPendingMediaUseCase`](../feature/media/domain/src/main/kotlin/com/appvoyager/cloudphotos/domain/media/usecase/GetPendingMediaUseCase.kt)）。

避けるもの・直接利用の判断:

- 新規追加において、付加価値のない単純な Repository メソッドの 1 対 1 ラップを機械的に増やさない（`feature:auth:domain` には先行実装の単純委譲 UseCase があるが、新規設計の模倣元にはしない）。
- 単一画面の操作で調整や固有のビジネスルールを持たず、Android I/O を domain の契約として切り出すだけで足りる場合は、[`CameraViewModel`](../feature/media/ui/src/main/kotlin/com/appvoyager/cloudphotos/ui/media/viewmodel/CameraViewModel.kt) が [`CapturedPhotoWriter`](../feature/media/domain/src/main/kotlin/com/appvoyager/cloudphotos/domain/media/repository/CapturedPhotoWriter.kt) を直接呼ぶように、domain abstraction を直接注入してよい。

## Value Object

検証済みのドメイン概念や識別子の取り違え防止には `@JvmInline value class` を使う。

- `private constructor(val value: T)` と `companion object` の `fun of(raw: T)` で正規化（`trim()` など）と `require` による検証を閉じ込める。
- 検証を持たない `IsDeleted` や、同じ `Long` を包む `MediaCreatedAt` / `MediaUploadedAt` も、引数順の取り違えを型で防ぐために分けて維持する。
- data / UI との境界（mapper、DTO 組み立て、DB 書き込み）まで value object のまま運び、途中で生プリミティブへ戻さない。

## Repository / DataSource / Mapper

- **Repository interface（domain）**:
  - 保存先と役割の粒度で分割する。`auth` は単一 provider のため [`AuthRepository`](../feature/auth/domain/src/main/kotlin/com/appvoyager/cloudphotos/domain/auth/repository/AuthRepository.kt) に集約し、`media` は [`LocalMediaRepository`](../feature/media/domain/src/main/kotlin/com/appvoyager/cloudphotos/domain/media/repository/LocalMediaRepository.kt)、[`LocalUploadRecordsRepository`](../feature/media/domain/src/main/kotlin/com/appvoyager/cloudphotos/domain/media/repository/LocalUploadRecordsRepository.kt)、[`RemoteUploadRecordsRepository`](../feature/media/domain/src/main/kotlin/com/appvoyager/cloudphotos/domain/media/repository/RemoteUploadRecordsRepository.kt)、[`UploadRepository`](../feature/media/domain/src/main/kotlin/com/appvoyager/cloudphotos/domain/media/repository/UploadRepository.kt)、[`SettingsRepository`](../feature/media/domain/src/main/kotlin/com/appvoyager/cloudphotos/domain/media/repository/SettingsRepository.kt)、[`CapturedPhotoWriter`](../feature/media/domain/src/main/kotlin/com/appvoyager/cloudphotos/domain/media/repository/CapturedPhotoWriter.kt)、[`UploadScheduler`](../feature/media/domain/src/main/kotlin/com/appvoyager/cloudphotos/domain/media/repository/UploadScheduler.kt)、[`DeleteScheduler`](../feature/media/domain/src/main/kotlin/com/appvoyager/cloudphotos/domain/media/repository/DeleteScheduler.kt) に分けている。
  - シグネチャには domain の型と `Flow` のみを出す。
  - 失敗契約は操作ごとに定義する。provider 由来の予期される失敗を呼び出し側で分岐する操作は `AuthResult<T>` / `UploadResult<T>`（または `SavePhotoResult`）を返し、ローカル DB 操作などは生の値や `Unit` を返す。
- **Repository 実装（data）**:
  - DataSource を呼び出して domain contract へ接続する。画面都合の分岐や同期の状態遷移判断は Repository 実装に置かず、UseCase や Worker 側で扱う。
- **DataSource（data）**:
  - Amplify（Cognito / S3 / REST）、Firebase、Room DAO、DataStore、MediaStore / ContentResolver などの外部 I/O を閉じ込める。
- **Mapper（data）**:
  - DTO / JSON / Room Entity と domain model の変換、および provider 例外から domain error（`AuthError` / `UploadError`）への変換を一箇所に集約し、Repository と DataSource に同じ変換を分散させない。

## Worker と Scheduler の分離

メディアのアップロードと削除は、アプリがバックグラウンドに移っても完了させるため WorkManager を使う。

- **Scheduler（domain interface + data 実装）**: WorkManager への enqueue 条件（unique work name、policy、constraints）だけを担当し、UseCase から WorkManager 依存を切り離す。
- **Worker（data）**: バックグラウンドでのレコード取得、S3 転送、API 登録・削除、一時失敗と永続失敗の振り分け、通知表示を担当する。詳細な契約は [`docs/media-sync.md`](media-sync.md) を正本とする。
- なお、FCM デバイストークン登録の [`RegisterDeviceTokenWorker`](../app/src/main/kotlin/com/appvoyager/cloudphotos/fcm/RegisterDeviceTokenWorker.kt) は `:app` 内の起動・FCM 受信経路から直接使われるため、domain の Scheduler interface を介さず `companion object.enqueue()` で登録する。詳細は [`docs/authentication.md`](authentication.md) を正本とする。
