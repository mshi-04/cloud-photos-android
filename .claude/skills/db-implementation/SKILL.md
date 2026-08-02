---
name: db-implementation
description: Room DB の設計・実装・修正に使う。CloudPhotosDatabase、DAO、Entity、Room query、migration、UploadRecordEntity、SyncStatus と DB 文字列契約を触る作業で使用する。
---

# DB Implementation

## 実装手順

1. DB 変更が Entity、DAO query、Database、DI、migration のどれに当たるか分ける。
2. 既存の `CloudPhotosDatabase`、`UploadRecordEntity`、`UploadRecordDao`、Entity mapper、Repository/DataSource 呼び出し元を確認し、DB の変更点と domain model への変換点を特定する。判断に迷う場合は、参考資料の該当種別を読む。
3. Entity を変える場合は、mapper、DAO query、Repository/DataSource の入出力、既存テスト fixture を同時に更新する。
4. DAO query を変える場合は、呼び出し元の期待順序、空結果、複数件、状態フィルタ、Room の引数制限に影響がないか確認してから実装する。
5. `SyncStatus` の文字列を query に含める場合は、domain enum 名、DAO query、ガードテストを同じ変更単位で扱う。
6. DB version を上げる変更では、`DatabaseModule` の `Room.databaseBuilder()` が destructive migration のフォールバックを持たないことを前提に、既存データを保持するか破棄するかを先に決める。schema export と migration test の要否は [docs/testing-conventions.md](../../../docs/testing-conventions.md) の Room migration に従う。
7. DB provider や DAO provider を変える場合は、`DatabaseModule` と Hilt scope、database name、既存利用箇所を追う。

## 注意点

- Entity の列や制約を変えて DB version を据え置くと、Room の schema 検証が実行時に失敗する。
- `DatabaseModule` の `Room.databaseBuilder()` は destructive migration のフォールバックを持たない。version を上げて migration を与えないと、既存インストールが起動時に落ちる。
- DAO query は `SyncStatus` の文字列を直接持っている。enum 名を変えると、query は静かにマッチしなくなる。
- Entity を変えると mapper、DAO query、テスト fixture が同時にずれる。まとめて追う。
- 空の IN 句や存在しない `mediaId` は結果 0 件で返る。呼び出し側が要素の存在を前提にしていると壊れる。

## 検証手順

1. DAO、Entity、mapper の変更は `./gradlew :feature:media:data:test` を実行する。
2. DAO query や Entity の契約を変えた場合は `./gradlew :feature:media:data:connectedDebugAndroidTest` を実行する。エミュレータや実機がない場合は CI の `instrumented-test` job に委ねたことを報告する。
3. migration を追加した場合は `MigrationTestHelper` を使う migration test を追加するか、追加しない理由を報告する。
4. Worker/Scheduler/SyncStatus に波及する場合は `media-sync-implementation` の検証手順も実行する。

## 参考資料

このリポジトリでの書き方。対象の種別だけ読む。

- [references/entity.md](references/entity.md): Entity の列定義と、変更時に同時にずれるもの
- [references/dao.md](references/dao.md): DAO の形、SQL に埋まった enum 名、空結果の契約
- [references/entity-mapper.md](references/entity-mapper.md): Entity と domain model の変換
- [references/database.md](references/database.md): Database 定義、DI、version を上げる前の判断

規約:

- [docs/implementation-rules.md](../../../docs/implementation-rules.md)
- [docs/android-conventions.md](../../../docs/android-conventions.md)
- [docs/testing-conventions.md](../../../docs/testing-conventions.md)
- [docs/verification-policy.md](../../../docs/verification-policy.md)
- [docs/media-upload-flow.md](../../../docs/media-upload-flow.md)
