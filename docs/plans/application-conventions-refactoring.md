# アプリ本体の規約準拠リファクタリング設計

状態: 設計案。アプリ本体・テスト・ビルド設定は未変更。
調査日: 2026-10-31。
調査基準: Cloud Photos のローカル `HEAD` `3d61fd888665cd7271e4e95a340e7ab09cb02567` と作業ツリー。リモートの最新状態は再取得していない。

## 目的と範囲

刷新した技術文書・Skill にアプリ本体を合わせ、規約との差が実行時の問題につながる箇所から修正する。
`app / core / feature` のマルチモジュール構成と `ui -> domain <- data` の依存方向は維持する。
Lite Memo のアーキテクチャと実装も参考にし、責務・状態・外部 I/O の境界を明確にする。

対象は `app/src/main`、`core/*/src/main`、`feature/*/*/src/main` と、変更した振る舞いを保証するテスト。
テスト全体の書き換え、依存更新、CI/CD の修正、モジュール再編、認証機能や同期仕様の追加は含めない。
本設計書は develop から分岐した `feature/application-conventions-refactoring-plan` で管理する。
今回は設計書の作成・更新、設計用 feature ブランチの作成と設計書の commit までとし、アプリ実装・push・PR 作成は行わない。

判断の正本:

- [Architecture](../architecture.md): モジュール境界、UseCase、domain abstraction、Repository / DataSource、Composition Root。
- [Implementation Guidelines](../implementation-guidelines.md): StateFlow、Effect、キャンセル、main-safe、Clock、Resource、コメント。
- [Authentication](../authentication.md)、[Media Sync](../media-sync.md)、[Data Model](../data-model.md): 維持する業務・保存・バックグラウンド実行契約。
- [Development Setup](../development-setup.md)、[Testing](../testing.md): 実装時の検証範囲とテスト方法。

## Lite Memo から取り入れる構造と適用しない差分

参照元は `C:\Users\masato\AndroidStudioProjects\lite-memo` の現行ファイル。読み取りのみとし、Git の版は確認できていない。
`docs/architecture.md`、`docs/implementation-guidelines.md` と関連する実装を照合した。
参照元のコードにも先行実装・例外があるため、そのままコピーすることは準拠の根拠にしない。

参照元のコードパスは `app/src/main/kotlin/com/lambdarc/litememo/` を基準とする。表内で省略した `ui/`、`domain/`、`data/` はすべてこの配下を指す。

| 観点 | 参照した実装 | Cloud Photos への適用 |
| --- | --- | --- |
| 接続と描画の分離 | `app/src/main/kotlin/com/lambdarc/litememo/ui/route/SettingsRoute.kt`、`ui/screen/SettingsScreen.kt` | Hilt、Lifecycle、権限、Android SDK と接続する Screen と、state / callback を受け取る Content を分ける。Cloud Photos の Screen / private Content という名前と配置を維持する |
| 画面単位の状態所有 | `ui/viewmodel/SettingsViewModel.kt`、`MainViewModel.kt`、`LiteMemoAppViewModel.kt` | 起動時のセッション判定は app、フォームは各 auth ViewModel、一覧・撮影は各 media ViewModel が所有する。全画面の状態を MainViewModel へ集約しない |
| 必要な範囲の domain contract | `domain/repository/DisplaySettingsRepository.kt`、`data/repository/DataStoreUserSettingsRepository.kt` | 呼び出し側の責務に合わせた interface を使う。既存の local / remote Repository、Scheduler、CapturedPhotoWriter を維持する。列数設定しかない SettingsRepository を機械的に分割しない |
| I/O の実行場所と資源の所有 | `data/image/MemoImageFileDataSource.kt`、`data/di/DispatcherModule.kt` | Data 側で dispatcher を切り替え、開く・使う・閉じるまでを所有する。単一モジュールの `data.di` 配置は転用せず、既存の app DI を維持する |
| 外部値の差し替え | `data/provider/SystemCurrentTimeProvider.kt` | Cloud Photos の既存 Clock を使う。別名の時刻 provider を増やさない |
| キャンセルと通常失敗の分離 | `data/repository/StagingMemoImportArchiveRepository.kt` | 通常失敗への変換前に CancellationException を再スローする |
| 重要な処理結果の配送 | `ui/route/SettingsRoute.kt` の要求 ID と処理済み callback | collector 不在時の取りこぼしを検討する参考にする。Cloud Photos 全体を state + acknowledgment へ移行する変更は今回に含めない |

意図的に転用しない方針:

- Lite Memo の「単純委譲でも UseCase 必須」は、Cloud Photos の [UseCase と Domain Abstraction](../architecture.md#usecase-と-domain-abstraction) と異なる。CameraViewModel → CapturedPhotoWriter を維持し、既存 auth UseCase も一括削除しない。
- Lite Memo の単一 `:app`、役割別の全体パッケージ、Route / Screen のファイル配置を移植しない。
- Room の schema export、migration、UI result / event 命名、テスト命名、Throwable を持つ例外契約を移植しない。
- ローカルファイルの cleanup と S3 / API / Room の同期を同じ設計にしない。キャンセル時の MediaStore レコード削除や S3 孤立オブジェクトの扱いは Cloud Photos の契約を守る。

## 維持する責務

| 所有者 | 維持する内容 |
| --- | --- |
| `app` | 初期化、認証開始先、トップレベル Navigation、DI、FCM 起動連携と登録 Worker |
| `feature:*:ui` | Screen / Content、画面固有の StateFlow、入力 callback、Effect の UI 解決 |
| `feature:*:domain` | 業務ルール、入力値、Repository / Scheduler / Writer の契約、複数操作の調整 |
| `feature:*:data` | SDK / DB / DataStore / MediaStore、変換、メディア Worker / Scheduler |
| `core:*` | 現在複数箇所で使う最小限の共通契約・基盤・リソース |

Domain への Android / SDK 型の持ち込みや、feature UI から data 実装への直接依存は、調査したソースでは見つからなかった。
Repository の単純委譲やファイルの行数だけを理由に、層の削除やクラスの分割を行わない。

## 変更設計

### A. 認証キャンセルを通常エラーへ変換しない

対象: [AuthDataSourceImpl](../../feature/auth/data/src/main/kotlin/com/appvoyager/cloudphotos/data/auth/datasource/AuthDataSourceImpl.kt)。

現状は `signUp`、`confirmSignUp`、`signIn`、`fetchCurrentUser`、`getSession`、`resendSignUpCode`、`resetPassword`、`confirmResetPassword` の 8 経路で、suspend 処理の `runCatching.fold` が CancellationException をそのまま AuthErrorMapper に渡す。
Mapper の未分類分岐により AuthError.Unknown へ変わり、キャンセルと通信・認証失敗の契約が混ざっている。

- 既存の `rethrowIfCancellation()` を、8 経路とも mapper 適用前に呼ぶ。
- provider の通常エラー分類、サインインの追加認証 step、サインアウトの PartialSignOut 判定は維持する。
- FCM cleanup と backend → Cognito の退会順序は変更しない。
- Amplify.Auth 自体の通信停止を実現する変更とは扱わない。キャンセル可能な operation を返さない API は、呼び出し側の待機のキャンセルを保証する。
- テストのために必要なら SDK facade を data 内の internal コンストラクタで差し替える。SDK の型・テスト用抽象を domain へ追加しない。最小の差し替え方法は実装時に使用中 SDK の型で確認する。

検証: 新規 AuthDataSourceImplTest で 8 経路のキャンセル、代表的な成功・通常失敗を確認する。既存 AuthErrorMapperTest / Repository / auth UI のテストも実行する。

### B. アップロード準備の大量 ID 検索を分割する

対象: [PrepareUploadQueueUseCase](../../feature/media/domain/src/main/kotlin/com/appvoyager/cloudphotos/domain/media/usecase/PrepareUploadQueueUseCase.kt)。

現状の `getUploadRecords(allMediaIds)` は端末内の全 ID を一度に DAO の IN 句まで渡す。
[GetPendingMediaUseCase](../../feature/media/domain/src/main/kotlin/com/appvoyager/cloudphotos/domain/media/usecase/GetPendingMediaUseCase.kt) は、同じ契約に対して既に 900 件ずつ分割している。
端末ごとの SQLite 上限を断定せず、プロジェクトで採用済みの保守的な分割方針に合わせる。

- PrepareUploadQueueUseCase でも 900 件単位に分割し、既存 ID を集合へ集約する。
- 全チャンクの取得が終わるまで新規レコードを保存・enqueue しない。
- 空一覧、全件登録済みの場合の早期 return、新規レコードの保存後に Scheduler を 1 回呼ぶ順序を維持する。
- DAO の SQL、Repository interface、Room schema は変えない。2 箇所の短い分割処理のために汎用 framework を導入しない。

検証: PrepareUploadQueueUseCaseTest に 900 / 901 件境界、複数チャンクで既存・新規が混在する例、全件既存、後続チャンクの失敗・キャンセル時に保存と enqueue をしない例を追加する。

### C. メディア I/O の main-safe と Result 契約を揃える

対象:

- [UploadDataSourceImpl](../../feature/media/data/src/main/kotlin/com/appvoyager/cloudphotos/data/media/datasource/UploadDataSourceImpl.kt)
- [MediaStoreCapturedPhotoWriter](../../feature/media/data/src/main/kotlin/com/appvoyager/cloudphotos/data/media/datasource/MediaStoreCapturedPhotoWriter.kt)

UploadDataSourceImpl は ContentResolver.openInputStream を呼び出し元の context で実行している。
stream の取得・use・close を IO context 内で所有する。開いた stream だけを `withContext` の外へ返す形にはしない。dispatcher 境界でキャンセルされても資源を残さないためである。
Amplify の非同期 callback 待機を、ブロッキング処理へ置き換えない。既存の operation.cancel() 連携と UploadResult の分類を維持する。

MediaStoreCapturedPhotoWriter は createContentValues / insert が try の外にあり、挿入時の予期される失敗が SavePhotoResult に変換されない。
またファイル名の生成に System.currentTimeMillis() を直接使っている。

- insert から保存確定までをエラー処理の対象にし、実際に作成された Uri だけを cleanup 対象として保持する。
- ENOSPC は STORAGE_FULL、その他の予期される保存失敗は SAVE_FAILED、null insert も SAVE_FAILED とする。
- insert 前の失敗では削除を呼ばない。書き込み / pending 解除失敗では作成済みレコードの削除を試みる。
- CancellationException は再スローし、その経路ではレコードを削除しない現行契約を維持する。NonCancellable cleanup は追加しない。
- 既存の Clock を注入し、ファイル名を決める時刻だけを差し替えられるようにする。形式、Locale.US、端末の既存 timezone、保存場所、MIME、IS_PENDING の順序を維持する。
- dispatcher のテスト差し替えは、SettingsDataSourceImpl と同様の internal コンストラクタと Inject コンストラクタの委譲を第一案とする。新しい共有モジュール・DI qualifier は不要。

検証: stream の成功・失敗・キャンセル時の close と operation.cancel、固定 Clock、insert 失敗、null stream、write / update 失敗、pending 解除、cleanup の範囲を確認する。
JVM でモック化できる振る舞いは Unit Test とし、実 ContentValues / ContentResolver / MediaStore に依存する保存統合は androidTest または実機で確認する。テストのためだけの Robolectric や新規 framework は前提にしない。
ClockModule の既存 binding を再利用し、Hilt を含むビルドで解決を確認する。

### D. アップロード通知の終了処理を保証する

対象: [UploadMediaWorker](../../feature/media/data/src/main/kotlin/com/appvoyager/cloudphotos/data/media/worker/UploadMediaWorker.kt)。

進行中通知の cancel は現在ループ後にしかない。SDK / Room 処理がキャンセル・例外で抜けると到達しない。

- 通知表示後のループを try / finally で囲み、finally で通知を消す。
- 例外や CancellationException は元の契約どおり伝播させ、retry / success へ変換しない。
- `completeUpload()` は通常のループ完了後、`!hasTemporaryFailure && successCount > 0` の場合だけ 1 回呼ぶ。finally には入れない。
- pending が空なら通知を表示も cancel もしない。S3 再転送防止、1 件ごとの status 再取得、失敗分類、cleanup 後の PENDING_DELETE は維持する。

検証: 既存 UploadMediaWorkerTest にキャンセルと未捕捉 DB 例外のケースを加え、通知が 1 回消され、元の例外が伝播し、completeUpload が呼ばれないことを確認する。

### E. 起動状態を StateFlow で公開する

対象: [MainViewModel](../../app/src/main/kotlin/com/appvoyager/cloudphotos/ui/MainViewModel.kt)、[MainActivity](../../app/src/main/kotlin/com/appvoyager/cloudphotos/ui/MainActivity.kt)、MainViewModelTest。

feature の ViewModel は StateFlow を使っているが、MainViewModel の `uiState`、`isCheckingSession`、`isRetrying` は Compose の mutableStateOf を公開している。

- 3 つの状態を private MutableStateFlow / public read-only StateFlow に置き換える。sealed な MainUiState と既存のフラグの意味を維持し、無関係な状態の統合を必須にしない。
- 更新は update を使い、MainActivity の Compose 内では collectAsStateWithLifecycle() で購読する。
- スプラッシュ保持条件は最新の isCheckingSession.value を読む。起動時のセッション確認開始箇所、重複 Job ガード、finally でのフラグ解除を維持する。
- startDestination と back stack の構成は維持する。MainViewModel を全画面のセッション監視・ナビゲーションの新しい状態機械へ拡張しない。
- Authenticated 描画時の FCM 登録連携とアプリ起動 / onNewToken の登録契機を維持する。

検証: 初期状態、guest / signed-in / error、再試行中、重複呼び出し、キャンセルと finally を private Snapshot で確認する。スプラッシュ表示、再試行、画面再生成、認証前後の Back 操作を手動確認する。

### F. 認証 UI の resource と派生値の配置を揃える

対象: [LoginScreen](../../feature/auth/ui/src/main/kotlin/com/appvoyager/cloudphotos/ui/auth/screen/LoginScreen.kt)、[ForgotPasswordScreen](../../feature/auth/ui/src/main/kotlin/com/appvoyager/cloudphotos/ui/auth/screen/ForgotPasswordScreen.kt)、auth UiState / ViewModel、[共有文字列](../../core/ui/src/main/res/values/strings.xml)。

- Login / ForgotPassword の直書きラベル、ボタン、説明、contentDescription を string resource へ移す。既存の意味が同じ resource は再利用する。
- 日本語の文言と操作を維持し、values-en も更新する。新しい認証エラー表現やアカウントの存在を示す文言は追加しない。
- ForgotPasswordUiState / ResetPasswordUiState / VerificationCodeUiState の `isFormValid`、`isCodeComplete`、`isResendEnabled` は、規約に合わせて所有 ViewModel の computed property へ移す。入力値や timer の保存場所は変えない。
- 現在も computed getter であり、重複して保存された state の不具合ではない。保存済み Boolean を増やしたり、独立した StateFlow に二重管理したりしない。
- Screen は購読した uiState の更新で再描画し、computed property を Content の表示引数へ渡す。ViewModel 内の入力検証と UI の有効条件を合わせ、テストも更新する。
- 同時に触る純粋 helper の StringUtils / ValidationUtils を internal object に限定する。

検証: auth UI Unit Test、フォームの有効条件、コード貼り付け・自動送信・再送 timer、日英 Preview、長い文字列、resource build / lint を確認する。

### G. カメラの接続処理と描画を分ける

対象: [CameraScreen](../../feature/media/ui/src/main/kotlin/com/appvoyager/cloudphotos/ui/media/screen/CameraScreen.kt)。

現在は 1 つの Screen に ViewModel 接続、権限、camera manager、Lifecycle、描画が同居し、CameraContent と画面全体の Preview がない。

- Screen は state / Effect の収集、権限 launcher、Lifecycle、CameraPreviewManager、撮影 bridge を所有する。
- private CameraContent は CameraUiState、画面限定の表示値、callback、カメラ表示 slot を受け取る。slot を使い、Preview ではハードウェアや PreviewView を生成しない。
- lens 選択、flash アニメーション、thumbnail、ダイアログ開閉は寿命の短い画面状態として適切な UI 所有者に置く。描画値を理由なく ViewModel へ移さない。
- CameraX は UI と結び付いた SDK のため、この分割を理由に domain / data へ移さない。
- MediaDetailScreen に ViewModel を新設しない。MediaViewModel も行数を理由に分割しない。

検証: 権限確認中・要求・Ready・Capturing・Error の Hilt 不要 Preview、media UI Unit Test、実機の権限拒否 / 許可、切替、撮影、pause / resume、容量不足表示を確認する。

### H. 認証 Effect の Lifecycle と配送を一緒に整える

対象: auth の 4 ViewModel / 4 Screen / 対応テストと、共通規約・両クライアントの UI reference。

ForgotPassword / VerificationCode / ResetPassword の 3 Screen は Effect を直接 collect している。一方 Login は既に STARTED で収集している。
4 ViewModel はいずれも `MutableSharedFlow(extraBufferCapacity = 1)` であり、collector 不在時に Effect を保持しない。3 Screen の収集側だけを止めると、バックグラウンド中の認証完了による画面遷移を落とし得る。

この項目は単純な規約修正ではなく、既存の配送例外を見直す設計提案とする。

採用案:

- 4 ViewModel の Effect を `Channel<XxxEffect>(Channel.BUFFERED)` / `receiveAsFlow()` に揃え、4 Screen を STARTED の repeatOnLifecycle で収集する。
- effect の意味、Snackbar message 型、画面遷移先は変えない。各 ViewModel の collector は 1 つとする。
- collector の停止中に発生し、まだ受信されていない Effect を ViewModel 存続中はキューで待たせる。回転・停止復帰をまたぐ配送をテストする。
- timer の開始は repeat ブロックの外に置き、collector 再開を理由に cooldown をリセットしない。
- 長生きする collect ブロック内の callback / Context は rememberUpdatedState で参照する。
- Channel をプロセス終了後も残る配送や exactly-once の保証とは扱わない。受信時と Lifecycle 停止が競合すると、キューから取り出した Effect が handler 実行前にキャンセルされる可能性も残る。未受信のキュー保持と、すべての停止復帰での画面遷移保証を区別する。
- 受信済みの Snackbar 中断、受信と handler の引き渡し時の取りこぼし、プロセス再生成後の結果復元を新たに保証する変更は、この段階には含めない。これらを含めて重要結果の配送を保証する要件にする場合は、H を state + 消費確認の設計へ改める。

現行規約との関係:

- [Implementation Guidelines](../implementation-guidelines.md) は「既存 auth 4 ViewModel は MutableSharedFlow を維持する」と明記している。本提案はこの例外と異なるため、実装する際は例外を更新する文書変更と一体にする。
- 両方の [Codex UI event reference](../../.agents/skills/ui-implementation/references/uievent.md) / [Claude UI event reference](../../.claude/skills/ui-implementation/references/uievent.md) と ViewModel reference の対応箇所を確認し、差が生じる箇所を手動反映する。
- Lite Memo の重要結果を state に保持して消費確認する方式は、より強い再生成対応が要件になった時の別設計とする。今回の全 Effect をその方式に置き換えない。
- H を実施しない段階では、3 Screen の Lifecycle gating だけを先行させない。

検証: 4 ViewModel の collector 不在 → 再収集での配送、継続収集での二重送信なし、ログイン完了・コード確認完了・パスワード再設定完了、SavedStateHandle の message 初期配送を確認する。
実際の Lifecycle STOPPED / STARTED、回転、callback 更新、timer 継続は UI の手動確認または UI テストで別に確認する。Unit Test だけで Lifecycle まで検証済みとしない。

## 小さな整理と今回見送る変更

規約に直接対応する小さな整理は、関連する実装を触る段階に含める。

- ConfirmationCode.of の長さと数字の require を分ける。trim、6 文字、Char.isDigit の受理範囲、例外型を維持する。ASCII 数字限定へ変更しない。
- AuthSignInStepMapper は純粋な変換のため internal object にする。
- AuthError.Unknown に残る実態と合わない「cause はログ用」という KDoc を削除する。

以下は今回の必須変更にしない。

- Upload / Delete Worker の HTTP エラー判定共通化。重複自体は現在の規約違反ではない。実施する場合も media:data 内に閉じ、404 / 429 を一時失敗とする意味を維持する。FCM の isNonServerError へ置換しない。
- 4 Screen の Snackbar message → resource 変換の共通化。resource 化と混ぜず、変更の価値がある場合に auth UI 内だけで行う。
- Room Entity / DAO / mapper / DB version の変更、schema export の導入。未知 syncStatus → ERROR と保存済みデータの互換性を維持する。
- テーマのテンプレートコメントの全整理、全パッケージの改名、全 sealed class / interface の統一。
- 同期 runCatching の一律置換。値生成・JSON パースだけの同期処理と、suspend 境界のキャンセルを区別する。
- CameraPreviewManager の非 coroutine future listener にあるキャンセルの無視。カメラ切替・unbind による意図的な終了であり、coroutine のキャンセル握り潰しと区別する。

## モジュール・レイヤー単位の段階的移行プラン

移行は Domain → Data → UI → app の順を基本にする。各段階は対象モジュールの本番コードと対応テストを同時に更新し、検証を通した状態で区切る。
既存の公開 contract を維持して内部実装を置き換えるため、全層の変更を待たずに各 PR をマージできる構成にする。
実装時は **段階 01〜09 ごとに別の feature ブランチを作成し、段階ごとに PR を分ける**。各段階の「ブランチ案」をその開始時の名前とする。
設計用ブランチに実装を積み重ねず、設計書のマージ後に各実装段階のブランチを develop から分岐する。
段階 00 は事前確認であり、コード変更がなければ専用ブランチ・PR は作らない。未採用の段階 07 のブランチも、その採用と着手が決まるまで作成しない。
この設計作業で作成するのは設計用 feature ブランチだけとし、段階別の実装ブランチはまだ作成しない。

### モジュール別の移行範囲

| モジュール | 移行対象 | 段階 |
| --- | --- | --- |
| `:core:common` | Clock の既存契約を維持。共通抽象の新設なし | 00 で確認、04 で利用 |
| `:core:data` | REST のキャンセル橋渡し、SystemClock、FCM API の既存契約を維持 | 00 で確認、03〜05・09 の関連検証 |
| `:core:ui` | 認証画面の日本語・英語 resource | 06。利用側の auth UI と同じ PR |
| `:feature:auth:domain` | ConfirmationCode の制約分離、古い AuthError KDoc の整理 | 01 |
| `:feature:media:domain` | アップロード準備の ID 分割 | 02 |
| `:feature:auth:data` | 認証のキャンセル再スロー、mapper の可視性 | 03 |
| `:feature:media:data` | 保存・アップロード I/O と Clock、Worker の通知終了処理 | 04・05 |
| `:feature:auth:ui` | resource 参照、派生値の配置、helper の可視性、Effect 配送 | 06・07 |
| `:feature:media:ui` | CameraScreen / Content / Preview の分離 | 08 |
| `:app` | 起動状態の StateFlow 化と全体の接続確認 | 09。04 で既存 DI による解決も確認 |

`core:common`、`core:data`、Room の保存構造には、今回確認した差分を解消するための変更を予定しない。対象がないモジュールに空の移行 PR を作らない。
`build-logic` とモジュール構成も維持する。実装中に新たな問題を見つけた場合は、根拠と波及先を本設計へ追加してから、その問題の所有モジュールで扱う。

### 00. 移行前の基準を確定する

- 作業ツリーと最新 develop を確認し、既存の変更を保持したうえで、実装対象を develop から分岐する。現在のブランチ名だけを根拠に実装を始めない。
- `./gradlew.bat test` と `./gradlew.bat ktlintCheck detekt` を実行し、既存の失敗と今回の変更による失敗を区別できる状態にする。未実施の検証と環境上の理由も記録する。
- 開発用環境値と端末が用意できる場合は dev debug build と、起動・認証・一覧・撮影の基準動作を確認する。環境を用意できない確認は未検証として残す。
- 完了条件: 対象 commit、作業ツリー、既存のテスト / 静的解析結果、端末検証の可否が記録されている。実行できない検証を成功扱いにしない。

### 01. Auth Domain

ブランチ案: `feature/01-auth-domain-conventions`。

- 対象: `:feature:auth:domain` の ConfirmationCode と AuthError、および ConfirmationCodeTest。
- 作業: require を長さ・数字ごとに分け、実態と合わない KDoc を除く。入力の受理範囲、例外型、AuthResult / AuthError の公開 contract は維持する。
- 検証: `./gradlew.bat :feature:auth:domain:test`。メッセージに依存する既存テストの期待値を合わせる。
- 完了条件: 既存の正常値と不正値の判定が変わらず、各制約の失敗理由を識別できる。Data / UI の実装変更を要求しない。

### 02. Media Domain

ブランチ案: `feature/02-media-domain-batching`。変更設計 B に対応する。

- 対象: `:feature:media:domain` の PrepareUploadQueueUseCase と対応テスト。
- 作業: 900 件単位の参照、既存 ID の集約、全取得後の保存と 1 回の enqueue を実装する。
- 検証: `./gradlew.bat :feature:media:domain:test`。境界件数、既存・新規混在、後続チャンクの失敗・キャンセルを確認する。
- 完了条件: 大量 ID を 1 回で渡さず、途中の参照失敗で部分的な保存・enqueue をしない。Repository interface と DAO は維持する。

### 03. Auth Data

ブランチ案: `feature/03-auth-data-cancellation`。変更設計 A に対応する。

- 対象: `:feature:auth:data` の AuthDataSourceImpl、AuthSignInStepMapper、対応テスト。
- 作業: 8 経路のキャンセル再スローと mapper の internal 化。テストに必要な SDK 差し替え口はこのモジュールに閉じる。
- 検証: `./gradlew.bat :feature:auth:domain:test :feature:auth:data:test :feature:auth:ui:test`。上位層へキャンセルが伝播しても通常失敗の表示に変わらず、既存の認証分岐を維持することを確認する。
- 完了条件: 8 経路すべてでキャンセルが AuthError.Unknown に変わらず、通常エラー分類・サインアウト・退会順序が保たれる。

### 04. Media Data の I/O

ブランチ案: `feature/04-media-data-io`。変更設計 C に対応する。

- 対象: `:feature:media:data` の MediaStoreCapturedPhotoWriter、UploadDataSourceImpl と対応テスト。
- 作業: 最初に保存の Result 境界と Clock 注入、次にアップロード stream の IO context と資源管理を変更する。差分が大きければ、同じモジュール内の保存 / アップロードで 2 PR に分ける。
- 検証: media domain / data / UI の Unit Test、保存の Android I/O 検証、必要な実機権限を用意した `:feature:media:data:connectedDebugAndroidTest`、`assembleDevDebug` による既存 Clock binding と Hilt の解決確認。テストの具体的な振り分けは C に従う。
- 完了条件: stream が各終了経路で閉じられ、保存失敗が所定の Result へ変換される。固定時刻のファイル名とキャンセル時の非削除契約を確認している。
- app 側の既存 binding で解決する限り、そのコードは変更しない。配線変更が必要なら、この PR に必要最小限の app DI 差分を含める。

### 05. Media Data の Worker

ブランチ案: `feature/05-media-data-worker-cleanup`。変更設計 D に対応する。

- 対象: `:feature:media:data` の UploadMediaWorker と対応テスト。
- 作業: 通知終了を finally で保証する。04 の DataSource 契約を前提に、通常完了・例外・キャンセルを確認する。
- 検証: `./gradlew.bat :feature:media:domain:test :feature:media:data:test :feature:media:ui:test`。通知、例外伝播、completeUpload の呼び出し条件・回数を確認する。
- 完了条件: 通知を表示したループの異常終了でも通知が消え、異常終了時には completeUpload を呼ばない。Scheduler の名前・policy・constraints・input Data と DB 保存形式は変わらない。

### 06. Auth UI と共有 resource

ブランチ案: `feature/06-auth-ui-conventions`。変更設計 F に対応する。

- 対象: `:feature:auth:ui` と `:core:ui` の日英 strings.xml、および auth UI テスト。
- 作業: resource 化、派生値の ViewModel への移動、helper の internal 化を行う。利用側の書き換えと新規 resource は同じ PR に含め、参照先が揃った状態でマージする。
- 検証: core:ui を変更するため `./gradlew.bat test`、resource を含む `assembleDevDebug`、auth の日英 Preview とフォーム操作。
- 完了条件: 表示文字列と contentDescription が resource から解決され、有効条件・コード入力・再送 timer が既存どおり動く。Effect の配送方式と Lifecycle gating はこの段階では変更しない。

### 07. Auth UI の Effect 配送

ブランチ案: `feature/07-auth-ui-effect-delivery`。変更設計 H に対応する別設計の採用段階。

- 着手条件: 03・06 の完了と、H の配送方式・保証範囲を移行方針として確定していること。A〜G の規約準拠とは別の方針変更であり、未採用ならこの段階だけを保留し、08・09 は進められる。
- 対象: `:feature:auth:ui` の 4 ViewModel / 4 Screen / 対応テストと、共通規約・両クライアントの UI reference。
- 作業: producer の Channel 化、consumer の STARTED 収集、callback の最新化、規約の例外更新を同じ PR に含める。consumer だけの移行は行わない。
- 検証: auth UI Unit Test、停止復帰・回転・message 初期配送・timer 継続の UI 確認、文書リンクと両クライアントの対応箇所。
- 完了条件: 未受信 Effect の保持と停止復帰時の挙動を確認し、Channel の受信時キャンセル競合・プロセス終了への保証がないことを文書化している。より強い保証を求める場合は H の再設計を先に行う。

### 08. Media UI

ブランチ案: `feature/08-media-ui-camera-content`。変更設計 G に対応する。

- 対象: `:feature:media:ui` の CameraScreen / private CameraContent / Preview と関連テスト。
- 作業: 接続処理と描画を分離する。04 の保存契約を利用し、UI 側に I/O や新たな UseCase を増やさない。
- 検証: `./gradlew.bat :feature:media:ui:test`、状態別 Preview、実機の権限・切替・撮影・pause / resume・保存失敗表示。
- 完了条件: Hilt・カメラ実機に依存しない Content Preview が描画でき、Screen の Lifecycle と操作結果が既存どおり動く。

### 09. App の状態管理と最終統合

ブランチ案: `feature/09-app-stateflow`。変更設計 E と全体の接続確認に対応する。

- 対象: `:app` の MainViewModel / MainActivity / 対応テスト。
- 作業: 起動状態を StateFlow へ移し、ここまでの feature と既存 Navigation / DI / FCM の接続を確認する。feature 固有の状態を app へ移さない。
- 検証: `./gradlew.bat test`、`./gradlew.bat ktlintCheck detekt assembleDevDebug`。端末ではスプラッシュ・再試行・認証前後の Back・一覧・撮影・通知を確認する。端末が必要な検証は JVM 結果と分けて報告する。
- 完了条件: 起動状態・フラグ・認証開始先・FCM 登録契機が維持され、各段階の検証結果が揃っている。07 を保留した場合は、配送変更を未採用として明記する。

### 段階間の依存とレビュー運用

番号は推奨する実行順であり、コード上すべてが直列依存するわけではない。

| 段階の関係 | 意味 |
| --- | --- |
| 01 → 03 → 06 | auth の下位層から順に検証する推奨順。公開 contract を維持するため、後続の実装を同時にマージする必要はない |
| 02 → 04 → 05 | media の準備・I/O・Worker を順に確認する。04 と 05 は同じ data モジュールのため、テスト変更と例外処理の前提を引き継ぐ |
| 03・06 → 07 | 認証キャンセルと UI 整理を終えてから、同じ auth UI ファイルの配送変更に進む |
| 04 → 08 | 保存契約を先に整え、Content 分離後の撮影結果・保存失敗表示を通して確認する |
| 01〜06・08 → 09 | 基本移行を終えて app 接続を最終確認する。07 は採用した場合だけ最終確認へ含める |

各 PR は 1 本ずつレビュー・マージし、その結果を取り込んだ最新 develop から、次段階専用の feature ブランチを新規作成する。前段階のブランチを使い回したり、そのブランチから後続を分岐したりしない。
未マージの後続ブランチを一括作成しない。段階 04 を保存 / アップロードの 2 PR に分割する場合も、各 PR 用の feature ブランチを順に作成する。
各段階の完了時に、対象 commit、変更モジュール、公開 contract の変更有無、実行した検証と未実施理由、次段階への影響を PR または作業記録へ残す。
レビューで contract や規約を変更した場合は、後続の対象・テスト・完了条件も更新する。
不具合が出た場合はその段階の PR 単位で戻せる範囲を維持し、未完の後続実装で補う前提にしない。
A / B / C / D は規約から外れていた失敗・キャンセル経路の改善であることを、単なる整形と区別して PR に記載する。

## 検証と完了条件

### 実装時に選ぶ検証

PowerShell では `./gradlew` に相当する `./gradlew.bat` を使う。実際の変更に応じて [Development Setup](../development-setup.md) の基準で選択する。

| 対象 | コマンド / 確認 |
| --- | --- |
| Auth の変更 | `./gradlew.bat :feature:auth:domain:test :feature:auth:data:test :feature:auth:ui:test` |
| Media のロジック・I/O・Worker | `./gradlew.bat :feature:media:domain:test :feature:media:data:test :feature:media:ui:test` |
| app / core:ui / 複数領域 | `./gradlew.bat test` |
| 最終統合 | `./gradlew.bat ktlintCheck detekt assembleDevDebug` |
| 実 Android I/O、DAO / DataStore / Scheduler への変更が生じた場合 | `./gradlew.bat :feature:media:data:connectedDebugAndroidTest`。エミュレータ・実機と必要権限を用意する |
| UI | 日英・ライト / ダーク Preview、起動再試行、認証 Back、停止復帰、カメラ撮影 / 権限 / 切替 |
| 文書変更 | ローカルリンク、Codex / Claude 対応箇所、`git diff --check` |

Kotlin と resource の変更だけなら Gradle Sync は通常不要。テスト都合で Gradle 設定や依存を追加した場合だけ、その変更に応じた Sync・全体検証を追加する。
assembleDevDebug と実行には開発用の環境値を使い、ダミーの接続値で実機検証成功と報告しない。
Unit Test は Cloud Photos の JUnit 5・backtick 名・private Snapshot、androidTest は JUnit 4・snake_case の規約に従う。
テストを新設・更新する実装段階では test-implementation Skill も併用する。

完了条件:

1. モジュール・依存方向・公開 domain contract・保存データ互換性を維持している。
2. A〜G の確認済み差分が解消され、関連する通常動作と失敗・キャンセルのテストが通っている。
3. H を採用する場合は、受信側と配送側と規約を一緒に変更し、認証結果の停止復帰時の挙動を確認している。
4. アーキテクチャ上の例外、意図した cleanup、pending 保護を整形・共通化の名目で削除していない。
5. 実行した検証と未実施の検証を区別し、端末・環境値などの理由を記載している。

### この設計作業での確認範囲

- 実施: 現行規約・役割別 Skill と本番コードの静的照合、関連する既存テストの確認、Lite Memo の規約と関連実装の読み取り。
- 未実施: Gradle test、静的解析、ビルド、IDE Preview、実機 / エミュレータ、リモート PR / CI の再確認。設計書のみの変更であり、アプリの動作確認を行う段階ではないため。
- SDK facade の具体的なテスト差し替え方法と Android I/O テストの配置は、実装時にコンパイル可能な最小構成で確定する。現在のコードに直接テストがない境界について、既にテスト可能と断定しない。
- 本設計書は両クライアント共通の `docs/plans` に 1 件置く。入口・Skill 自体は今回は変更しない。H の実装時には対応する両クライアント文書を同時に見直す。
