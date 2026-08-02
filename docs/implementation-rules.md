# 実装ルール

コードの置き場所と依存方向を決める文書です。

## レイヤー対応

| Android公式の層 | このリポジトリの配置 |
|---|---|
| UI layer | `feature:<name>:ui` |
| Domain layer | `feature:<name>:domain` |
| Data layer | `feature:<name>:data` |
| App composition | `app` |
| Shared concerns | `core:*` |

依存方向は `ui -> domain <- data` を基本にします。`app` は組み立て役です。

## 配置判断

| 変更内容 | 置き場所 |
|---|---|
| Compose screen/component | `feature:<name>:ui` |
| ViewModel、UI state/effect/event | `feature:<name>:ui` |
| UseCase、Repository interface | `feature:<name>:domain` |
| Domain model、value object、domain error/result | `feature:<name>:domain` |
| Repository implementation | `feature:<name>:data` |
| DTO、Entity、DAO、DataSource、Mapper | `feature:<name>:data` |
| Room、DataStore、WorkManager、SDK統合 | `feature:<name>:data` |
| 起動、トップレベルNavigation、アプリ全体DI、Manifest、flavor | `app` |
| 複数フィーチャーで共有する契約 | `core:common` |
| 複数フィーチャーで共有するdata infrastructure | `core:data` |
| Theme、共有resource、共有UI component | `core:ui` |
| Gradle convention | `build-logic` |

`core` は最後の選択肢です。単に便利、汎用的、将来使いそう、という理由では移動しません。

## app

`app` はコンポジションルートです。

置くもの:

- `Application`、`Activity`
- トップレベルNavGraph
- アプリ全体のHilt配線
- Manifest、flavor、初期化処理

置かないもの:

- フィーチャー固有のビジネスルール
- Repository実装の詳細
- DAO/DataSource/Worker本体
- フィーチャー固有のUI state/effect

## domain

`domain` はAndroidから独立したビジネス境界です。

置くもの:

- UseCase
- Repository interface
- Domain model
- Value object
- Domain result/error
- 純粋なvalidation

禁止:

- Android framework型
- Compose API
- Room API
- WorkManager API
- Amplify/Firebase/Cognito/S3などのSDK型
- Repository/DataSourceの具体実装

UseCaseはmain-safeにします。重いCPU処理やブロッキングI/Oが必要なら、責任を持つ層でDispatcherを切り替えます。

## data

`data` は外部世界との接続を閉じ込めます。

置くもの:

- Repository implementation
- Local/Remote DataSource
- DTO、Entity、DAO
- Mapper
- Room、DataStore
- Worker、Scheduler
- SDK/API client連携

ルール:

- DTO、Entity、SDK modelをdomain/UIへ漏らさない。
- provider例外はdomain向けerrorへmapする。
- Repository実装に画面都合の分岐を入れない。
- ビジネスルールが増えたらUseCaseへ移す。
- `CancellationException` を握り潰さない。
- Room DB versionを上げる変更では、migration方針を先に決める。[docs/testing-conventions.md](testing-conventions.md) のRoom migrationを読む。

## ui

`ui` は状態駆動の画面を所有します。

置くもの:

- ViewModel
- UI state/effect/event
- Compose screen/component
- 表示用formatting

ルール:

- Composableはstateを受け取りeventを返す。
- ViewModelはUseCaseまたはdomain abstractionを呼ぶ。
- UIからDAO、SDK、Repository実装、Workerを直接呼ばない。
- UI stateにDTO/Entity/SDK modelを入れない。
- Navigationやsnackbarなどの一回限りの動作は既存effectパターンに合わせる。

## UseCase

UseCaseを作る基準:

- 複数Repositoryを調整する。
- 複数画面/複数ViewModelから再利用される。
- domain ruleを名前付き操作として表したい。
- data操作の前後にvalidationや状態遷移が必要。

避けるもの:

- 単純なRepositoryメソッドの無意味な1対1ラップ。
- Android/SDK/DB型を受け取るUseCase。
- UI都合のformatting。

## Repository

- interfaceはdomain、implementationはdata。
- Repositoryはデータ取得、保存、同期、キャッシュ、競合解決の境界です。
- provider固有の型や例外はdata内で閉じます。
- UIに都合のよい文字列加工はRepositoryへ入れません。

## Mapper

- DTO/Entity/SDK modelとdomain modelの変換はdataに置く。
- provider例外からdomain errorへの変換もdataに置く。
- 同じ変換をRepositoryやDataSourceに分散させない。

## Value Object

検証済みdomain概念には `@JvmInline value class` を優先します。

- constructorはprivate。
- `of(raw: ...)` で生成する。
- `of()` 内でtrimやvalidationを行う。
- 既存value objectを生プリミティブに戻さない。
- 置き場所は該当domainモジュールの `valueobject/` を優先する。

## auth固有

- `Email`、`Password`、`UserId`、token/code系のvalue objectを尊重する。
- Cognito/Amplify型はdataから外へ出さない。
- アカウント列挙を避けるために統合されたエラー表現を不用意に分解しない。
- 入力validation、UseCase実行、UI effectを分離する。

## media固有

旧 `settings` は `media` に統合済みです。

- 設定domain contract/usecase/value objectは `feature:media:domain`。
- 設定永続化、DataStore、Repository実装は `feature:media:data`。
- 設定UIは `feature:media:ui`。

Worker/Scheduler/SyncStatusに触れる場合は [docs/media-upload-flow.md](media-upload-flow.md) を読む。

## コメント

字面から読み取れる説明をコメントへ書きません。
説明を書きたくなったら、コードで表現できていない合図として扱います。
コードでは名前、値オブジェクト、早期リターン、関数分割で意図を表します。
設定ファイルではkey、job名、step名、task名で意図を表します。

字面から読み取れない事情に限り書きます。

- 呼び出し側から見えない並行性の制約（排他制御、実行順序、冪等性）
- 不具合に見える意図的な挙動、外部都合のworkaroundの理由
- ツールや法的要件が要求するもの（警告抑制の理由、ライセンス表記）
- 依存のpinに添えるバージョン注記

背景、トレードオフ、採用しなかった案は、PRの説明か [docs/](../docs/) に書き、コードへ残しません。

対象はテストコードを除くすべてのファイルです。
GitHub Actionsのworkflow、Gradleスクリプト、その他の設定ファイルを含みます。
テストのAAAラベルと観点コメントは [docs/testing-conventions.md](testing-conventions.md) に従います。

既存ファイル全体の一括整理は、意図した移行作業でない限り行いません。

## 禁止パターン

- `domain` が `data` に依存する。
- UIがDAO、SDK、Repository実装、Workerを直接呼ぶ。
- DTO/Entity/SDK modelをdomain/UIへ公開する。
- `RepositoryImpl` にビジネスルールを蓄積する。
- `catch (Exception)` や `runCatching` で `CancellationException` を握り潰す。
- 実利用のない共有化を `core` に入れる。
- 一時ログや `android.util.Log` をプロダクションコードに残す。
