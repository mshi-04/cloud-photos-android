# 検証ポリシー

変更後に何を確認するかを定義します。

## 原則

- 変更が壊し得る範囲を検証する。
- レイヤーやモジュールを越えた変更は検証範囲を広げる。
- ドキュメントのみの変更ではGradleテストを要求しない。
- 実行しなかった検証は理由と一緒に報告する。

## Gradle Syncが必要な変更

- `build.gradle.kts`
- `settings.gradle.kts`
- `libs.versions.toml`
- `build-logic`
- モジュール追加/削除
- AGP、Kotlin、KSP、Hilt、Compose compilerなどの設定変更

Kotlinソース、resource、文書のみの変更では通常不要です。

## 変更種別ごとの検証

| 変更種別 | 検証 |
|---|---|
| ドキュメントのみ | 文書間参照と整合性確認 |
| 単一domainモジュール | `./gradlew :feature:<name>:domain:test` |
| 単一dataモジュール | `./gradlew :feature:<name>:data:test` |
| 単一uiモジュール | `./gradlew :feature:<name>:ui:test` |
| 同一フィーチャー複数レイヤー | 触れた各レイヤーのtest |
| `app` | `./gradlew test`、必要に応じてassemble |
| `core:*` | `./gradlew test` |
| Gradle / 依存 / build-logic | `./gradlew test`、`ktlintCheck detekt`、Kover XML生成、必要に応じてassemble |
| CI / GitHub Actions | 対象workflowの構文確認、関連Gradleタスク、PRコメント権限と投稿条件の確認 |
| media Worker/Scheduler/SyncStatus | `:feature:media:data:test` と関連domain/uiテスト |

迷った場合は `./gradlew test` を選びます。

## 開発中

```bash
./gradlew :feature:<name>:<layer>:test
./gradlew ktlintCheck
```

毎回フルスイートを走らせる必要はありません。

## PR前

```bash
./gradlew ktlintFormat
./gradlew ktlintCheck detekt
```

その後、変更種別に応じたテストを実行します。

## CI

CIは最終ゲートです。ローカルで通っていても、CIが赤ならマージしません。

主なCI相当:

```bash
bundle exec fastlane lint
bundle exec fastlane test
```

カバレッジCIを変更した場合は、PRコメント用のKover XML生成も確認します。

```bash
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

Koverのカバレッジコメントは可視化目的です。閾値でCIを失敗させる場合は、導入前に方針を明記します。

## 報告形式

```text
実行したテスト/検証: <コマンドまたは確認内容> — <結果>
実行しなかったテスト/検証: <コマンド> — 理由: <理由>
```
