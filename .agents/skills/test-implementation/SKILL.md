---
name: test-implementation
description: テスト追加・修正・レビューに使う。JUnit 5、MockK、kotlinx-coroutines-test、ViewModel、UseCase、Repository、Mapper、Worker、Room DAO、Flow/coroutine テスト、検証範囲判断で使用する。
---

# Test Implementation

## 実装手順

1. 変更対象のレイヤーと実行すべき Gradle タスクを決める。
2. 対象 production class の既存テストを探し、JUnit 5、MockK、`kotlinx-coroutines-test`、Arrange / Act / Assert の形を合わせる。
3. テスト名は `` `[対象関数名] [期待結果] when [条件]` `` の形にし、期待結果が state、effect、戻り値、例外、呼び出し、無視のどれか分かる名前にする。
4. UI/ViewModel では UseCase 呼び出しだけで終わらせず、画面契約として見える state/effect を assert する。
5. Domain では value object の正規化・境界値・不正値、UseCase の success/error/validation/cancellation を必要な範囲で追加する。
6. Data では Repository/DataSource/Mapper の境界を mock し、provider model/Entity から domain contract への変換を assert する。
7. Worker では `Result.success()`、`Result.retry()`、`Result.failure()` と状態更新、呼び出し順序、永続/一時エラーの分岐を確認する。
8. Coroutine/Flow テストでは `runTest` を使い、必要な仮想時間操作を明示する。

## 検証手順

1. 変更種別ごとの Gradle タスクを `docs/verification-policy.md` に従って実行する。
2. テストだけを変更した場合も、対象モジュールの test task を実行する。
3. 実行しなかった検証があれば、理由と残るリスクを報告する。

## 参考資料

- [docs/testing-conventions.md](../../../docs/testing-conventions.md)
- [docs/verification-policy.md](../../../docs/verification-policy.md)
