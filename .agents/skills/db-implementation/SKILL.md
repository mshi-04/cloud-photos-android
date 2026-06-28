---
name: db-implementation
description: Room DB の設計・実装・修正に使う。CloudPhotosDatabase、DAO、Entity、Room query、migration、UploadRecordEntity、SyncStatus と DB 文字列契約を触る作業で使用する。
---

# DB Implementation

## 実装手順

1. DB 変更が Entity、DAO query、Database、DI、migration のどれに当たるか分ける。
2. 既存の `CloudPhotosDatabase`、`UploadRecordEntity`、`UploadRecordDao`、Entity mapper、Repository/DataSource 呼び出し元を確認し、DB の変更点と domain model への変換点を特定する。
3. Entity を変える場合は、mapper、DAO query、Repository/DataSource の入出力、既存テスト fixture を同時に更新する。
4. DAO query を変える場合は、呼び出し元の期待順序、空結果、複数件、状態フィルタ、Room の引数制限に影響がないか確認してから実装する。
5. `SyncStatus` の文字列を query に含める場合は、domain enum 名、DAO query、ガードテストを同じ変更単位で扱う。
6. DB version や migration が必要な変更では、既存データ互換、schema export 方針、Room migration test の要否を整理してから実装する。
7. DB provider や DAO provider を変える場合は、`DatabaseModule` と Hilt scope、database name、既存利用箇所を追う。

## 検証手順

1. DAO、Entity、mapper の変更は `./gradlew :feature:media:data:test` を実行する。
2. migration を追加した場合は Room migration test を追加するか、実行しない理由を報告する。
3. Worker/Scheduler/SyncStatus に波及する場合は `media-sync-implementation` の検証手順も実行する。

## 参考資料

- [docs/implementation-rules.md](../../../docs/implementation-rules.md)
- [docs/android-conventions.md](../../../docs/android-conventions.md)
- [docs/verification-policy.md](../../../docs/verification-policy.md)
- [docs/media-upload-flow.md](../../../docs/media-upload-flow.md)
