# CI

この文書は GitHub Actions の workflow（CI / CD / CodeQL）の実行条件、job 構成、権限、カバレッジ投稿、および失敗時の切り分け観点をまとめます。
ローカルでの環境構築と検証コマンドは [`docs/development-setup.md`](development-setup.md) を正本とします。

## Workflow 一覧と実行条件

| Workflow | ファイル | トリガー | 主な役割 |
| --- | --- | --- | --- |
| `CI` | [`.github/workflows/ci.yml`](../.github/workflows/ci.yml) | `pull_request`（`opened`, `synchronize`, `reopened`）、`main` / `develop` への `push` | 静的解析、UnitTest、カバレッジ PR コメント、InstrumentedTest |
| `CD` | [`.github/workflows/cd.yml`](../.github/workflows/cd.yml) | `main` / `develop` への `push` | `develop` は `build_dev`（APK）、`main` は `build_prod`（APK + AAB）をビルドし artifact 保存 |
| `CodeQL` | [`.github/workflows/codeql.yml`](../.github/workflows/codeql.yml) | `main` / `develop` への `push` | `java-kotlin` の静的セキュリティ解析 |

すべての workflow で `concurrency` グループと `cancel-in-progress: true` を設定しており、同一 PR または同一ブランチで新しい実行が始まると進行中の古い実行をキャンセルする。

## `CI` の Job 構成と詳細

`CI` workflow は次の 3 つの job を並列実行する。権限は `contents: read`、`issues: write`、`pull-requests: write`（カバレッジコメント投稿用）を持つ。

### 1. `lint`

- Ruby / Bundler のキャッシュを復元（`main` / `develop` への push 時のみ保存）し、[`fastlane/Fastfile`](../fastlane/Fastfile) の `bundle exec fastlane lint`（内部で `./gradlew ktlintCheck detekt`）を実行する。
- `ktlintCheck` と `detekt` は flavor 必須環境値の検証対象外タスクのため、secret なしで実行される。

### 2. `unit-test`

- フォークからの PR でもビルドできるよう、`secrets.DEV_*` が空の場合はダミー値（`DEV_API_BASE_URL`、`DEV_COGNITO_CLIENT_ID`、`DEV_S3_BUCKET_NAME` と各 `ORG_GRADLE_PROJECT_DEV_*`）を `$GITHUB_ENV` に設定する。
- `bundle exec fastlane test`（内部で `./gradlew testDevDebugUnitTest`）を実行する。
- **Kover カバレッジレポート（`pull_request` イベントのみ）**:
  - 全 10 モジュールの Kover XML 生成タスク（`:app:koverXmlReportDevDebug`、JVM モジュールは `koverXmlReportJvm`、Android ライブラリは `koverXmlReportDebug`）を実行する。
  - `actions/github-script` で各モジュールの `LINE` カバレッジを集計し、マーカー `<!-- unit-test-kover-coverage -->` を使って PR コメントを作成・更新する。
  - カバレッジコメントは UnitTest の可視化目的であり、閾値による CI fail は設定していない。

### 3. `instrumented-test`

- Fastlane を経由せず、ダミー対応済みの `ORG_GRADLE_PROJECT_DEV_*` を設定したうえで、KVM 権限を有効化し `reactivecircus/android-emulator-runner`（API 36、`x86_64`、`google_apis`）上で `./gradlew :feature:media:data:connectedDebugAndroidTest` を実行する。

## `CD` と `CodeQL` の特徴

- **`CD`（`build-and-deploy` job）**:
  - `develop` push 時は `development` environment で `bundle exec fastlane build_dev`（`assembleDevRelease`）、`main` push 時は `production` environment で `bundle exec fastlane build_prod`（`assembleProdRelease` と `bundleProdRelease`）を実行し、生成された APK / AAB を `actions/upload-artifact` で保存する。
- **`CodeQL`（`analyze` job）**:
  - `github/codeql-action/autobuild` でビルドを行う。CodeQL 環境では `CODEQL_ACTION_VERSION` / `CODEQL_DIST` 環境変数が存在するため、[`app/build.gradle.kts`](../app/build.gradle.kts) の `requireEnvProperty` は flavor 環境値が未設定でも空文字を返してビルドを通す。

## CI 失敗時の切り分け手順

1. **最初の失敗箇所を特定する**:
   - Gradle やコンパイルのエラーは後続タスクに波及するため、ログ末尾ではなく最初に失敗した task と原因行を確認する。
2. **ローカルと CI の差分を確認する**:
   - **環境値・Flavor**: ローカルでは `local.properties` で通っていても、CI / CD では `secrets` または `$GITHUB_ENV`（`ORG_GRADLE_PROJECT_*`）経由で解決される。新しい必須プロパティを `app/build.gradle.kts` や `Fastfile` に追加した場合は、`ci.yml` / `cd.yml` と CodeQL 回避分岐の両方を確認する。
   - **Gradle キャッシュ**: PR では `setup-gradle` が `cache-read-only: true` で動く。
   - **Kover XML タスク名**: モジュール追加・変更時は、`ci.yml` の `Generate Unit Test Kover XML Reports` ステップのタスク一覧（JVM モジュールは `koverXmlReportJvm`、Android ライブラリは `koverXmlReportDebug`、`:app` は `koverXmlReportDevDebug`）と一致しているか確認する。
   - **ktlint / detekt**: 整形だけで解消する指摘か、複雑度・例外処理（`@Suppress` が必要なケースを含む）など設計上の修正が必要かを切り分ける。
3. **最小差分で修正し、対応するローカルコマンドで再確認する**:
   - 調査用に入れた一時設定やログは残さず戻す。ローカルで実行できない検証（エミュレータ不在の `instrumented-test` や実 secret が必要な `CD`）は未検証範囲として明示する。
