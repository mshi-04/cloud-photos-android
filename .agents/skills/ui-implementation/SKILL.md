---
name: ui-implementation
description: 画面の表示、操作、画面遷移、表示文言、UI state、Snackbar・ダイアログの振る舞いを変更する依頼で使う。Cloud Photos の UI 層（app、core:ui、feature:*:ui）の Compose Screen / Content、ViewModel、UiState、Effect、Navigation、strings.xml を担当する。ドメインの業務ルール・入力検証や Data 層の通信・永続化だけの変更は対象外。
---

# 目的

Cloud Photos の UI 層（`app`、`core:ui`、`feature:auth:ui`、`feature:media:ui`）を、既存の Compose / MVVM 構成とレイヤー境界に沿って変更する。

# 条件別の参照先

全技術文書を一律に読まず、変更内容に該当する文書・節だけを確認する。

- モジュール境界・ViewModel の依存先（UseCase と `CapturedPhotoWriter` などの domain abstraction の使い分け）を判断するとき: [`docs/architecture.md`](../../../docs/architecture.md) の「モジュール境界と依存方向」「UI 層と単方向データフロー」「UseCase と Domain Abstraction」
- Compose、ViewModel、coroutines、Resource / テーマ、キャンセル伝播の規約を確認するとき: [`docs/implementation-guidelines.md`](../../../docs/implementation-guidelines.md) の該当節
- 認証・アカウント操作のエラー出し分け（アカウント列挙防止を含む）やセッション状態遷移に関わるとき: [`docs/authentication.md`](../../../docs/authentication.md)
- メディア一覧の同期契機、権限復帰時の再同期、撮影保存フローに関わるとき: [`docs/media-sync.md`](../../../docs/media-sync.md)
- 対象モジュールの実装とテスト:
  - [`app/src/main/kotlin/com/appvoyager/cloudphotos/`](../../../app/src/main/kotlin/com/appvoyager/cloudphotos/)
  - [`core/ui/src/main/`](../../../core/ui/src/main/)
  - [`feature/auth/ui/src/main/`](../../../feature/auth/ui/src/main/) / [`feature/auth/ui/src/test/`](../../../feature/auth/ui/src/test/)
  - [`feature/media/ui/src/main/`](../../../feature/media/ui/src/main/) / [`feature/media/ui/src/test/`](../../../feature/media/ui/src/test/)

# 役割クラスと参照先

変更対象の役割を見分け、該当する reference だけを読んでから実装する。複数に跨る場合は関連する reference を併読する。

| 役割 | 参照 |
| --- | --- |
| Screen / Content / Preview | [`references/screen.md`](references/screen.md) |
| ViewModel | [`references/viewmodel.md`](references/viewmodel.md) |
| UiState / FieldError | [`references/ui-state.md`](references/ui-state.md) |
| UI event（callback / Effect / Snackbar message） | [`references/ui-event.md`](references/ui-event.md) |
| Navigation（`NavGraph` / route 定義） | [`references/navigation.md`](references/navigation.md) |
| テーマ・共通 UI・strings.xml のみの変更 | reference なし。[`docs/implementation-guidelines.md`](../../../docs/implementation-guidelines.md) の「Compose」「Resource / Theme」に従う |

# 手順

1. 変更対象の役割を判定し、該当する reference と必要な技術文書の節だけを読む。
2. `feature:*:ui -> feature:*:domain` および `app`（composition root）の依存方向を保つ。
3. reference の実装時の注意と波及先に沿って変更する。
4. ViewModel の状態遷移・effect・エラー分岐を変更した場合は [`test-implementation`](../test-implementation/SKILL.md)（および [`docs/testing.md`](../../../docs/testing.md) の「対象別のテストの書き方」の ViewModel 節）に沿ってテストを追加・更新する。
5. 変更範囲に応じたローカル検証を [`docs/development-setup.md`](../../../docs/development-setup.md) の「ローカル検証の選び方とコマンド」から選んで実行する。
6. 変更内容、実行した検証と結果、未実施の検証と理由を簡潔に報告する。

# 注意事項

- `feature:*:ui` から `feature:*:data`、Room Entity、DAO、Amplify SDK 型、Repository 実装を直接参照しない。
- UI 変更が Domain（入力制約・UseCase・Result/Error 型）や Data（ErrorMapper・DataSource）へ波及する場合は、[`domain-implementation`](../domain-implementation/SKILL.md) や [`data-implementation`](../data-implementation/SKILL.md) を併用する。
- ViewModel に `Context` や `R.string` を持ち込まず、表示文字列への変換は UI 層（Screen ファイルの拡張関数など）で行う。
