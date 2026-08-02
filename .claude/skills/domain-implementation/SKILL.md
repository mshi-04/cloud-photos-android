---
name: domain-implementation
description: Domain 層の設計・実装・修正に使う。UseCase、Repository interface、domain model、value object、domain result/error、純粋 validation、feature domain モジュールの変更で使用する。
---

# Domain Implementation

## 実装手順

1. 変更する概念が UseCase、Repository interface、domain model、value object、domain result/error のどれかを決める。
2. 既存の `feature:<name>:domain` 内で同種の class を探し、package、命名、constructor injection、`operator fun invoke`、request/model/valueobject の形を合わせる。同種が見つからない場合は、参考資料の該当種別を読む。
3. UseCase を追加する場合は、複数 Repository の調整、再利用操作、validation、状態遷移など、名前付き操作として残す価値がある処理だけにする。
4. Repository が必要な場合は interface を domain に追加し、実装は data 側で扱う。provider 型、Entity、DTO、Android framework 型が必要になったら data 側の作業に切り替える。
5. Value object を追加・修正する場合は、既存の `of(raw: ...)` パターンに合わせて正規化と validation を閉じ込める。
6. `AuthError`、`UploadError`、`SyncStatus`、Result 型などの domain contract を変えた場合は、data mapper、UI state/effect、テスト、関連 docs の追随箇所を洗い出す。

## 注意点

- domain に Android framework、Room、WorkManager、Amplify/Cognito の型が入ると、JVM だけでテストできなくなり、provider の差し替えが domain まで波及する。
- Repository メソッドを機械的に UseCase でラップすると、名前が増えるだけで domain の意味は増えない。
- value object を生プリミティブへ戻すと、検証済みであるという保証がその時点で失われる。
- `AuthError`、`UploadError`、`SyncStatus`、Result 型は data mapper と UI state が対で依存している。片側だけ変えると、DB 文字列や画面表示との対応がずれる。
- suspend を main-safe にしないと、呼び出し側が Dispatcher の詳細を知る必要が出る。

## 検証手順

1. UseCase、value object、domain model の対象テストを追加・更新する。
2. 単一 domain モジュール変更は `./gradlew :feature:<name>:domain:test` を実行する。
3. domain contract が data/ui に波及する場合は、触れた各レイヤーのテストも実行する。

## 参考資料

このリポジトリでの書き方。対象の種別だけ読む。

- [references/usecase.md](references/usecase.md): UseCase の形と、新設する価値がある基準
- [references/repository-interface.md](references/repository-interface.md): Repository interface の分割単位とシグネチャ
- [references/valueobject.md](references/valueobject.md): value object の `of()` と validation の書き方
- [references/result-error.md](references/result-error.md): Result 型、error 型、変更時の波及先

規約:

- [docs/implementation-rules.md](../../../docs/implementation-rules.md)
- [docs/architecture-decisions.md](../../../docs/architecture-decisions.md)
- [docs/error-handling-guide.md](../../../docs/error-handling-guide.md)
- [docs/testing-conventions.md](../../../docs/testing-conventions.md)
