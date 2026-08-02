# CLAUDE.md

このリポジトリのAI作業ルールです。詳細は [docs/](docs/) の該当文書を参照してください。
ここは作業時の地図として役割をします。

## 主要文書

- [docs/implementation-rules.md](docs/implementation-rules.md): 配置、依存方向、UseCase/Repository/値オブジェクト、コメント
- [docs/android-conventions.md](docs/android-conventions.md): Compose、ViewModel、coroutines、Hilt、Navigation、WorkManager
- [docs/testing-conventions.md](docs/testing-conventions.md): テスト命名、AAA、coroutines/Flow/ViewModel/Workerテスト、InstrumentedTest、Room migration
- [docs/verification-policy.md](docs/verification-policy.md): 変更種別ごとの検証スコープ
- [docs/build-environment.md](docs/build-environment.md): Gradle、flavor、環境値、依存追加
- [docs/error-handling-guide.md](docs/error-handling-guide.md): CancellationException、Result、provider error mapping
- [docs/media-upload-flow.md](docs/media-upload-flow.md): mediaのアップロード、削除、同期、SyncStatus
- [docs/architecture-decisions.md](docs/architecture-decisions.md): 現在の構造を選んでいる理由

## AI skills

作業内容に応じて [.claude/skills/](.claude/skills/) の該当スキルを読む。

- [ui-implementation](.claude/skills/ui-implementation/SKILL.md): UI 層の設計・実装・修正
- [domain-implementation](.claude/skills/domain-implementation/SKILL.md): Domain 層の設計・実装・修正
- [data-implementation](.claude/skills/data-implementation/SKILL.md): Data 層の設計・実装・修正
- [db-implementation](.claude/skills/db-implementation/SKILL.md): Room DB の設計・実装・修正
- [auth-implementation](.claude/skills/auth-implementation/SKILL.md): 認証機能の設計・実装・修正
- [test-implementation](.claude/skills/test-implementation/SKILL.md): テストの追加・修正・レビュー
- [implementation-review](.claude/skills/implementation-review/SKILL.md): 実装差分のレビュー
- [ci-build-troubleshooting](.claude/skills/ci-build-troubleshooting/SKILL.md): build / CI / 静的解析の調査・修正
- [media-sync-implementation](.claude/skills/media-sync-implementation/SKILL.md): media 同期・Worker・SyncStatus の設計・実装・修正

## 開発フロー

- フィーチャー開発は `develop` から分岐する。`main` と `develop` へ直接 push しない。
- PR の説明は [.github/pull_request_template.md](.github/pull_request_template.md) に従う。
- 背景、トレードオフ、採用しなかった案は PR の説明か [docs/](docs/) へ書き、コードへ残さない。
- 実行した検証と実行しなかった検証を報告する。判断基準は [docs/verification-policy.md](docs/verification-policy.md)。
