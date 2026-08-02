# Repository interface

`feature/<name>/domain/.../repository/` に interface だけを置き、実装は `feature/<name>/data/` 側に持つ。

## 分割の粒度

auth は `AuthRepository` ひとつに集約している。provider が Cognito 単一で、保存先による分割の理由がないため。`signUp` / `confirmSignUp` / `signIn` / `signOut` / `fetchCurrentUser` / `getSession` / `resendSignUpCode` / `resetPassword` / `confirmResetPassword` / `deleteUser` の 10 メソッドがすべて `suspend` で `AuthResult<T>` を返す。

media は保存先と役割ごとに 8 つへ分けている。

- `LocalMediaRepository` — 端末の media 一覧
- `LocalUploadRecordsRepository` — Room 上の upload record の取得・保存・削除
- `RemoteUploadRecordsRepository` — API 側の upload record の取得・作成・削除・完了通知
- `UploadRepository` — 実体ファイルのアップロード
- `UploadScheduler` / `DeleteScheduler` — 非同期処理の起動
- `SettingsRepository` — 設定値の読み書き
- `CapturedPhotoWriter` — 撮影 JPEG の保存

分けてあることで、UseCase が必要な依存だけを取れる。`SyncUploadRecordsUseCase` は remote と local の両方を、`DeleteMediaUseCase` は local と scheduler だけを取る。

## シグネチャの型

`LocalMediaRepository` は同じデータに `getMediaListFlow(): Flow<List<Media>>` と `suspend fun getMediaList(): List<Media>` の 2 入口を持つ。継続監視する UI 向けが `Flow`、Worker や UseCase の 1 回きりの読み出しが suspend。`SettingsRepository` のように `val gridColumnCount: Flow<GridColumnCount>` とプロパティで公開する形もある。

`UploadScheduler.scheduleUpload()` と `DeleteScheduler.scheduleDelete()` は `suspend` を付けない。実装が WorkManager への enqueue で、待つ対象がないため。「投げたら返る」ことを型で表している。

戻り値の使い分けは、provider の失敗を呼び出し側に選ばせる操作だけ `AuthResult<T>` / `UploadResult<T>` で包み、ローカル DB 操作は生の値や `Unit` を返す。`LocalUploadRecordsRepository` は Result 型を使わない。

引数が 2 つ以上なら `request/` の data class を受ける。`createUploadRecord` が `CreateUploadRecordRequest` を、`uploadMedia` が `UploadMediaRequest` を取る形。

## 型の制約

シグネチャに出てよいのは domain の model / value object / request / Result 型と `Flow` のみ。`Uri`、`Bitmap`、Room の Entity、`ListenableWorker`、Amplify の型が必要になった時点で、それは data 側の DataSource の仕事になる。

domain モジュールの gradle 依存は `javax.inject` と `kotlinx-coroutines-core` だけ（media はこれに `core:common` が加わる）。Android SDK が入っていないので、framework 型を書けばコンパイルが通らない。この制約が domain を JVM テストだけで検証できる状態に保っている。

- 依存方向: `../../../../docs/implementation-rules.md`
