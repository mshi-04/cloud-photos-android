# ビルドと環境

Gradle、flavor、環境値、依存追加に関するルールです。

## 方針

- Kotlin DSLとVersion Catalogを使う。
- 共有設定は `build-logic` のconvention pluginへ寄せる。
- 単一モジュールだけの依存は、そのモジュールに閉じる。
- ビルド構造に関係しない作業で `build-logic` を触らない。
- 依存追加は理由、スコープ、検証範囲を報告する。

## Flavor必須値

各flavorは以下の値を必要とします。

- `COGNITO_CLIENT_ID`
- `API_BASE_URL`
- `S3_BUCKET_NAME`

実際のキー:

- `DEV_COGNITO_CLIENT_ID`
- `DEV_API_BASE_URL`
- `DEV_S3_BUCKET_NAME`
- `PROD_COGNITO_CLIENT_ID`
- `PROD_API_BASE_URL`
- `PROD_S3_BUCKET_NAME`

## 解決順序

1. `local.properties`
2. Gradle project property (`-PKEY=value`)
3. `ORG_GRADLE_PROJECT_*` 環境変数

例:

```properties
DEV_COGNITO_CLIENT_ID=xxxxx
DEV_API_BASE_URL=https://dev.example.com/
DEV_S3_BUCKET_NAME=my-dev-bucket
PROD_COGNITO_CLIENT_ID=yyyyy
PROD_API_BASE_URL=https://api.example.com/
PROD_S3_BUCKET_NAME=my-prod-bucket
```

`local.properties` はコミットしません。

CIで渡す例:

```bash
./gradlew assembleDevDebug -PDEV_COGNITO_CLIENT_ID=xxxxx
```

または:

```text
ORG_GRADLE_PROJECT_DEV_COGNITO_CLIENT_ID=xxxxx
```

## 禁止

- Kotlinソースへsecret、endpoint、client id、bucket名を直書きする。
- 実環境値や `local.properties` をコミットする。
- flavor差分をソースコードの条件分岐へ散らす。
- 単一フィーチャー用途の依存を全体conventionへ入れる。
- 既存conventionの意図を確認せず整理目的で `build-logic` を書き換える。

## 依存追加チェック

依存を追加する前に確認します。

1. 既存依存や標準APIで足りないか。
2. runtime依存かtest依存か。
3. 追加先は最小モジュールか。
4. KSP、Hilt、Room、Compose、AGP設定変更が必要か。
5. ライセンス、バイナリサイズ、メンテナンス状況に問題がないか。
6. 検証スコープは [docs/verification-policy.md](verification-policy.md) に合っているか。

## テスト / カバレッジ依存

- UnitTestはJUnit 5を維持し、MockK、Turbine、`kotlinx-coroutines-test` はJupiter上の部品として追加する。
- 依存バージョンは `gradle/libs.versions.toml` に集約する。
- Flow、StateFlow、SharedFlow検証が必要なモジュールには `testImplementation(libs.turbine)` を追加する。
- KoverはKotlin公式のGradle pluginを使い、カバレッジXMLはPRコメント投稿用にCIで生成する。
- GitHub ActionsでPRコメントを投稿するworkflowには `pull-requests: write` 権限を付ける。
- カバレッジ値はPR上の可視化に使う。閾値でCIを失敗させる場合は、別途方針を決めてから設定する。

## build-logic

触ってよい例:

- 複数モジュール共通のAndroid/Kotlin設定。
- lint、test、Compose、Hilt、Room、KSPの共通設定。
- モジュール追加に伴う既存conventionの拡張。

避ける例:

- 単一モジュールだけの依存。
- 一時的な回避設定。
- ついでの大規模整理。

## 検証

Gradle、依存、build-logicを変更した場合:

```bash
./gradlew test
./gradlew ktlintCheck detekt
```

variantやflavorに影響する場合:

```bash
./gradlew assembleDevDebug
```
