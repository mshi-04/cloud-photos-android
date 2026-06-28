---
name: domain-implementation
description: Domain 層の設計・実装・修正に使う。UseCase、Repository interface、domain model、value object、domain result/error、純粋 validation、feature domain モジュールの変更で使用する。
---

# Domain Implementation

## 実装手順

1. 変更する概念が UseCase、Repository interface、domain model、value object、domain result/error のどれかを決める。
2. 既存の `feature:<name>:domain` 内で同種の class を探し、package、命名、constructor injection、`operator fun invoke`、request/model/valueobject の形を合わせる。
3. UseCase を追加する場合は、複数 Repository の調整、再利用操作、validation、状態遷移など、名前付き操作として残す価値がある処理だけにする。
4. Repository が必要な場合は interface を domain に追加し、実装は data 側で扱う。provider 型、Entity、DTO、Android framework 型が必要になったら data 側の作業に切り替える。
5. Value object を追加・修正する場合は、既存の `of(raw: ...)` パターンに合わせて正規化と validation を閉じ込める。
6. `AuthError`、`UploadError`、`SyncStatus`、Result 型などの domain contract を変えた場合は、data mapper、UI state/effect、テスト、関連 docs の追随箇所を洗い出す。

## 検証手順

1. UseCase、value object、domain model の対象テストを追加・更新する。
2. 単一 domain モジュール変更は `./gradlew :feature:<name>:domain:test` を実行する。
3. domain contract が data/ui に波及する場合は、触れた各レイヤーのテストも実行する。

## 参考資料

- [docs/implementation-rules.md](../../../docs/implementation-rules.md)
- [docs/architecture-decisions.md](../../../docs/architecture-decisions.md)
- [docs/error-handling-guide.md](../../../docs/error-handling-guide.md)
- [docs/testing-conventions.md](../../../docs/testing-conventions.md)
