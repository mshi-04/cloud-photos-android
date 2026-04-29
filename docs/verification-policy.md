# 検証ポリシー

このドキュメントはいつ・どの程度の検証が必要かを定義します。
目標は、検証不足（実際の問題を見逃す）と過剰検証（単一行の変更ごとにGradleシンクとフルテストスイートを実行する）の両方を避けることです。

## 原則

変更によって引き起こされるエラーのクラスを実際にキャッチできる最小スコープを実行する。
変更がモジュールまたはレイヤー境界を越える場合のみスコープを拡大する。

## Gradleシンク

Gradleシンクはソースのみの変更の後に**必要ありません**。
以下を変更した場合のみGradleシンクを実行すること：

- `build.gradle.kts` ファイル
- `libs.versions.toml`
- `settings.gradle.kts`
- `build-logic` コンベンションプラグイン
- モジュールの追加または削除

既存モジュール内のKotlinソースの変更に対してシンクは不要です。

## 開発中

変更内容をカバーする最小スコープを実行する：

```bash
# 単一モジュール（開発中は推奨）
./gradlew :feature:<name>:<layer>:test

# 素早いlintチェック
./gradlew ktlintCheck
```

毎回のイテレーションでフルテストスイートを実行しない — CIが最終ゲートであり、ローカル開発ループではない。

## PRを開く前

変更したすべてのモジュールに対してlintとテストを実行する：

```bash
# まずフォーマットを自動修正
./gradlew ktlintFormat

# lintゲート
./gradlew ktlintCheck detekt

# テスト — スコープは変更内容による（下記参照）
```

### 変更タイプ別テストスコープ

| 変更タイプ                                          | テストコマンド                                                                                    |
|-----------------------------------------------------|---------------------------------------------------------------------------------------------------|
| 単一フィーチャーレイヤー（`feature:<name>:<layer>`）| `./gradlew :feature:<name>:<layer>:test`                                                          |
| 一つのフィーチャー内の複数レイヤー                  | `./gradlew :feature:<name>:domain:test :feature:<name>:data:test :feature:<name>:ui:test`         |
| `core:*` モジュール                                 | `./gradlew test`（全モジュール）                                                                   |
| `app` 配線、ナビゲーション、トップレベルDI          | `./gradlew test`                                                                                  |
| Gradle / `build-logic` / 依存関係の変更             | `./gradlew test`                                                                                  |
| ドキュメントのみ                                    | テスト実行不要                                                                                    |

スコープに迷った場合は `./gradlew test` を実行する。

## マージ前

CIが最終ゲートです。CIが赤の場合はマージしないこと。

CIはlintチェック（`bundle exec fastlane lint` → `ktlintCheck detekt`）と
DEV Debugバリアントのユニットテスト（`bundle exec fastlane test` → `testDevDebugUnitTest`）を実行します。

ローカルでのパスはCIでのパスの代替にはなりません。
PRがオープンされた後にCIが失敗した場合は、マージ前に調査して修正してください。

## モジュールテストターゲットリファレンス

```bash
./gradlew :feature:auth:domain:test
./gradlew :feature:auth:data:test
./gradlew :feature:auth:ui:test
./gradlew :feature:media:domain:test
./gradlew :feature:media:data:test
./gradlew :feature:media:ui:test
```

## レポート

テストを実行しなかった場合は、レポートに明示的に記載すること：

```
実行しなかったテスト: <コマンド> — 理由: <なぜ>
```

これを省略することは許容されません。CIがゲートであることはレポートをスキップする理由になりません。
