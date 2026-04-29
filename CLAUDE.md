# CloudPhotos (`com.appvoyager.cloudphotos`)

## 主要な情報源

`AGENTS.md` がリポジトリ全体の開発ルールの情報源です。
このリポジトリで作業する際は、まず `AGENTS.md` を読んで従ってください。
このファイルと `AGENTS.md` が重複・矛盾する場合は `AGENTS.md` を優先します。

## このファイルの目的

意図的に軽量なファイルです。
`AGENTS.md` をClaude向けのエントリーガイダンスとリポジトリの概要で補足します。
Claude固有の理由がない限り、詳細なアーキテクチャポリシーをここに重複させないでください。

## スタック

- Kotlin
- Jetpack Compose（Material 3）
- クリーンアーキテクチャ
- Hilt + KSP
- AWS Amplify（Cognito）
- Firebase（Analytics、FCM）
- 最小SDK 29 / コンパイルSDK 36 / Java 17
- フレーバー：`dev`、`prod`

## スキル

- `.agent/skills/android-clean-arch/SKILL.md`
- `.agent/skills/android-composable/SKILL.md`
- `.agent/skills/android-auth-error/SKILL.md`
- `.agent/skills/android-testing/SKILL.md`
- `.agent/skills/android-media-upload/SKILL.md`

## サブエージェント

- `.claude/agents/reviewer.md` — 読み取り専用コードレビュアー（アーキテクチャ、セキュリティ、規約）
- `.claude/agents/test-writer.md` — ユニットテスト生成（JUnit 5 + MockKパターン）
- `.claude/agents/arch-checker.md` — アーキテクチャバリデータ（モジュール境界、依存方向）

## Claudeクイックスタート

変更を行う前に：

1. `AGENTS.md` を読む。
2. 影響を受ける最小のモジュールを特定する。
3. 同じフィーチャー内にすでに存在するパターンを再利用する。
4. 変更をローカルに保ち、暗黙のリファクタリングを避ける。
5. 最小限の関連テストスコープを実行し、何が変わったかをレポートする。

## リポジトリ固有のリマインダー

- UseCaseスタイル：単一責任の `suspend operator fun invoke()`。
- リポジトリ実装はデータソースへの委譲のみ行い、ビジネスロジックを持たない。
- エラーマッピングはマッパーオブジェクト経由でdata層に属する。
- `runCatching` フローでは `CancellationException` を必ず再スローする。
- ドメインモデルは、バリデーション済み概念に対して生プリミティブより値オブジェクトを優先する。
- Composeスクリーンは宣言的に保ち、ステートはViewModel/UIステートクラスで所有する。
- UI文字列は `stringResource()` を使用する。
- テストはJUnit 5 + MockK + `kotlinx-coroutines-test` を使用する。

## テストのエントリポイント

ローカル検証には最小限の関連Gradleコマンドを使用する（例：`./gradlew :feature:<name>:<layer>:test`）。
CI準拠の検証（DEV/Debugビルドのユニットテスト）には `bundle exec fastlane test` を使用する。
このコマンドはCIと同期した検証にスコープされており、すべてのモジュールを対象とするわけではない。
`./gradlew test` は各モジュールのビルドとテストを担当し（より広いモジュールレベルのカバレッジ）、
`bundle exec fastlane test` はCI同期チェック、`./gradlew test` は包括的なモジュール全体テストに使用する。
CIゲートを含む総合的な検証ポリシーは `docs/verification-policy.md` を参照。
