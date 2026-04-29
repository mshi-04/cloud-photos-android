# AGENTS.md

このリポジトリのAI作業ルールです。詳細は `docs/` の該当文書を参照してください。

## 基本ルール

- commit、push、branch作成、PR作成、merge、rebaseは、ユーザーが明示的に依頼した場合だけ行う。

## 主要文書

- `docs/implementation-rules.md`: 配置、依存方向、UseCase/Repository/値オブジェクト
- `docs/android-conventions.md`: Compose、ViewModel、coroutines、Hilt、Navigation、WorkManager
- `docs/testing-conventions.md`: テスト命名、AAA、coroutines/Flow/ViewModel/Workerテスト
- `docs/verification-policy.md`: 変更種別ごとの検証スコープ
- `docs/build-environment.md`: Gradle、flavor、環境値、依存追加
- `docs/error-handling-guide.md`: CancellationException、Result、provider error mapping
- `docs/media-upload-flow.md`: mediaのアップロード、削除、同期、SyncStatus
- `docs/architecture-decisions.md`: 現在の構造を選んでいる理由

## 構成

- `app`: 起動、トップレベルNavigation、アプリ全体DI、Manifest、flavor
- `core:*`: 複数フィーチャーで共有する契約、data infrastructure、UI資産
- `feature:<name>:domain`: UseCase、Repository interface、domain model、value object
- `feature:<name>:data`: Repository実装、DataSource、Mapper、Room、DataStore、Worker、SDK統合
- `feature:<name>:ui`: ViewModel、UI state/effect/event、Compose screen/component
- `build-logic`: 共有Gradle convention plugin

## Git

コミット前は `ktlintCheck detekt` と関連テストを通し、コミットメッセージは日本語で短く書きます。

## 報告

変更した場合は、変更したモジュール/文書、配置理由、概要、実行した検証、実行しなかった検証と理由、既知の制限を報告します。
`media` のWorker、Scheduler、SyncStatus、アップロード/削除フローに触れた場合は、retry、状態遷移、整合性への影響も明記します。
