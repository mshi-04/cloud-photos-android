---
name: data-implementation
description: Data 層の設計・実装・修正に使う。Repository 実装、DataSource、Mapper、DTO、SDK/API client、Amplify REST/S3、DataStore、WorkManager 連携、feature data モジュールの変更で使用する。
---

# Data Implementation

## 実装手順

1. 変更対象を Repository 実装、DataSource、Mapper、DTO、SDK/API client、DataStore、Worker/Scheduler に分ける。
2. 既存の `feature:<name>:data` で同じ責務の class を確認し、Repository 実装から DataSource を呼び、Mapper で domain contract へ戻す流れに合わせる。同種が見つからない場合は、参考資料の該当種別を読む。
3. SDK/API/Room/DataStore から得た model や例外を domain/UI へ渡さず、DataSource または Mapper で domain model/result/error に変換する。
4. 例外処理を追加・変更する場合は、`CancellationException` を再スローする経路を先に作ってから provider error mapping を実装する。
5. Repository 実装に画面都合の分岐や business rule が入りそうな場合は、domain UseCase に移すか、既存 UseCase の変更に切り分ける。
6. Room/DAO/Entity を触る場合は `db-implementation`、Worker/Scheduler/SyncStatus を触る場合は `media-sync-implementation`、認証 data を触る場合は `auth-implementation` も併用する。

## 注意点

- provider の model や例外を domain/UI へ渡すと、SDK の差し替えや API 変更が UI まで波及する。mapper 境界で閉じる。
- `runCatching` は `CancellationException` も捕まえる。再スローしないと、キャンセル済みの処理が続行する。
- Repository 実装に画面都合の分岐が入ると、別の画面から同じ Repository を使えなくなる。
- 同じ変換を Repository と DataSource へ分散させると、片方だけ直された状態が生まれる。
- DTO/Entity を domain model として使い回すと、外部フォーマットの変更が domain を壊す。

## 検証手順

1. Repository 実装は DataSource 呼び出し、Mapper 適用、domain result をテストする。
2. Mapper は DTO/Entity 変換と provider 例外 mapping をテストする。
3. 単一 data モジュール変更は `./gradlew :feature:<name>:data:test` を実行する。

## 参考資料

このリポジトリでの書き方。対象の種別だけ読む。

- [references/repository-impl.md](references/repository-impl.md): Repository 実装が持つ責任と持たない責任
- [references/datasource.md](references/datasource.md): DataSource の interface 規約と SDK callback の橋渡し
- [references/mapper.md](references/mapper.md): Mapper の形、provider 例外の変換、部分失敗の扱い

規約:

- [docs/implementation-rules.md](../../../docs/implementation-rules.md)
- [docs/error-handling-guide.md](../../../docs/error-handling-guide.md)
- [docs/android-conventions.md](../../../docs/android-conventions.md)
- [docs/architecture-decisions.md](../../../docs/architecture-decisions.md): provider を data に閉じ込めている理由
