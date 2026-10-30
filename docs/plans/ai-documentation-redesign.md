# AI文書の全面刷新設計

状態: 共通技術文書10件を追加済み。入口とSkillの切り替え、旧文書の整理は未実施。

## 目的と前提

Cloud PhotosのAI文書を、プロジェクトの理解、変更対象の判断、実装、検証、レビューへ到達しやすい構成に作り直す。
既存のファイル名、文書数、Skill数は維持条件にしない。

- `AGENTS.md`と`CLAUDE.md`、`.agents/skills/`と`.claude/skills/`の二重管理は維持する。
- 両クライアントの文書は手動で編集し、自動生成・同期スクリプトを導入しない。
- 共通の技術的な方針・契約は`docs/`を正本とし、両クライアントから参照する。
- 文書刷新とアプリの仕様変更は分ける。現在の構造や契約は実装を確認して記述する。
- 一般的な説明、同じ規約の繰り返し、網羅的なクラス紹介を減らし、このプロジェクトで判断が変わる情報を残す。

## Lite Memoから取り入れる設計

参照元は`C:\Users\masato\AndroidStudioProjects\lite-memo`の現行ファイル。
入口文書、6種類のSkill、主要技術文書と一部のreferenceを読み取り確認した。

| 参照元 | 取り入れる点 |
| --- | --- |
| `AGENTS.md` / `CLAUDE.md` | プロジェクトの前提、技術文書、作業別Skillを入口で案内する |
| UI / Domain / Data / DB Skill | 変更対象の役割とreferenceの対応を示し、該当するものを選ぶ |
| Test Skill | テスト規約を技術文書へ委ね、Skillには対象選定と進め方を置く |
| `docs/review.md` | 比較範囲、指摘分類、指摘の根拠、確認範囲に応じた報告を明示する |
| `docs/memo-edit-lifecycle.md`など | レイヤーをまたぐ機能の契約を、独立した文書に集める |
| referenceの構成 | 確認する対象、実装時の注意、テスト判断、検証観点に絞る |

Cloud Photos向けに次を調整する。

- Lite Memoの単一`:app`内のパッケージ配置を転用せず、`app / core / feature`のマルチモジュール構成を説明する。
- Lite MemoのUseCase必須方針は転用せず、Cloud PhotosでUseCaseが担う調整・validation・状態遷移と、domain abstraction（`CapturedPhotoWriter`など）を直接利用する条件を明示する。
- UI eventの扱いはCloud Photosのstate / callback / effectの契約から定義する。Lite Memoの配送方式へ変更する指示を混ぜない。
- テスト規約はCloud Photosの現行規約（`src/test`のbacktick名と許可動詞、`src/androidTest`のsnake_case、JUnit 5 / JUnit 4の構成、ViewModelテストのprivate Snapshot比較など）を維持し、Lite Memoの命名・assert規約や`HiltTestRunner`前提を転用しない。
- Cloud PhotosのDBは現時点で`exportSchema = false`。Lite Memoのschema exportタスクや保存済みschemaを存在するものとして案内しない。
- 各Skillで複数の共通文書を無条件に読む方式は取り入れず、作業別に必要な節を案内する。
- コメント方針は共通の技術文書に置き、両クライアントから同じ規約へ到達させる。

## 全体構成

```text
AGENTS.md
CLAUDE.md
docs/
  project-overview.md
  architecture.md
  implementation-guidelines.md
  data-model.md
  authentication.md
  media-sync.md
  development-setup.md
  testing.md
  review.md
  ci.md
.agents/skills/
  ui-implementation/
  domain-implementation/
  data-implementation/
  db-implementation/
  test-implementation/
  implementation-review/
.claude/skills/
  同じ6種類を手動管理
```

共通の技術文書は10件とする。この設計書は移行用の資料であり、通常作業の必読文書には含めない。
既存文書をこの構成へ移し、移行完了時に置き換え済みの文書と参照を整理する。

## 入口文書の責務

`AGENTS.md`と`CLAUDE.md`は、それぞれのクライアント用に次を持つ。

1. Cloud Photosの目的と、モジュール構成の最小限の前提。
2. 作業の種類に応じた技術文書とSkillへの案内。
3. `develop`からの分岐、`main` / `develop`への直接push禁止、PRテンプレートへの参照。
4. 実行した検証と未実施の検証を報告するルール。
5. ユーザーの依頼範囲を維持し、未導入の仕組みを実装済みとして扱わない前提。
6. AI文書変更時に、両側の対応箇所を確認する運用。
7. ユーザーの明示指示とSkill・技術文書が衝突した場合はユーザー指示を優先し、衝突したSkill・文書の箇所を報告する。
8. 可逆で影響の小さい操作は確認せずに合理的に推測して進める。操作の影響と既に承認された範囲に基づいて判断し、不可逆な操作に必要な承認がない場合は確認する。ユーザーが依頼したpushやPR作成など、既に承認された操作の再確認は不要とする。

入口には詳細なコーディング規約、全クラス一覧、検証コマンド一覧を置かない。
通常作業で全技術文書・全Skillを読む指示を置かず、変更する振る舞いと役割から参照先を選ぶ。

## 共通技術文書の責務

| 文書 | 扱う内容 | 参照する条件 |
| --- | --- | --- |
| `project-overview.md` | 主要機能、実装の入口、モジュールの見取り図、技術スタックの確認先 | 初めての領域、対象の配置が不明、全体像の確認 |
| `architecture.md` | モジュール境界、依存方向、composition root、UseCase / Repository / DataSource / Workerの責務と採用理由 | 配置・責務・依存・公開contractの判断 |
| `implementation-guidelines.md` | Kotlin、Compose、ViewModel、coroutines、DI、resource、コメント、ログ、エラー処理の基本方針 | 実装対象に該当する節だけ |
| `data-model.md` | `upload_records`、保存値とdomain値の対応、DAOの契約、DB互換性、migrationの判断 | Entity / DAO / 保存値 / DB versionの変更 |
| `authentication.md` | 認証セッション、確認コード、再設定、サインアウト、退会、FCM device tokenの登録と解除、アカウント列挙を避けるエラー表現 | 認証やアカウント操作、device tokenの扱いを変更 |
| `media-sync.md` | アップロード・削除・リモート同期、状態遷移、処理順、部分失敗、retry、WorkManagerの契約 | 同期・キュー・Worker・状態の意味を変更 |
| `development-setup.md` | SDK / JDK / flavor / 環境値、依存追加時の確認、ローカル検証の選び方とコマンド | ビルド・環境・依存・検証の選択 |
| `testing.md` | JVM / instrumentedの振り分け、命名、AAA、JUnit、MockK、Flow、ViewModel、Worker、DBテストの書き方 | テストの設計・追加・修正 |
| `review.md` | 比較範囲の確定、確認観点、指摘分類、根拠、終了時の報告 | 差分レビュー |
| `ci.md` | CI / CDの実行条件、job、権限、カバレッジ、CodeQL、release lane、失敗箇所の切り分け、ローカルとの違い | workflowやCI / CDの変更・調査 |

### 文書間の所有境界

- 依存方向やUseCaseの作成基準は`architecture.md`が所有し、実装規約やreferenceで再定義しない。
- Resultと例外の使い分け、キャンセル伝播の基本は`implementation-guidelines.md`が所有する。失敗契約は操作ごとに定義し、ローカルDB操作を一律にResultへ変える指示にしない。
- 認証や同期の失敗分類・処理順・状態の意味は、対応する機能文書が所有する。
- DBの保存形式・互換性とmigration判断は`data-model.md`、テストの組み立て方は`testing.md`が所有する。
- ローカルで何を実行するかは`development-setup.md`、CI / CDが何を実行するかは`ci.md`、テストをどう書くかは`testing.md`が所有する。
- `SyncStatus`の業務上の意味は`media-sync.md`、DB文字列との互換性は`data-model.md`で扱う。
- FCM device tokenの登録から解除までは`authentication.md`が所有する。`app`モジュールの`FcmTokenRegistrar`、`RegisterDeviceTokenWorker`、`CloudPhotosFirebaseMessagingService.onNewToken`、`core:data`の`DeviceTokenDataSource`を対象とする。
- upload進行中通知（`UploadNotificationHelper`）の表示・解除と、upload完了通知（`CloudPhotosFirebaseMessagingService.onMessageReceived`）の受信・表示は`media-sync.md`が扱う。両通知チャネルの作成は`app`のcomposition rootとして`architecture.md`から案内する。
- 文書にはソースへの参照と判断に必要な情報を置き、SDKバージョン、enum全件、全関数のシグネチャなどの変わりやすい一覧を複製しない。

## Skillの再編

Skillはレイヤー・変更する役割・作業目的で6種類に整理する。
認証や同期の機能文書には複数のSkillから条件付きで到達させる。

| Skill | 選択する依頼 | 主なreference |
| --- | --- | --- |
| `ui-implementation` | 画面の表示、操作、遷移、表示文言、UI stateを変更 | screen / viewmodel / ui-state / ui-event / navigation |
| `domain-implementation` | 業務ルール、入力制約、UseCase、domain model、Repository contractを変更 | usecase / value-object / result-error |
| `data-implementation` | Repository実装、DataSource、SDK / API、mapper、設定永続化、Worker / Schedulerを変更 | repository-implementation / datasource / mapper / worker / scheduler |
| `db-implementation` | Roomの保存構造、DAO query、保存値の互換性、migrationを変更 | entity / dao / migration |
| `test-implementation` | テストコードを追加・修正 | 専用referenceは作らず`testing.md`へ案内 |
| `implementation-review` | 実装差分をレビュー | 専用referenceは作らず`review.md`へ案内 |

`description`は利用者の依頼に現れる言葉から書き始め、担当する責務と、誤選択しやすい境界を示す。
テスト実行だけでTest Skill、文書に認証という語があるだけで認証実装手順、という選び方にしない。
Codexの`agents/openai.yaml`は6種類のSkillと整合させ、既存の有効なクライアント設定を確認して必要な範囲だけ更新する。

### 廃止する専用Skillの移行先

| 既存Skill | 知識・手順の移行先 |
| --- | --- |
| `auth-implementation` | `authentication.md`、各レイヤーSkillの参照条件、DataSource / mapper / workerのreference（device token登録を含む） |
| `media-sync-implementation` | `media-sync.md`、各レイヤーSkillの参照条件、worker / schedulerのreference |
| `ci-build-troubleshooting` | `ci.md`の調査手順と`development-setup.md`の環境・ローカル検証 |

移行先の案内と重要な制約が揃った後に、両クライアントの該当Skill本体・metadata・referenceを整理する。
専用Skillが持っていた情報を、ファイル数の削減だけを理由に消さない。

### SKILL.mdとreferenceの書き方

`SKILL.md`は、目的、変更対象と参照先の対応、判断が必要な手順、完了時の報告を中心にする。
全Skillを機械的に同じ長さや構成へ揃えず、短く完結するTest / Reviewにはreferenceを増やさない。

referenceは次を基本にする。

- 確認する実装・契約・テストへの参照。
- その役割で見落としやすいプロジェクト固有の注意。
- 変更時に確認する波及先。
- 必要なテスト判断と検証観点。

現在の実装例、必須の制約、選択可能な方式を区別する。
「DataSourceを1つだけ注入」「全overrideが単一式」といった実装の現状を、将来の全クラスを拘束するルールにしない。
共通のコーディング規約や検証コマンドを再掲せず、正本の該当節へ案内する。

## 機能文書に残す重要な契約

次は現行文書と実装を照合して移す。刷新によって意味を変えない。

- 認証providerの型と例外をdataに閉じる境界。
- アカウント列挙を避けるための認証エラー表現。
- サインアウト・退会でのFCM、backend、Cognitoの処理順と途中失敗。
- FCM device tokenの登録契機（アプリ起動時、認証済み状態への遷移時、`onNewToken`）。
- `RegisterDeviceTokenWorker`のenqueue契約。media系Workerの`KEEP`と異なり`REPLACE`で最新tokenを優先し、指数backoffを設定している。
- device token登録の失敗分類。`CancellationException`は再スローし、`retry`へ変換しない。不正なtokenと`AmplifyRestException.isNonServerError`（4xxなど5xx以外の失敗応答）は`failure`、それ以外の失敗は`retry`とする。
- `CancellationException`の再スローと、SDK操作をキャンセルできる経路・できない経路。
- `completeUpload()`をWorker実行単位で呼ぶ契約。
- S3アップロード、local保存、remote登録の順序と、途中失敗・削除予約の競合。
- 一時失敗と永続失敗の分類、およびretry時に残す状態。
- 未送信のpending状態をリモート同期で上書きしない契約。
- 未アップロードの削除と、S3削除失敗を孤立オブジェクトとして許容する既存の経路。
- unique work name、enqueue policy、constraintsなどの既存キューとの互換性。
- `SyncStatus`とDAO query・DB保存文字列の対応。
- schema exportやmigrationが未導入の部分と、導入時に必要になる判断。

## 作業例と参照経路

以下は移行後に確認するケースの設計であり、実際のSkill選択を評価済みという意味ではない。
テスト追加が必要な場合はTest Skillを併用する。

| 依頼例 | Skill | 条件付きで読む技術文書・reference |
| --- | --- | --- |
| ログイン画面のボタン間隔を変える | UI | screenとUI規約。認証フローの文書は通常不要 |
| 確認コード期限切れの表示とprovider mappingを変える | UI + Data | authentication、mapper、必要なUI state |
| 認証入力のvalidationを変える | Domain、UIに波及する場合はUIも | authentication、value-object / usecase |
| アップロードの一時失敗をretryする条件を変える | Data | media-sync、worker、必要なエラー規約 |
| SyncStatusを追加する | Domain + Data + DB、表示に波及する場合はUIも | media-sync、data-model、result-error / mapper / dao |
| DAOの取得順を変更する | DB、呼び出し側を変える場合はDataも | data-model、dao |
| ViewModelのFlowテストを追加する | Test | testing、対象ViewModelと既存テスト |
| PRの実装差分をレビューする | Review | review、比較対象に該当するarchitecture・機能文書 |
| CIだけでビルドが失敗する原因を調べる | 実装Skillは調査対象の判明後に選択 | ci、development-setup、失敗jobの実装 |
| AI文書の説明だけを整理する | アプリ実装Skillは不要 | 変更する文書とその参照元 |

## 現行文書との移行対応

| 現行 | 主な移行先 |
| --- | --- |
| `implementation-rules.md` | architecture、implementation-guidelines、authentication、media-sync |
| `android-conventions.md` | architecture、implementation-guidelines、UI / Dataのreference |
| `architecture-decisions.md` | architecture内の責務と理由、対応する機能文書 |
| `error-handling-guide.md` | implementation-guidelines、authentication、media-sync、datasource / mapperのreference |
| `testing-conventions.md` | testing、data-model、development-setup |
| `verification-policy.md` | development-setup、ci、review |
| `build-environment.md` | development-setup、ci |
| `media-upload-flow.md` | media-sync、data-model |
| 各Skillとreference | 新しい6種類のSkill、役割ごとのreference、共通技術文書 |
| READMEの日英AI文書案内 | 新しい入口・文書名に合わせたリンク |

### referenceの移行対応

現行referenceは34〜76行あり、新しいreferenceは「確認する対象 / 実装時の注意 / テスト判断 / 検証観点」に絞るため、内容の多くを技術文書へ移す。
次の表はファイル単位の移行先で、新しいreferenceへ残すのは役割固有の注意と波及先に限る。
`.claude/skills/`と`.agents/skills/`の同名ファイルは同じ内容のため、両側を同じ移行先とする。

| 現行reference | 新しいreference | 技術文書へ移す内容 |
| --- | --- | --- |
| UI `screen.md` | `ui/screen` | effectの購読は`ui/ui-event`、表示文字列の解決の基本は`implementation-guidelines.md` |
| UI `viewmodel.md` | `ui/viewmodel` | エラーハンドリングとJob管理の基本は`implementation-guidelines.md` |
| UI `uistate.md` | `ui/ui-state` | なし |
| UI `effect.md` | `ui/ui-event` | なし。effect / Snackbar message型 / 購読契約を現行の方式のまま記述する |
| なし | `ui/navigation` | `android-conventions.md`のNavigation節から作る |
| Domain `usecase.md` | `domain/usecase` | 作成基準は`architecture.md`。`DeleteMediaUseCase`の未アップロード分岐、`SyncUploadRecordsUseCase`のpending保護、`GetPendingMediaUseCase`の分割取得は`media-sync.md` |
| Domain `valueobject.md` | `domain/value-object` | なし |
| Domain `result-error.md` | `domain/result-error` | 例外とResultの使い分けは`implementation-guidelines.md` |
| Domain `repository-interface.md` | 作らない | 分割の方針と型の制約は`architecture.md`。interfaceの一覧は複製せずソースへ案内する |
| Data `repository-impl.md` | `data/repository-implementation` | 「同期の判断は置かない」は`architecture.md` |
| Data `datasource.md` | `data/datasource` | `CancellationException`の基本は`implementation-guidelines.md`、SDK操作をキャンセルできる経路は`authentication.md` / `media-sync.md`。`MediaStoreCapturedPhotoWriter`や`ContentTypeResolver`などの具体I/Oもこの役割に含める |
| Data `mapper.md` | `data/mapper` | 認証のprovider例外の分類は`authentication.md` |
| Media sync `worker.md` | `data/worker` | doWorkの処理順、永続失敗と一時失敗の分岐、`completeUpload()`の呼び出し単位は`media-sync.md`。`RegisterDeviceTokenWorker`（`app`配置、直接enqueue）も対象に含め、契約の詳細は`authentication.md`へ案内する |
| Media sync `scheduler.md` | `data/scheduler` | unique work name、`KEEP`、constraints、input Dataを渡さない理由は`media-sync.md`。Scheduler interfaceを介さない`RegisterDeviceTokenWorker.enqueue`との違いは`data/worker`と`authentication.md`で扱う |
| DB `entity.md` | `db/entity` | null許容列の意味は`data-model.md` |
| DB `dao.md` | `db/dao` | SyncStatus文字列、空・不在の扱い、削除の冪等性は`data-model.md` |
| DB `entity-mapper.md` | 作らない。`db/entity`の波及先に含める | 不明なstatusを`ERROR`へ落とす契約は`data-model.md` |
| DB `database.md` | `db/migration` | DB構成、`DatabaseModule`、version判断、`exportSchema = false`の現状は`data-model.md`、Gradle依存の分担は`development-setup.md`、テストでの組み立ては`testing.md` |
| Test `fixture.md` | 作らない | `testing.md`のfixture節。domainとdataで命名が異なる点を残す |
| Test `usecase-test.md` / `repository-test.md` / `mapper-test.md` / `valueobject-test.md` | 作らない | `testing.md`の対象別の節。suspend / Flow / 例外伝播のassertは共通節にまとめる |
| Test `viewmodel-test.md` | 作らない | `testing.md`のViewModel節。private Snapshot比較、effectのassert、二重実行ガードを残す |
| Test `worker-test.md` | 作らない | `testing.md`のWorker節。media系Workerと`RegisterDeviceTokenWorker`の両方を対象に含める |
| Test `instrumented-test.md` | 作らない | JUnit 4である点とDAO / Scheduler / DataStoreの書き方は`testing.md`、実行方法は`development-setup.md` |

`testing.md`は、現行の`testing-conventions.md`とTestのreference 8件を合わせて規約の本文を1回だけ書く。
テスト対象ごとの例やクラス紹介は、既存のテストへの参照に置き換える。

移行時は、残す契約と移行先の対応を確認し、移行先のない削除を個別に点検する。
単なるリンク置換で新旧の全文を残さず、完了時に通常の参照先を新構成へ統一する。

## 実装順序

1. **共通技術文書を執筆する（追加済み）。** 実装を照合し、現状・必須契約・将来変更時の判断を分けて10件へ再構成した。
2. **両クライアントのSkillを再編する。** 6種類のdescription、役割別案内、reference、Codex metadataを揃える。
3. **入口と参照元を切り替える。** 両入口、README、文書間リンクを更新し、移行済み文書と3種類の専用Skillを整理する。
4. **構造と選択動作を別々に確認する。** 機械的な整合性と、依頼に対して適切な文書へ到達するかを検証する。

各段階は移行順序を示す。途中状態を最終構成として扱わず、全体の参照と契約の移行を完了させる。

## 完了条件と検証

### 文書とSkillの構造

- 両クライアントに意図した6種類のSkillがあり、役割と共通部分が整合する。
- 自動同期を前提とせず、共通内容の変更を両側へ反映できている。
- Markdownのローカル参照、該当節、実装・テストの参照先が正しい。
- Skillのname / description / frontmatterとCodex metadataが有効である。
- 廃止したファイル・Skillへの参照や、移行で取り残した重要な契約がない。
- コーディング規約、機能契約、実装例、検証コマンドの所有先が明確である。
- `git diff --check`で差分の空白エラーがない。

### Skill選択と参照の動作

- 作業例について、期待するSkillと文書が選ばれるかを実際に確認する。
- 該当しない機能文書・全referenceを一律に読む指示になっていない。
- 認証・同期・DBの重要な変更で、必要な契約の文書が読み落とされない。
- Reviewで比較基準を確定でき、未コミット差分とコミット済みのブランチ差分を区別できる。
- 静的なfrontmatter検証をSkill選択精度の測定として報告しない。実施した方法と結果を区別する。

文書とSkillのみの変更にはGradleテストを要求しない。
検証用の実行がアプリや環境へ影響する場合は、その操作範囲を別途定める。

## 今回の作業範囲

今回はLite MemoとCloud Photosの文書・関連実装を読み取り、この設計書と共通技術文書10件を追加した。
入口と既存Skillの切り替え、旧文書の整理は後続の文書刷新作業で行う。アプリ実装の変更はこの文書刷新の対象外とする。
