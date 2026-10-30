# CLAUDE.md

Cloud Photos で Claude Code が最初に読む入口です。
ここには作業時の地図と基本前提だけを書き、技術的な方針・契約の詳細は `docs/` 配下の文書を正本とします。
通常作業で全技術文書・全 Skill を読む必要はありません。変更する振る舞いと役割に応じて必要な参照先だけを選んでください。

## 技術文書（`docs/`）

- [`docs/project-overview.md`](docs/project-overview.md): プロジェクト概要 / 主要機能と実装の入口 / モジュール見取り図 / 技術スタックの確認先
- [`docs/architecture.md`](docs/architecture.md): モジュール境界 / 依存方向 / `app`（Composition Root） / UseCase・Repository・DataSource・Worker の責務と採用理由
- [`docs/implementation-guidelines.md`](docs/implementation-guidelines.md): Kotlin / Compose / ViewModel / Coroutines / Hilt / Resource / コメント / ログ / エラー処理の基本方針（該当する節だけを参照）
- [`docs/data-model.md`](docs/data-model.md): Room `upload_records` / 保存値と domain 値の対応 / DAO の契約 / DB 互換性と migration 判断
- [`docs/authentication.md`](docs/authentication.md): 認証セッション / 確認コード / パスワード再設定 / サインアウト・退会 / FCM デバイストークン登録・解除 / アカウント列挙を避けるエラー表現
- [`docs/media-sync.md`](docs/media-sync.md): アップロード・削除・リモート同期 / `SyncStatus` 状態遷移 / 処理順 / 部分失敗と retry / WorkManager 契約 / 通知
- [`docs/development-setup.md`](docs/development-setup.md): SDK・JDK・flavor・環境値 / 依存追加時の確認 / ローカル検証の選び方とコマンド
- [`docs/testing.md`](docs/testing.md): JVM Unit Test と Instrumented Test の振り分け / 命名・AAA / JUnit・MockK・Flow・ViewModel・Worker・DB テストの書き方
- [`docs/review.md`](docs/review.md): 差分レビューの比較範囲 / 確認観点 / 指摘分類と根拠 / 終了時の報告形式
- [`docs/ci.md`](docs/ci.md): CI / CD の実行条件 / job 構成 / カバレッジ / CodeQL / Fastlane / 失敗箇所の切り分け

## AI 作業用 Skill

変更するレイヤー・役割・作業目的に応じて、必要な Skill を確認します。
Claude Code 向け Skill は `.claude/skills/` で管理します。Codex 向けの `.agents/skills/` とは自動同期しないため、手動で整合を保ちます。

- `ui-implementation`: 画面の表示、操作、遷移、表示文言、UI state、Effect を変更するときに使う
  - [`.claude/skills/ui-implementation/SKILL.md`](.claude/skills/ui-implementation/SKILL.md)
- `domain-implementation`: 業務ルール、入力制約、UseCase、domain model、Result / Error、公開 contract を変更するときに使う
  - [`.claude/skills/domain-implementation/SKILL.md`](.claude/skills/domain-implementation/SKILL.md)
- `data-implementation`: Repository 実装、DataSource、SDK / API、mapper、DataStore、MediaStore、Worker / Scheduler を変更するときに使う
  - [`.claude/skills/data-implementation/SKILL.md`](.claude/skills/data-implementation/SKILL.md)
- `db-implementation`: Room の保存構造、DAO query、保存値の互換性、DB version / migration を変更するときに使う
  - [`.claude/skills/db-implementation/SKILL.md`](.claude/skills/db-implementation/SKILL.md)
- `test-implementation`: テストコード（JVM Unit Test / Instrumented Test）を追加・修正するときに使う
  - [`.claude/skills/test-implementation/SKILL.md`](.claude/skills/test-implementation/SKILL.md)
- `implementation-review`: 実装差分をレビューするときに使う
  - [`.claude/skills/implementation-review/SKILL.md`](.claude/skills/implementation-review/SKILL.md)

## 前提と開発・作業ルール

- **プロジェクトとモジュール構成の前提**: Cloud Photos は端末内の写真・動画をクラウド（AWS S3 / API Gateway / Cognito）へバックアップ・同期する Android アプリ。`app`（Composition Root）、`core:common` / `core:data` / `core:ui`、`feature:auth:{domain,data,ui}`、`feature:media:{domain,data,ui}` のマルチモジュール構成を採り、各機能は `ui -> domain <- data` の依存方向を守る。
- **ブランチと PR**: フィーチャー開発は `develop` から分岐する。`main` と `develop` へ直接 push しない。PR の説明は [`.github/pull_request_template.md`](.github/pull_request_template.md) に従う。背景、トレードオフ、採用しなかった案は PR の説明か `docs/` に書き、コードコメントへ残さない。
- **検証の報告**: 作業完了時は、実行した検証（コマンドと結果）と未実施の検証（理由を含む）を必ず区別して報告する。ローカル検証の選択基準は [`docs/development-setup.md`](docs/development-setup.md) に従う。
- **スコープと現状の尊重**: ユーザーの依頼範囲を維持し、依頼外の仕様変更や全面書き換えを混ぜない。Room schema export JSON など、現時点で未導入の仕組みを実装済みとして扱わない。
- **AI 文書の二重管理**: `CLAUDE.md` と `AGENTS.md`、`.claude/skills/` と `.agents/skills/`、および `docs/` 内の AI 向け文書を変更する際は、両クライアントの対応箇所を確認して手動で反映する。
- **指示の優先順位**: ユーザーの明示指示と Skill・技術文書が衝突した場合はユーザー指示を優先し、衝突した Skill・文書の箇所を報告する。
- **自律判断と確認**: 可逆で影響の小さい操作は確認せずに合理的に推測して進める。操作の影響と既に承認された範囲に基づいて判断し、不可逆な操作に必要な承認がない場合は確認する。ユーザーが依頼した push や PR 作成など、既に承認された操作の再確認は不要とする。
