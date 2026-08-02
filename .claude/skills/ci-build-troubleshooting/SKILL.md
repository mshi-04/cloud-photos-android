---
name: ci-build-troubleshooting
description: build、CI、Gradle、ktlint、detekt、KSP、Hilt、Room、Compose compiler、Fastlane、GitHub Actions の失敗調査・修正に使う。ビルド設定、依存追加、flavor 環境値、静的解析エラー、テスト実行エラーを扱う作業で使用する。
---

# CI Build Troubleshooting

## 調査手順

1. 失敗したコマンド、task、variant/flavor、CI job、エラーログの最初の原因箇所を特定する。
2. 直近差分を確認し、Gradle 設定、Version Catalog、build-logic、KSP/Hilt/Room/Compose、source、test、環境値のどれが原因候補か分ける。
3. flavor 値が関係する場合は、`local.properties`、Gradle property、`ORG_GRADLE_PROJECT_*`、CodeQL 例外経路のどこで解決されるか確認する。
4. 依存追加や plugin 設定が関係する場合は、追加先が最小モジュールか、runtime/test scope が正しいか、KSP や convention plugin の追随が必要か確認する。
5. ktlint/detekt では、まず指摘箇所を読み、機械的整形で済むか、設計上の修正が必要かを分ける。
6. 原因を絞ったら最小差分で修正し、同じ失敗コマンドから再実行する。

## 注意点

- ログの末尾ではなく最初の原因箇所を読む。後続のエラーは波及であることが多い。
- flavor 値は `local.properties`、Gradle property、`ORG_GRADLE_PROJECT_*` の順で解決される。ローカルで通っても、CI に値がなければ同じビルドが失敗する。
- 単一モジュールの依存を convention plugin へ入れると、無関係なモジュールのビルドまで変わる。
- ktlint/detekt の指摘を整形だけで消すと、指摘の元になった設計上の問題が残る。
- 環境値や secret がなく実行できなかった検証は、通ったことにせず未検証として報告する。
- 調査のために追加した設定やログは、原因を特定した時点で最小差分へ戻す。

## 検証手順

1. Gradle、依存、build-logic 変更は `./gradlew test` と `./gradlew ktlintCheck detekt` を実行する。
2. flavor/variant に影響する場合は `./gradlew assembleDevDebug` を検討する。
3. CI 相当の確認が必要なら `bundle exec fastlane lint` と `bundle exec fastlane test` を実行する。
4. 環境値や secret がなく実行できない検証は、必要な値と未検証リスクを報告する。

## 参考資料

- [docs/build-environment.md](../../../docs/build-environment.md)
- [docs/verification-policy.md](../../../docs/verification-policy.md)
