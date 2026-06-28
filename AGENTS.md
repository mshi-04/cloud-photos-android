# AGENTS.md

このリポジトリのAI作業ルールです。詳細は [docs/](docs/) の該当文書を参照してください。
ここは作業時の地図として役割をします。

## 主要文書

- [docs/implementation-rules.md](docs/implementation-rules.md): 配置、依存方向、UseCase/Repository/値オブジェクト
- [docs/android-conventions.md](docs/android-conventions.md): Compose、ViewModel、coroutines、Hilt、Navigation、WorkManager
- [docs/testing-conventions.md](docs/testing-conventions.md): テスト命名、AAA、coroutines/Flow/ViewModel/Workerテスト
- [docs/verification-policy.md](docs/verification-policy.md): 変更種別ごとの検証スコープ
- [docs/build-environment.md](docs/build-environment.md): Gradle、flavor、環境値、依存追加
- [docs/error-handling-guide.md](docs/error-handling-guide.md): CancellationException、Result、provider error mapping
- [docs/media-upload-flow.md](docs/media-upload-flow.md): mediaのアップロード、削除、同期、SyncStatus
- [docs/architecture-decisions.md](docs/architecture-decisions.md): 現在の構造を選んでいる理由

## AI skills

作業内容に応じて [.agents/skills/](.agents/skills/) の該当 `SKILL.md` を読む。

- [ui-implementation](.agents/skills/ui-implementation/SKILL.md): UI 層の設計・実装・修正
- [domain-implementation](.agents/skills/domain-implementation/SKILL.md): Domain 層の設計・実装・修正
- [data-implementation](.agents/skills/data-implementation/SKILL.md): Data 層の設計・実装・修正
- [db-implementation](.agents/skills/db-implementation/SKILL.md): Room DB の設計・実装・修正
- [auth-implementation](.agents/skills/auth-implementation/SKILL.md): 認証機能の設計・実装・修正
- [test-implementation](.agents/skills/test-implementation/SKILL.md): テストの追加・修正・レビュー
- [implementation-review](.agents/skills/implementation-review/SKILL.md): 実装差分のレビュー
- [ci-build-troubleshooting](.agents/skills/ci-build-troubleshooting/SKILL.md): build / CI / 静的解析の調査・修正
- [media-sync-implementation](.agents/skills/media-sync-implementation/SKILL.md): media 同期・Worker・SyncStatus の設計・実装・修正
