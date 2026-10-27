---
name: data-implementation
description: Repository 実装、DataSource、SDK / REST API 呼び出し、例外・JSON mapper、DataStore 設定永続化、MediaStore I/O、WorkManager の Worker / Scheduler を変更する依頼で使う。Cloud Photos の Data 層（core:data、feature:*:data）および app モジュールの Worker・DI 配線・FCM token 連携を担当する。画面表示のみの変更や Room のテーブル構造・SQL のみの変更は対象外。
---

# 目的

Cloud Photos の Data 層（`core:data`、`feature:auth:data`、`feature:media:data`）および `app` のバックグラウンド処理・DI 配線で、domain の contract を外部 SDK（Amplify Auth / Storage / API、Firebase Messaging）、Room、DataStore、MediaStore、WorkManager に接続し、実装詳細を UI / Domain へ漏らさない。

# 条件別の参照先

全技術文書を一律に読まず、変更内容に該当する文書・節だけを確認する。

- Repository / DataSource / Worker の責務分担、`app` の DI module 構成を確認するとき: [`docs/architecture.md`](../../../docs/architecture.md) の「Repository / DataSource / Mapper」「Worker と Scheduler の分離」「`:app`（Composition Root）」
- `CancellationException` の再スロー、coroutines / Dispatcher、Hilt、WorkManager、ログの規約を確認するとき: [`docs/implementation-guidelines.md`](../../../docs/implementation-guidelines.md) の該当節
- 認証・サインアウト・退会の処理順、provider 例外の分類、FCM device token の登録・解除（`RegisterDeviceTokenWorker`、`FcmTokenRegistrar`、`DeviceTokenDataSource`）に関わるとき: [`docs/authentication.md`](../../../docs/authentication.md)
- メディアのアップロード・削除・リモート同期の処理順、永続失敗と一時失敗（retry）の分類、`completeUpload()`、WorkManager 契約、通知（`UploadNotificationHelper` / FCM `upload_completed`）に関わるとき: [`docs/media-sync.md`](../../../docs/media-sync.md)
- `upload_records` の保存値や `SyncStatus` の復元に関わるとき: [`docs/data-model.md`](../../../docs/data-model.md)
- 対象モジュールの実装とテスト:
  - [`core/data/src/main/`](../../../core/data/src/main/) / [`core/data/src/test/`](../../../core/data/src/test/)
  - [`feature/auth/data/src/main/`](../../../feature/auth/data/src/main/) / [`feature/auth/data/src/test/`](../../../feature/auth/data/src/test/)
  - [`feature/media/data/src/main/`](../../../feature/media/data/src/main/) / [`feature/media/data/src/test/`](../../../feature/media/data/src/test/) / [`feature/media/data/src/androidTest/`](../../../feature/media/data/src/androidTest/)
  - [`app/src/main/kotlin/com/appvoyager/cloudphotos/`](../../../app/src/main/kotlin/com/appvoyager/cloudphotos/) / [`app/src/test/`](../../../app/src/test/)

# 役割クラスと参照先

変更対象の役割を見分け、該当する reference だけを読んでから実装する。複数に跨る場合は関連する reference を併読する。

| 役割 | 参照 |
| --- | --- |
| Repository 実装 | [`references/repository-implementation.md`](references/repository-implementation.md) |
| DataSource / 具体 I/O（`MediaStoreCapturedPhotoWriter`・`ContentTypeResolver`・`DeviceTokenDataSource` を含む） | [`references/datasource.md`](references/datasource.md) |
| Mapper（例外・enum・JSON・Entity 変換） | [`references/mapper.md`](references/mapper.md) |
| Worker（`UploadMediaWorker`・`DeleteMediaWorker`・`RegisterDeviceTokenWorker`） | [`references/worker.md`](references/worker.md) |
| Scheduler（`UploadSchedulerImpl`・`DeleteSchedulerImpl`） | [`references/scheduler.md`](references/scheduler.md) |
| Hilt DI module の単純な binding 追加 | [`docs/architecture.md`](../../../docs/architecture.md) の「`:app`（Composition Root）」と [`docs/implementation-guidelines.md`](../../../docs/implementation-guidelines.md) の「Hilt / DI」に従う |

# 手順

1. 変更対象の役割を判定し、該当する reference と必要な技術文書の節だけを読む。
2. domain interface の契約と、provider / SDK / OS API 側の制約（キャンセル可否、スレッド、例外型）を分けて確認する。
3. reference の実装時の注意と波及先に沿って実装し、新規実装クラスを追加した場合は `app/src/main/kotlin/com/appvoyager/cloudphotos/di/` の対応する Hilt module の binding も確認する。
4. 変更した振る舞いに応じて [`test-implementation`](../test-implementation/SKILL.md)（および [`docs/testing.md`](../../../docs/testing.md)）に沿って JVM Unit Test または `androidTest` を追加・更新する。
5. 変更範囲に応じたローカル検証を [`docs/development-setup.md`](../../../docs/development-setup.md) の「ローカル検証の選び方とコマンド」から選んで実行する。
6. 変更内容、実行した検証と結果、未実施の検証と理由を簡潔に報告する。

# 注意事項

- Amplify SDK、Firebase、Room Entity、HTTP ステータスコードなどの詳細を domain / UI の型シグネチャへ漏らさない。
- Room の Entity 定義、DAO SQL、DB version / migration を変更する場合は [`db-implementation`](../db-implementation/SKILL.md) を併用する。
- domain の interface、`AuthError` / `UploadError`、`SyncStatus` を変更する場合は [`domain-implementation`](../domain-implementation/SKILL.md) を併用する。
