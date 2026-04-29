# メディアアップロード/削除フロー

このドキュメントは `media` フィーチャーのアップロードと削除のライフサイクルを説明します。
ルールは `AGENTS.md` と `feature/media/AGENTS.md` にあります。このファイルはフロー全体の
コールグラフを追わなくても変更の影響を把握できるよう、フローがどのように機能するかを説明します。

## このドキュメントが存在する理由

`media` フィーチャーはリポジトリ内で最もリスクの高い領域です。
ドメインルール、ローカル永続化、リモート同期、バックグラウンドワーカー、キャンセルロジックが混在しています。
フローを誤解すると、リトライ動作の破損、同期ステータスの不整合、孤立したレコードが生じます。

---

## アップロードフロー

### トリガー（UI → Domain）

```text
MediaViewModel.onScreenResumed()
  ├─ syncRemote()         → SyncUploadRecordsUseCase
  ├─ prepareUploadQueue() → PrepareUploadQueueUseCase
  └─ scheduleUpload()     → ScheduleUploadUseCase
                               └─ UploadScheduler.scheduleUpload()
                                    └─ UploadSchedulerImpl → WorkManager.enqueueUniqueWork(UploadMediaWorker)
```

### ワーカー実行（Data — バックグラウンド）

```text
UploadMediaWorker.doWork()
  ↓
  LocalUploadRecordsRepository.getPendingUploadRecords()   [Room]
  ↓ （各レコードに対して）
  ContentTypeResolver.resolve()
  UploadDataSource.uploadMedia()
    └─ Amplify.Storage.uploadInputStream()                 [S3]
  ↓ （成功時）
  LocalUploadRecordsRepository.saveUploadRecords()         [ローカルDBを更新]
  RemoteUploadRecordsRepository.createUploadRecord()       [POST /media/uploads]
  ↓ （全レコード処理後）
  RemoteUploadRecordsRepository.completeUpload()           [POST /media/uploads/complete]
```

### SyncStatus遷移

```text
（新しいローカルメディア）
  PENDING_UPLOAD
      ↓ アップロード成功 + リモート登録成功
  SYNCED
      ↓ ユーザーが削除
  PENDING_DELETE
      ↓ 削除成功
  （レコード削除）

  PENDING_UPLOAD / PENDING_DELETE
      ↓ 永続的なエラー
  ERROR
```

---

## 削除フロー

### トリガー（UI → Domain）

```text
MediaViewModel.scheduleDelete()
  └─ ScheduleDeleteUseCase
       └─ DeleteScheduler.scheduleDelete()
            └─ DeleteSchedulerImpl → WorkManager.enqueueUniqueWork(DeleteMediaWorker)
```

### ワーカー実行（Data — バックグラウンド）

```text
DeleteMediaWorker.doWork()
  ↓
  LocalUploadRecordsRepository.getPendingDeleteRecords()   [Room]
  ↓ （各レコードに対して）
  RemoteUploadRecordsRepository.deleteUploadRecord()       [DELETE /media/uploads/:id]
  UploadDataSource.deleteUploadedObject()                  [S3削除]
  LocalUploadRecordsRepository.deleteUploadRecord()        [Roomから削除]
```

特殊ケース：`cloudStoragePath` がnullの場合（まだアップロードされていない）、
リモート/S3のステップをスキップしてローカルレコードを直接削除します。

S3孤立の許容：`deleteUploadedObject()` がキャンセル以外のエラーで失敗した場合、
ワーカーはリトライせずに続行し、孤立オブジェクトを受け入れます。

---

## リモート同期フロー

```text
SyncUploadRecordsUseCase
  └─ RemoteUploadRecordsRepository.getUploadRecords()      [GET /media/uploads]
       └─ LocalUploadRecordsRepository.saveUploadRecords() [Roomにupsert]
```

これは他のデバイスやセッションからアップロードされたレコードを取得するために
レジューム時に実行されます。

---

## エラー分類

ワーカーはリトライ動作を決定するために永続的エラーと一時的エラーを区別します。

| エラー種別                               | ワーカーの応答                                              | SyncStatus |
|------------------------------------------|------------------------------------------------------------|------------|
| 永続的（例：HTTP 4xx）                   | リトライなし、クリーンアップして ERROR としてマーク         | `ERROR`    |
| 一時的（例：ネットワーク、HTTP 5xx）     | `hasTemporaryFailure = true` を設定 → `Result.retry()` を返す | 変化なし   |
| CancellationException                    | 即座に再スロー                                              | 変化なし   |

---

## このフローを変更する際に尊重すべき重要な境界

1. **ワーカー/スケジューラ/リポジトリ/データソースの責任を崩さない。**
   各クラスは異なるライフサイクルとテスト可能性プロファイルを持ちます。
   理由は `docs/architecture-decisions.md` を参照。

2. **SyncStatus遷移の整合性を維持する。**
   新しいステータスを追加したり遷移を変更する場合は、ワーカー、リポジトリ、UIステートが
   すべて意味について合意していることを確認すること。

3. **すべての `catch` ブロックで `CancellationException` を再スローする。**
   ワーカーはネストされた `runCatching` ブロックを使用します。各ブロックは独立して再スローしなければなりません。
   `docs/error-handling-guide.md` を参照。

4. **リモート登録とローカルDB更新は同期を保たなければならない。**
   リモート登録が失敗した場合、ローカルレコードを成功を示す状態に放置してはなりません。

5. **`completeUpload()` は各レコードごとではなく、ワーカー実行ごとに1回呼び出される。**
   バッチ完了を通知します。レコードごとのループ内に移動しないこと。
