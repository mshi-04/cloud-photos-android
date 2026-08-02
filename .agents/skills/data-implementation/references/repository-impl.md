# Repository 実装

`feature/<name>/data/src/main/kotlin/.../data/<name>/repository/` に置く `XxxRepositoryImpl` の説明。

## 構造

domain 側の `XxxRepository` interface を実装する public class で、`@Inject constructor` に DataSource を 1 つだけ取る。
`AuthRepositoryImpl` は `AuthDataSource`、`UploadRepositoryImpl` は `UploadDataSource`、
`LocalUploadRecordsRepositoryImpl` と `RemoteUploadRecordsRepositoryImpl` はそれぞれ
`UploadRecordLocalDataSource` と `UploadRecordRemoteDataSource` を受け取る。

各 override は DataSource の同名関数への単一式委譲で、本体に文が並ぶことはない。戻り値は domain の型
（`AuthResult<SignInState>`、`UploadResult<CloudStoragePath>`、`List<UploadRecord>`、`Flow<GridColumnCount>`）で、
Amplify や Room 由来の型は現れない。

## try/catch と mapper を持たない

Repository 実装には try/catch も `runCatching` も mapper 呼び出しも無い。provider 例外の変換と
`AuthResult` / `UploadResult` の生成は DataSource 側で完了している。ここで再度 catch すると同じ変換が二箇所へ分散し、
片方だけ直された状態が生まれる。result 型を使わない `upload_records` 系では、DataSource が投げた例外が
そのまま呼び出し元（UseCase / Worker）へ伝わる。

## 値の型変換だけは境界で行う

`SettingsRepositoryImpl` は例外的に変換を持つ。`SettingsDataSource` が DataStore の格納型に合わせて `Int` を扱うため、
Repository 側で `Flow` を `map` して `GridColumnCount` へ包み、書き込み時は `.value` を渡して戻す。
既定値や下限（0 以下を無視する等）の判断は DataStore の都合なので DataSource に残し、Repository へ持ち上げない。

## 同期の判断は置かない

local と remote の upload records は別々の Repository 実装に分かれており、どちらも委譲だけを行う。
両者を突き合わせる同期判断（`SyncStatus` の遷移、どちらを正とするか、リトライ）は UseCase と Worker 側にある。
画面都合の分岐を Repository 実装へ入れると、別の画面から同じ Repository を使えなくなる。

## Flow を返す場合

`LocalMediaRepositoryImpl.getMediaListFlow()` は DataSource の suspend 関数を `flow` builder で 1 回 `emit` して
完了する Flow に包むだけ。MediaStore の変更通知を購読していないためで、継続更新が必要になった場合は
DataSource 側に購読を作る話になる。Repository 実装に `while` ループや再取得を書かない。

## DI binding

実装 class は `feature:<name>:data` にあるが、binding は `app/src/main/kotlin/com/appvoyager/cloudphotos/di/` の
`@Module` かつ `@InstallIn(SingletonComponent::class)` な abstract class に `@Binds @Singleton` で書く。
Repository 実装を追加したら `AuthRepositoryModule` / `UploadRecordModule` / `UploadRepositoryModule` / `MediaDataModule`
のうち対応するものへ追記する。app モジュール側の更新を忘れると DI graph の欠落としてビルド時に出る。

## テスト

`src/test/.../repository/XxxRepositoryImplTest` は DataSource を `mockk` で差し替え、
`runTest(StandardTestDispatcher())` の中で戻り値の一致（`assertEquals`）と `coVerify(exactly = 1)` の両方を確認する。
request は `testutil/` の fixture 関数を使い、テスト側で値オブジェクトを組み立て直さない。
