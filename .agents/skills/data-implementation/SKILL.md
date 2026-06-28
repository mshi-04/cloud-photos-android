---
name: data-implementation
description: Data 層の設計・実装・修正に使う。Repository 実装、DataSource、Mapper、DTO、SDK/API client、Amplify REST/S3、DataStore、WorkManager 連携、feature data モジュールの変更で使用する。
---

# Data Implementation

## 実装手順

1. 変更対象を Repository 実装、DataSource、Mapper、DTO、SDK/API client、DataStore、Worker/Scheduler に分ける。
2. 既存の `feature:<name>:data` で同じ責務の class を確認し、Repository 実装から DataSource を呼び、Mapper で domain contract へ戻す流れに合わせる。
3. SDK/API/Room/DataStore から得た model や例外を domain/UI へ渡さず、DataSource または Mapper で domain model/result/error に変換する。
4. 例外処理を追加・変更する場合は、`CancellationException` を再スローする経路を先に作ってから provider error mapping を実装する。
5. Repository 実装に画面都合の分岐や business rule が入りそうな場合は、domain UseCase に移すか、既存 UseCase の変更に切り分ける。
6. Room/DAO/Entity を触る場合は `db-implementation`、Worker/Scheduler/SyncStatus を触る場合は `media-sync-implementation`、認証 data を触る場合は `auth-implementation` も併用する。

## 検証手順

1. Repository 実装は DataSource 呼び出し、Mapper 適用、domain result をテストする。
2. Mapper は DTO/Entity 変換と provider 例外 mapping をテストする。
3. 単一 data モジュール変更は `./gradlew :feature:<name>:data:test` を実行する。

## 参考資料

- [docs/implementation-rules.md](../../../docs/implementation-rules.md)
- [docs/error-handling-guide.md](../../../docs/error-handling-guide.md)
- [docs/android-conventions.md](../../../docs/android-conventions.md)
