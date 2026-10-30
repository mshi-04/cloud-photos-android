# Project Overview

この文書は Cloud Photos の概要、実装の入口、モジュール構成、技術スタックの確認先をまとめます。

具体的なライブラリバージョンや設定値は、必ず [`gradle/libs.versions.toml`](../gradle/libs.versions.toml) と各モジュールの `build.gradle.kts` を確認してください。

## 概要

Cloud Photos は、端末内の写真・動画をクラウドストレージ（AWS S3）へオフラインファーストでアップロード・管理する Android アプリです。

- パッケージ名: `com.appvoyager.cloudphotos`
- ルートプロジェクト名: `Cloud Photos`
- ビルドフレーバー: `dev`（`.dev` サフィックス） / `prod`

## 主要機能と実装の入口

| 機能領域 | 主な内容 | 主な実装の入口 | 関連する技術文書 |
| --- | --- | --- | --- |
| セッション確認・起動 | スプラッシュ表示、認証セッション確認、トップレベル遷移、通知チャネル初期化 | [`CloudPhotosApp`](../app/src/main/kotlin/com/appvoyager/cloudphotos/CloudPhotosApp.kt)、[`MainActivity`](../app/src/main/kotlin/com/appvoyager/cloudphotos/ui/MainActivity.kt)、[`MainViewModel`](../app/src/main/kotlin/com/appvoyager/cloudphotos/ui/MainViewModel.kt)、[`NavGraph`](../app/src/main/kotlin/com/appvoyager/cloudphotos/ui/NavGraph.kt) | [`architecture.md`](architecture.md)、[`authentication.md`](authentication.md) |
| 認証・アカウント管理 | サインイン、サインアップ、確認コード、パスワード再設定、サインアウト、退会 | `feature/auth/{domain,data,ui}` | [`authentication.md`](authentication.md) |
| メディア一覧・設定 | 端末メディアのグリッド表示、同期状態表示、列数設定（DataStore 永続化） | [`MediaScreen`](../feature/media/ui/src/main/kotlin/com/appvoyager/cloudphotos/ui/media/screen/MediaScreen.kt)、[`MediaViewModel`](../feature/media/ui/src/main/kotlin/com/appvoyager/cloudphotos/ui/media/viewmodel/MediaViewModel.kt) | [`architecture.md`](architecture.md)、[`media-sync.md`](media-sync.md) |
| カメラ撮影 | CameraX による撮影、MediaStore 保存、アップロード予約 | [`CameraScreen`](../feature/media/ui/src/main/kotlin/com/appvoyager/cloudphotos/ui/media/screen/CameraScreen.kt)、[`CameraViewModel`](../feature/media/ui/src/main/kotlin/com/appvoyager/cloudphotos/ui/media/viewmodel/CameraViewModel.kt)、[`CameraPreviewManager`](../feature/media/ui/src/main/kotlin/com/appvoyager/cloudphotos/ui/media/camera/CameraPreviewManager.kt)、[`MediaStoreCapturedPhotoWriter`](../feature/media/data/src/main/kotlin/com/appvoyager/cloudphotos/data/media/datasource/MediaStoreCapturedPhotoWriter.kt) | [`architecture.md`](architecture.md)、[`implementation-guidelines.md`](implementation-guidelines.md) |
| メディア詳細 | ページャー閲覧、ピンチズーム、Media3 ExoPlayer による動画再生 | [`MediaDetailScreen`](../feature/media/ui/src/main/kotlin/com/appvoyager/cloudphotos/ui/media/screen/MediaDetailScreen.kt) | [`implementation-guidelines.md`](implementation-guidelines.md) |
| バックグラウンド同期 | アップロード・削除キュー、WorkManager 実行、Room `upload_records` 管理 | [`UploadMediaWorker`](../feature/media/data/src/main/kotlin/com/appvoyager/cloudphotos/data/media/worker/UploadMediaWorker.kt)、[`DeleteMediaWorker`](../feature/media/data/src/main/kotlin/com/appvoyager/cloudphotos/data/media/worker/DeleteMediaWorker.kt)、[`UploadRecordDao`](../feature/media/data/src/main/kotlin/com/appvoyager/cloudphotos/data/media/db/dao/UploadRecordDao.kt) | [`media-sync.md`](media-sync.md)、[`data-model.md`](data-model.md) |
| プッシュ通知・FCM token | デバイストークンの登録・解除、アップロード完了通知の表示 | [`FcmTokenRegistrar`](../app/src/main/kotlin/com/appvoyager/cloudphotos/fcm/FcmTokenRegistrar.kt)、[`RegisterDeviceTokenWorker`](../app/src/main/kotlin/com/appvoyager/cloudphotos/fcm/RegisterDeviceTokenWorker.kt)、[`CloudPhotosFirebaseMessagingService`](../app/src/main/kotlin/com/appvoyager/cloudphotos/fcm/CloudPhotosFirebaseMessagingService.kt)、[`DeviceTokenDataSource`](../core/data/src/main/kotlin/com/appvoyager/cloudphotos/data/fcm/DeviceTokenDataSource.kt) | [`authentication.md`](authentication.md)、[`media-sync.md`](media-sync.md) |

## モジュールの見取り図

```text
app/                          # Application、MainActivity、トップレベルNavGraph、Hilt DI module、FCM起動連携
core/
  common/                     # 横断的な純粋抽象（Clock など）
  data/                       # Amplify REST 共通呼び出し、FCM デバイストークン送信
  ui/                         # Material 3 テーマ、共通 UI、共有文字列リソース
feature/
  auth/
    domain/                   # 認証 UseCase、AuthRepository、AuthResult / AuthError、値オブジェクト
    data/                     # Amplify Cognito 連携、AuthDataSource、AuthErrorMapper
    ui/                       # Login / VerificationCode / ForgotPassword / ResetPassword 画面と ViewModel
  media/
    domain/                   # メディア・同期・設定の UseCase、Repository / Scheduler interface、SyncStatus、値オブジェクト
    data/                     # S3 / API DataSource、Room DB、Worker / Scheduler 実装、DataStore、MediaStore 保存
    ui/                       # Media / Camera / MediaDetail 画面、CameraPreviewManager、ViewModel
build-logic/                  # 共有 Gradle convention plugin（Android / Kotlin / Hilt / Lint）
```

モジュールの責務境界と依存方向は [`docs/architecture.md`](architecture.md) を正本とします。

## 技術スタックと確認先

- **UI / Navigation**: Jetpack Compose、Material 3、Navigation Compose、Coil、CameraX、Media3 ExoPlayer
  - 実装規約は [`docs/implementation-guidelines.md`](implementation-guidelines.md)
- **DI / 非同期**: Hilt（KSP）、Kotlin Coroutines / Flow
  - 配線と責務は [`docs/architecture.md`](architecture.md)、基本規約は [`docs/implementation-guidelines.md`](implementation-guidelines.md)
- **ローカル永続化 / バックグラウンド処理**: Room、DataStore Preferences、WorkManager
  - DB 構造と互換性は [`docs/data-model.md`](data-model.md)、同期・Worker 契約は [`docs/media-sync.md`](media-sync.md)
- **クラウド / 認証 / 通知**: AWS Amplify（Cognito Auth / S3 Storage / REST API）、Firebase（Messaging / Analytics）
  - 認証と FCM token 管理は [`docs/authentication.md`](authentication.md)、メディア同期は [`docs/media-sync.md`](media-sync.md)
- **ビルド / 静的解析 / テスト / CI**: Gradle Kotlin DSL、Version Catalog、ktlint、detekt、JUnit 5、MockK、Turbine、`kotlinx-coroutines-test`、JUnit 4（InstrumentedTest）、Kover、Fastlane、GitHub Actions
  - 環境とローカル検証は [`docs/development-setup.md`](development-setup.md)、テスト規約は [`docs/testing.md`](testing.md)、CI / CD 構成は [`docs/ci.md`](ci.md)
