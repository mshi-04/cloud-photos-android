---
name: domain-implementation
description: 業務ルール、入力制約、値の妥当性、UseCase、domain model、Result / Error 型、SyncStatus、Repository / Scheduler / Writer の公開 contract を変更する依頼で使う。Cloud Photos の Domain 層（feature:*:domain、core:common）を担当する。Compose 画面の見た目や SDK / Room 実装詳細だけの変更は対象外。
---

# 目的

Cloud Photos の業務ルール、入力検証、状態遷移、およびレイヤー間の公開 contract を、Android Framework や外部 SDK に依存しない Domain 層（`feature:auth:domain`、`feature:media:domain`、`core:common`）へ閉じる。

# 条件別の参照先

全技術文書を一律に読まず、変更内容に該当する文書・節だけを確認する。

- UseCase の作成基準・省略条件、Repository / Scheduler / Writer interface の分割方針を確認するとき: [`docs/architecture.md`](../../../docs/architecture.md) の「UseCase と Domain Abstraction」「Repository / DataSource / Mapper」
- Result 型と例外伝播の使い分け、Kotlin 実装規約を確認するとき: [`docs/implementation-guidelines.md`](../../../docs/implementation-guidelines.md) の「エラー処理と CancellationException」「Kotlin」
- 認証フロー、確認コード、パスワード再設定、アカウント列挙を避ける `AuthError` 分類に関わるとき: [`docs/authentication.md`](../../../docs/authentication.md)
- アップロード・削除・リモート同期の調整（pending 保護、900 件分割取得、未アップロード削除分岐、`SyncStatus` の業務上の意味）に関わるとき: [`docs/media-sync.md`](../../../docs/media-sync.md)
- `SyncStatus` の追加・リネームに伴う DB 保存文字列との互換性を確認するとき: [`docs/data-model.md`](../../../docs/data-model.md)
- 対象モジュールの実装とテスト:
  - [`core/common/src/main/`](../../../core/common/src/main/)
  - [`feature/auth/domain/src/main/`](../../../feature/auth/domain/src/main/) / [`feature/auth/domain/src/test/`](../../../feature/auth/domain/src/test/)
  - [`feature/media/domain/src/main/`](../../../feature/media/domain/src/main/) / [`feature/media/domain/src/test/`](../../../feature/media/domain/src/test/)

# 役割クラスと参照先

変更対象の役割を見分け、該当する reference だけを読んでから実装する。

| 役割 | 参照 |
| --- | --- |
| UseCase / Request | [`references/usecase.md`](references/usecase.md) |
| Value object | [`references/value-object.md`](references/value-object.md) |
| Result / Error / `SyncStatus` | [`references/result-error.md`](references/result-error.md) |
| Repository / Scheduler / Writer interface、単純な domain model | reference なし。[`docs/architecture.md`](../../../docs/architecture.md) の「UseCase と Domain Abstraction」「Repository / DataSource / Mapper」に従う |

# 手順

1. 変更対象の役割を判定し、該当する reference と必要な技術文書の節だけを読む。
2. 既存の contract で足りるか、新しい UseCase / value object / interface が必要かを [`docs/architecture.md`](../../../docs/architecture.md) の基準に沿って決める。
3. reference の実装時の注意と波及先に沿って実装する。
4. 追加・変更した value object、UseCase、Result 拡張などの振る舞いを [`test-implementation`](../test-implementation/SKILL.md)（および [`docs/testing.md`](../../../docs/testing.md)）に沿って JVM Unit Test で検証する。
5. 変更範囲に応じたローカル検証を [`docs/development-setup.md`](../../../docs/development-setup.md) の「ローカル検証の選び方とコマンド」から選んで実行する。
6. 変更内容、実行した検証と結果、未実施の検証と理由を簡潔に報告する。

# 注意事項

- `feature:*:domain` は `core:common` 以外のモジュールや Android Framework（`Context`、`android.net.Uri`）、Room、DataStore、Amplify SDK に依存させない。
- Domain の contract や `AuthError` / `UploadError` / `SyncStatus` の変更が UI、Data、DB へ波及するときは、[`ui-implementation`](../ui-implementation/SKILL.md)、[`data-implementation`](../data-implementation/SKILL.md)、[`db-implementation`](../db-implementation/SKILL.md) を併用する。
