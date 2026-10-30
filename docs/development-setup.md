# Development Setup

この文書は SDK / JDK / flavor / 環境値の設定、依存追加や `build-logic` 変更時の判断、およびローカル検証の選び方とコマンドをまとめます。
CI / CD の実行条件と workflow 構成は [`docs/ci.md`](ci.md) を、テストの書き方は [`docs/testing.md`](testing.md) を正本とします。

## 前提環境

- **JDK**: 17（`jvmToolchain(17)`）
- **Android SDK**: `minSdk = 29`、`compileSdk = 36`、`targetSdk = 36`
- **Ruby / Bundler**: Fastlane を実行する場合は [`ci.yml`](../.github/workflows/ci.yml) / [`Gemfile.lock`](../Gemfile.lock) と整合する Ruby（4.0 系）と Bundler を使用する

## ビルドフレーバーと必須環境値

`environment` 次元に `dev`（applicationId サフィックス `.dev`）と `prod`（サフィックスなし）の 2 つの flavor がある。
各 flavor のビルドには次の 3 つのプロパティが必要となる。

- `DEV_COGNITO_CLIENT_ID` / `PROD_COGNITO_CLIENT_ID`
- `DEV_API_BASE_URL` / `PROD_API_BASE_URL`
- `DEV_S3_BUCKET_NAME` / `PROD_S3_BUCKET_NAME`

### 解決順序と検証条件

ルート [`build.gradle.kts`](../build.gradle.kts) と [`app/build.gradle.kts`](../app/build.gradle.kts) により、次の優先順位（上ほど優先）で解決される。

1. Gradle project property（`-PDEV_API_BASE_URL=...`）
2. `ORG_GRADLE_PROJECT_*` 環境変数（`ORG_GRADLE_PROJECT_DEV_API_BASE_URL=...`）
3. `local.properties`（[`local.properties.sample`](../local.properties.sample) をコピーして作成。**コミット禁止**）

`local.properties` の値は、`findProperty(key)` が未設定の場合だけ補完する。

`app/build.gradle.kts` の `requireEnvProperty` は、実行タスクが対象 flavor を含む場合（または `assemble` / `build` / `bundle`）に値が未設定だと `GradleException` で失敗させる。ただし、CodeQL 解析時（`CODEQL_ACTION_VERSION` または `CODEQL_DIST` が設定されている場合）と、対象 flavor に関係しないタスク（例: `:feature:auth:domain:test` や `ktlintCheck`）では空文字へフォールバックして通過する。

ソースコードへの secret、エンドポイント、クライアント ID、バケット名の直書きや、flavor 差分を Kotlin コードの条件分岐へ散らすことは禁止する。

## 依存追加と `build-logic`

- **Version Catalog**:
  - 依存ライブラリとプラグインのバージョンは [`gradle/libs.versions.toml`](../gradle/libs.versions.toml) に集約する。
- **依存追加時の確認**:
  1. 既存依存や標準 API で代替できないか。
  2. `implementation` か `testImplementation` / `androidTestImplementation` か。
  3. 追加先が最小限のモジュールになっているか（単一モジュールしか使わない依存を `build-logic` の convention plugin に入れない）。
  4. KSP、Hilt、Room、Compose、AGP の設定追随が必要か（例: Room DB 定義と DAO は `:feature:media:data` が `room.runtime` / `room.ktx` と `ksp(libs.androidx.room.compiler)` を持ち、`:app` は DI 参照用に `room.runtime` のみを持つ）。
- **`build-logic` を変更してよい条件**:
  - 複数モジュール共通の Android / Kotlin / Compose / Hilt / Lint 設定を変更する場合、またはモジュール追加に伴い既存 convention を拡張する場合に限る。ビルド構造と無関係な作業で `build-logic` を触らない。

## ローカル検証の選び方とコマンド

変更が壊し得る範囲に応じて検証を選び、レイヤーやモジュールを跨ぐ変更では範囲を広げる。
文書や AI Skill のみの変更では Gradle テストを要求しない。

### 変更種別ごとの検証目安

| 変更対象 | 実行する検証 |
| --- | --- |
| 文書・Skill のみ | 文書間リンク、参照先パス、`git diff --check` |
| 単一モジュールの実装・テスト | `./gradlew :<module>:test`（例: `./gradlew :feature:auth:domain:test`） |
| 同一フィーチャーの複数レイヤー | 触れた各レイヤーの `test` タスク |
| `:app` または `:core:*` | `./gradlew test`、必要に応じて `./gradlew assembleDevDebug` |
| メディア Worker / Scheduler / `SyncStatus` | `./gradlew :feature:media:data:test` と関連する `:domain:test` / `:ui:test` |
| Room DAO / Entity、DataStore、WorkManager enqueue 契約 | 上記に加えて `./gradlew :feature:media:data:connectedDebugAndroidTest` |
| Gradle 設定、依存追加、`build-logic` | `./gradlew test`、`./gradlew ktlintCheck detekt`、必要に応じて `./gradlew assembleDevDebug` と Kover XML 生成 |

迷った場合は `./gradlew test` を選ぶ。

### 主なコマンド

```bash
# 静的解析（整形とチェック）
./gradlew ktlintFormat
./gradlew ktlintCheck detekt

# 単一モジュールの UnitTest
./gradlew :core:common:test
./gradlew :core:data:test
./gradlew :core:ui:test
./gradlew :feature:auth:domain:test
./gradlew :feature:auth:data:test
./gradlew :feature:auth:ui:test
./gradlew :feature:media:domain:test
./gradlew :feature:media:data:test
./gradlew :feature:media:ui:test
./gradlew :app:test

# 全モジュールの UnitTest
./gradlew test

# CI 相当の Fastlane 実行（DEV 環境値が必要）
bundle exec fastlane lint
bundle exec fastlane test

# InstrumentedTest（エミュレータまたは実機が必要）
./gradlew :feature:media:data:connectedDebugAndroidTest

# PR カバレッジ用の Kover XML 生成（カバレッジ設定や Gradle 構成変更時）
./gradlew :app:koverXmlReportDevDebug \
  :core:common:koverXmlReportJvm \
  :core:data:koverXmlReportDebug \
  :core:ui:koverXmlReportDebug \
  :feature:auth:domain:koverXmlReportJvm \
  :feature:auth:data:koverXmlReportDebug \
  :feature:auth:ui:koverXmlReportDebug \
  :feature:media:domain:koverXmlReportJvm \
  :feature:media:data:koverXmlReportDebug \
  :feature:media:ui:koverXmlReportDebug
```

## 検証結果の報告

作業完了時は、実行した検証と未実施の検証（エミュレータ不在で CI の `instrumented-test` job に委ねた場合や、環境値未設定でスキップした場合など）を理由とともに報告する。

```text
実行したテスト/検証: <コマンドまたは確認内容> — <結果>
実行しなかったテスト/検証: <コマンド> — 理由: <理由>
```
