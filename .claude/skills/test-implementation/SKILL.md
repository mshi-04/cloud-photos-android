---
name: test-implementation
description: テスト追加・修正・レビューに使う。JUnit 5、MockK、kotlinx-coroutines-test、ViewModel、UseCase、Repository、Mapper、Worker、Room DAO、Flow/coroutine テスト、androidTest の InstrumentedTest、検証範囲判断で使用する。
---

# Test Implementation

## 実装手順

1. 変更対象のレイヤーと実行すべき Gradle タスクを決める。
2. `src/test` と `src/androidTest` のどちらに置くか決める。端末 API がないと確認できないもの（Room DAO、DataStore、WorkManager の enqueue 契約）だけ `src/androidTest` に置き、JUnit 4 と実体で書く。それ以外は `src/test` に置く。
3. 対象 production class の既存テストを探して形を合わせる。`src/test` は JUnit 5、MockK、`kotlinx-coroutines-test`、`src/androidTest` は JUnit 4 と実体を使い、どちらも Arrange / Act / Assert に従う。近い既存テストがない場合は、参考資料の該当種別を読んでから書き始める。
4. テスト名は `src/test` では `` `[対象関数名] [期待結果] when [条件]` ``、`src/androidTest` では同じ意味の snake_case にし、期待結果が state、effect、戻り値、例外、呼び出し、無視のどれか分かる名前にする。
5. UI/ViewModel では UseCase 呼び出しだけで終わらせず、画面契約として見える state/effect を assert する。
6. Domain では value object の正規化・境界値・不正値、UseCase の success/error/validation/cancellation を必要な範囲で追加する。
7. Data では Repository/DataSource/Mapper の境界を mock し、provider model/Entity から domain contract への変換を assert する。
8. Worker では `Result.success()`、`Result.retry()`、`Result.failure()` と状態更新、呼び出し順序、永続/一時エラーの分岐を確認する。
9. Coroutine/Flow テストでは `runTest` を使い、必要な仮想時間操作を明示する。

## 注意点

- `src/androidTest` は `AndroidJUnitRunner` 上で動くため JUnit 4 を使う。JUnit 5 の annotation を書いてもテストとして実行されない。
- UseCase が呼ばれたことだけを検証すると、state、effect、戻り値が変わっても気づけない。画面や domain の契約として見える結果を assert する。
- `CancellationException` の扱いは `rethrows` で確認しないと、握り潰しがテストを通過する。
- Turbine で最終値だけを見ると、emit の回数と順序の誤りを見逃す。`awaitItem()` で 1 件ずつ確認する。
- Mapper、value object、domain model まで mock すると、変換の誤りがテストに映らない。境界の依存だけ mock する。
- sleep や実時間待ちは CI の負荷で不安定になる。仮想時間を進める。

## 検証手順

1. 変更種別ごとの Gradle タスクを [docs/verification-policy.md](../../../docs/verification-policy.md) に従って実行する。
2. テストだけを変更した場合も、対象モジュールの test task を実行する。
3. `src/androidTest` を変更した場合は `./gradlew :feature:media:data:connectedDebugAndroidTest` を実行する。エミュレータや実機がない場合は CI の `instrumented-test` job に委ねたことを報告する。
4. 実行しなかった検証があれば、理由と残るリスクを報告する。

## 参考資料

このリポジトリでの書き方。対象の種別だけ読む。

- [references/viewmodel-test.md](references/viewmodel-test.md): ViewModel テスト
- [references/usecase-test.md](references/usecase-test.md): UseCase テスト
- [references/repository-test.md](references/repository-test.md): Repository 実装テスト
- [references/mapper-test.md](references/mapper-test.md): Mapper テスト
- [references/valueobject-test.md](references/valueobject-test.md): value object テスト
- [references/worker-test.md](references/worker-test.md): Worker テスト
- [references/fixture.md](references/fixture.md): テスト fixture
- [references/instrumented-test.md](references/instrumented-test.md): InstrumentedTest

規約:

- [docs/testing-conventions.md](../../../docs/testing-conventions.md)
- [docs/verification-policy.md](../../../docs/verification-policy.md)
