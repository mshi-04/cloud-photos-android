# アーキテクチャ決定

この文書は、ルールの背景を説明します。実装判断は `AGENTS.md` と各規約文書を優先します。

## `app / core / feature`

決定: アプリ配線、共有コード、フィーチャーをモジュールで分ける。

理由:

- Gradleモジュールで依存方向を制約できる。
- 変更影響を絞れる。
- テスト対象を絞れる。
- フィーチャー追加時に無関係な依存を避けられる。

避けるもの:

- すべてを `app` に集める。
- 実利用のない便利コードを `core` に集める。

## `ui / domain / data`

決定: Android公式のUI layer、optional domain layer、data layerをフィーチャーモジュールに対応させる。

理由:

- UIは状態描画とevent発行に集中できる。
- domainはAndroidなしでテストできる。
- dataはSDK、DB、network、cacheを閉じ込められる。
- provider変更がUI/domainへ波及しにくい。

避けるもの:

- ViewModelがDAOやSDK clientを直接呼ぶ。
- UseCaseがWorkManagerやAmplifyをimportする。
- Entity/DTOがUI stateに入る。

## 単方向データフロー

決定: UIから状態更新までの流れを一方向にする。

```text
UI event -> ViewModel -> UseCase/Repository -> UI state/effect -> UI
```

理由:

- 状態遷移を追いやすい。
- Composeの再描画モデルと合う。
- ViewModelテストで画面契約を検証しやすい。

## UseCase

決定: domain rule、複数Repository調整、再利用操作をUseCaseで表す。

理由:

- ViewModelを状態管理へ集中させられる。
- domainロジックをJVMテストできる。
- 操作の意味が名前として残る。

避けるもの:

- すべてのRepositoryメソッドを機械的にUseCase化する。
- Android/SDK/DB型をUseCaseへ入れる。

## Value Object

決定: `Email`、`Password`、`UserId` など検証済み概念をvalue objectで表す。

理由:

- 生プリミティブでは制約を表現できない。
- 無効値の伝播を防げる。
- APIの意味が型で分かる。

## Provider隔離

決定: Cognito、Amplify、Firebase、Room、WorkManager、HTTP実装はdataに閉じ込める。

理由:

- providerは変更され得る。
- SDK例外やmodelがdomain/UIへ漏れると移行コストが上がる。
- mapper境界でdomain contractを安定させられる。

## WorkManager分離

決定: mediaのバックグラウンド処理をWorker、Scheduler、Repository、DataSourceへ分ける。

理由:

- WorkManagerは永続的に完了させたい処理に向く。
- enqueue条件と実処理を分けるとテストしやすい。
- retry、constraints、SyncStatus、DB/API整合性を個別に検証できる。

## main-safe suspend

決定: suspend APIはメインスレッドから呼ばれても安全にする。

理由:

- 呼び出し側がDispatcher詳細を知らずに済む。
- ViewModelとUseCaseが読みやすくなる。
- テストでDispatcherを差し替えやすい。

## ログ制限

決定: プロダクションコードへ `android.util.Log` を直接残さない。

理由:

- token、user id、path、URL、bucketなどの漏洩リスクがある。
- 診断ノイズが増える。
- 現状、統一ログ抽象化がない。

## 検証スコープ

決定: 変更が壊し得る最小範囲を検証し、境界を越える変更では広げる。

理由:

- 小変更に毎回フル検証を要求すると開発速度が落ちる。
- 境界変更を狭いテストで済ませると回帰を見逃す。
- CIを最終ゲートにしつつ、ローカルでも意味のある確認を行う。
