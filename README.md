# CloudPhotos

写真・動画をクラウドストレージにアップロード・管理する Android アプリです。

## 機能

### 認証

AWS Cognito による認証フローを提供します。

- メールアドレス・パスワードによるサインイン / サインアップ
- 6 桁コードによるメール認証（再送タイマー付き）
- パスワード忘れ → リセットコード入力 → 新パスワード設定

### メディアグリッド

デバイス上の写真・動画をグリッド形式で一覧表示します。

- 列数の切り替え（2〜4 列、DataStore で保持）
- 同期状態インジケーター（`PENDING_UPLOAD` / `SYNCED` / `PENDING_DELETE` / `ERROR`）
- プルトゥリフレッシュ

### カメラ

CameraX を使用したアプリ内カメラ機能です。

- 写真撮影（低遅延モード）
- フロント / バックカメラの切り替え
- ピンチズームとタップフォーカス
- 撮影後に MediaStore へ保存し自動アップロード
- ストレージ不足やカメラ利用不可時のエラーハンドリング

### メディア詳細

写真・動画をフルスクリーンで閲覧できます。

- 水平ページャーによるスワイプ切り替え
- 画像のピンチズーム（`ZoomableContainer`）
- Media3 ExoPlayer による動画再生
- ツールバーの表示 / 非表示切り替え

### バックグラウンド同期

WorkManager を使ったオフラインファーストのアップロード・削除キューです。

- `UploadMediaWorker` — 未アップロードレコードを S3 にアップロードし、API と同期
- `DeleteMediaWorker` — 削除予約済みアイテムをリモートから削除
- ネットワーク接続制約付きの一回限りワーク（重複排除あり）
- Room でアップロードレコード（`UploadRecordEntity`）を永続管理

### プッシュ通知

Firebase Cloud Messaging（FCM）によるプッシュ通知を受信します。

## 技術スタック

| カテゴリ | ライブラリ / バージョン |
|---|---|
| 言語 | Kotlin 2.3 |
| UI | Jetpack Compose BOM 2026.02 + Material 3 |
| アーキテクチャ | Clean Architecture（domain / data / ui） |
| DI | Hilt 2.59 + KSP |
| ナビゲーション | Navigation Compose 2.9 |
| カメラ | CameraX 1.5 |
| 画像読み込み | Coil 3.4（Compose 連携） |
| 動画再生 | Media3 ExoPlayer 1.9 |
| 認証 | AWS Amplify Cognito 2.33 |
| ストレージ | AWS Amplify S3 2.33 |
| 分析 | Firebase Analytics（BOM 34.9） |
| プッシュ通知 | Firebase Cloud Messaging |
| バックグラウンド処理 | WorkManager 2.11 |
| ローカル DB | Room 2.8 |
| 設定保存 | DataStore Preferences 1.2 |
| Lint | ktlint 14.2 + detekt 2.0-alpha |
| テスト | JUnit 5 + MockK 1.14 + kotlinx-coroutines-test |
| Min SDK | 29（Android 10） |
| Compile SDK | 36 |
| JDK | 17 |

## 画面構成

```
スプラッシュ（セッション確認）
 ├─ セッション有効 → メディアグリッド（ホーム）
 └─ セッション無効 → ログイン
                      ├─ サインアップ → メール認証
                      └─ パスワード忘れ → リセット

メディアグリッド（ホーム）
 ├─ メディアタップ → メディア詳細（画像 / 動画）
 ├─ FAB → カメラ
 └─ サインアウト → ログイン
```

## モジュール構成

```
app/                          # Activity・NavGraph・DI ブートストラップ・Amplify / Firebase 初期化
core/
  common/                     # 横断的な抽象化（Clock など）
  data/                       # Amplify REST クライアント・FCM デバイストークン管理
  ui/                         # Material 3 テーマ・カラー・共通コンポーネント・文字列リソース
feature/
  auth/
    domain/                   # ユースケース・リポジトリ IF・値オブジェクト（Email, Password, UserId, JwtToken, ClientId）
    data/                     # Amplify Cognito 連携・認証エラーマッパー
    ui/                       # Login / VerificationCode / ForgotPassword / ResetPassword 画面・ViewModel
  media/
    domain/                   # メディアモデル（Media, MediaType）・SyncStatus・アップロード/削除ユースケース
    data/                     # S3 アップロード・Room DB（UploadRecordDao）・Worker・Scheduler
    ui/                       # MediaScreen / CameraScreen / MediaDetailScreen・CameraPreviewManager・ViewModel
  settings/
    domain/                   # GridColumnCount 値オブジェクト・設定ユースケース
    data/                     # DataStore によるグリッド列数の永続化
build-logic/                  # 共有 Gradle コンベンションプラグイン（cloudphotos.lint 等）
```

## セットアップ

### 前提条件

- Android Studio Hedgehog 以降
- JDK 17
- Ruby 4.0 + Bundler 4.0（Fastlane 用）

### ローカル設定

サンプルファイルをコピーして値を設定してください。

```bash
cp local.properties.sample local.properties
```

```properties
sdk.dir=/path/to/android/sdk

# Dev 環境
DEV_API_BASE_URL=https://your-dev-api.example.com/
DEV_COGNITO_CLIENT_ID=your_dev_cognito_client_id
DEV_S3_BUCKET_NAME=your-dev-bucket

# Prod 環境
PROD_API_BASE_URL=https://your-api.example.com/
PROD_COGNITO_CLIENT_ID=your_prod_cognito_client_id
PROD_S3_BUCKET_NAME=your-prod-bucket
```

> **`local.properties` は絶対にコミットしないでください。** `.gitignore` に登録済みです。

### ビルド

```bash
# Dev デバッグ APK
./gradlew assembleDevDebug

# Dev リリース APK（Fastlane 経由）
bundle exec fastlane build_dev

# Prod リリース APK + AAB（Fastlane 経由）
bundle exec fastlane build_prod
```

### ビルドフレーバー

| フレーバー | 用途 | applicationId サフィックス |
|---|---|---|
| `dev` | 開発・テスト環境 | `.dev` |
| `prod` | 本番環境 | なし |

各フレーバーは `local.properties` の `DEV_*` / `PROD_*` プレフィックス付きプロパティを参照します。

## 開発

### ブランチ運用

- フィーチャー開発は `develop` から分岐してください。
- `main`・`develop` への直接プッシュは禁止です。

### Lint

```bash
./gradlew ktlintFormat   # 自動修正（ローカルのみ）
./gradlew ktlintCheck detekt
```

### テスト

```bash
# 全モジュール
./gradlew test

# 単一モジュール
./gradlew :feature:auth:domain:test
./gradlew :feature:auth:data:test
./gradlew :feature:auth:ui:test
./gradlew :feature:media:domain:test
./gradlew :feature:media:data:test
./gradlew :feature:media:ui:test
./gradlew :feature:settings:domain:test
./gradlew :feature:settings:data:test

# Fastlane 経由（CI と同一環境）
bundle exec fastlane test
```

テストは **JUnit 5 + MockK + kotlinx-coroutines-test** を使用し、Arrange / Act / Assert 構造に従います。

### CI（継続的インテグレーション）

`main`・`develop` へのプッシュおよびすべての PR で GitHub Actions が実行されます。

| ステップ | コマンド | 内容 |
|---|---|---|
| Lint | `bundle exec fastlane lint` | ktlint + detekt |
| Test | `bundle exec fastlane test` | dev フレーバーのユニットテスト |

CI がパスしない PR はマージできません。フォーク PR ではダミーシークレットが自動設定されます。

### CD（継続的デリバリー）

`main` へのプッシュで Prod ビルド、`develop` へのプッシュで Dev ビルドが実行されます。
ビルド成果物（APK / AAB）は GitHub Actions のアーティファクトとしてアップロードされます。

## アーキテクチャ

本プロジェクトは Clean Architecture を採用し、各フィーチャーを `domain` / `data` / `ui` の 3 レイヤーに分割しています。

- **domain** — フレームワーク非依存。ユースケース・リポジトリインターフェース・値オブジェクト・ビジネスルール
- **data** — SDK / DB / ネットワーク等のフレームワーク実装。エラーマッピングはこのレイヤーで行う
- **ui** — Jetpack Compose による宣言的 UI。ViewModel が状態を管理し、ユースケースを呼び出す

依存方向は `ui → domain ← data` で、逆方向の依存は禁止です。

詳細は以下のドキュメントを参照してください。

- [`docs/architecture-decisions.md`](docs/architecture-decisions.md) — アーキテクチャ上の意思決定と理由
- [`docs/media-upload-flow.md`](docs/media-upload-flow.md) — アップロード / 削除フローと SyncStatus 遷移
- [`docs/error-handling-guide.md`](docs/error-handling-guide.md) — CancellationException とエラーマッピングのパターン
- [`docs/forbidden-patterns.md`](docs/forbidden-patterns.md) — 禁止パターンとその理由
- [`AGENTS.md`](AGENTS.md) — 開発ルール（人間・AI エージェント共通）

## セキュリティ

脆弱性の報告手順については [`SECURITY.md`](SECURITY.md) を参照してください。