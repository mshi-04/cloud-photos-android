---
name: test-implementation
description: テストコードを追加・修正する依頼で使う。Cloud Photos の JVM Unit Test（src/test、JUnit 5）と Instrumented Test（src/androidTest、JUnit 4）の設計・実装を担当する。既存テストの実行のみや、実装差分のレビューのみの依頼は対象外。
---

# 目的

Cloud Photos の変更した振る舞いや対象クラスの契約を、既存のテスト規約（[`docs/testing.md`](../../../docs/testing.md)）に沿って検証するテストコードとして追加・修正する。

# 最初に確認するもの

- テスト規約の正本: [`docs/testing.md`](../../../docs/testing.md)（JVM / `androidTest` の振り分け、命名規約、AAA、fixture、ViewModel / Worker / DAO / DataStore テストの書き方）
- 対象クラスの実装と、同じモジュールの既存テスト（`src/test/` または `src/androidTest/`）および `testutil/` の fixture
- ローカル検証の選択と実行コマンド: [`docs/development-setup.md`](../../../docs/development-setup.md) の「ローカル検証の選び方とコマンド」

# 手順

1. 検証したい振る舞いを洗い出し、[`docs/testing.md`](../../../docs/testing.md) の「テストスタックと配置の振り分け」に従って `src/test`（JUnit 5）か `src/androidTest`（JUnit 4）かを決める。
2. 対象モジュールの既存テストと `testutil/` の fixture 命名（domain の value object は `validXxx`・request/model は suffix なし、data は `xxxFixture`、単一テストファイル内ヘルパーは `private fun createXxx`）を確認する。
3. [`docs/testing.md`](../../../docs/testing.md) の共通規約（命名、`// Arrange` / `// Act` / `// Assert`、1 テスト 1 シナリオ、Dispatcher 統一）と対象別の要点（ViewModel の private Snapshot 比較、Worker の `TestListenableWorkerBuilder`、DAO の in-memory DB など）に沿ってテストを実装する。
4. [`docs/development-setup.md`](../../../docs/development-setup.md) に従って対象モジュールのテストタスクを実行し、結果を検証する。
5. 追加・修正したテストケース、実行した検証と結果、未実施の検証（実機/エミュレータ未接続時の `connectedDebugAndroidTest` など）と理由を簡潔に報告する。

# 注意事項

- テストの書式・命名・アサーション規約の詳細は [`docs/testing.md`](../../../docs/testing.md) を正本とし、この Skill 内に重複定義しない。
- `src/test`（JUnit Jupiter）と `src/androidTest`（JUnit 4）でテストフレームワークと関数命名規則（backtick + 許可動詞 vs `snake_case`）が異なるため混同しない。
- アプリ本体コードの変更に付随してテストを書く場合は、対応する実装 Skill（[`ui-implementation`](../ui-implementation/SKILL.md) / [`domain-implementation`](../domain-implementation/SKILL.md) / [`data-implementation`](../data-implementation/SKILL.md) / [`db-implementation`](../db-implementation/SKILL.md)）と併用する。
- 実装差分のレビュー依頼には [`implementation-review`](../implementation-review/SKILL.md) を使う。
