---
name: ui-implementation
description: UI 層の設計・実装・修正に使う。Compose screen/component、ViewModel、UI state/effect/event、Navigation、Camera/Media UI、表示文字列、core:ui テーマや共有 UI を触る作業で使用する。
---

# UI Implementation

## 実装手順

1. 変更対象が `feature:<name>:ui`、`app`、`core:ui` のどこに入るか決める。
2. 既存の Screen、Content、ViewModel、UI state/effect/event の分け方を確認し、同じ単方向データフローに沿って変更点を置く。新規画面や新規 state/effect を足す場合は、参考資料の該当種別を読んでから形を決める。
3. Composable では表示に必要な state と event callback を受け取り、永続状態や通信状態は ViewModel/domain/data 側へ流す。
4. ViewModel では UseCase または domain abstraction を呼び、画面に見える state/effect を更新する。DAO、SDK、Repository 実装、Worker が必要になったら対象レイヤーの skill に切り替える。
5. Navigation、snackbar、permission request、カメラ起動などの一回限りの動作は既存 effect 型に追加するか、既存 effect の使い方に合わせる。
6. 表示文字列、テーマ、共有 UI component を追加する場合は、既存 resource と `core:ui` の利用状況を確認して最小の配置にする。
7. 認証 UI は `auth-implementation`、media grid/camera/detail/同期表示は `media-sync-implementation` も併用する。

## 注意点

- 永続状態や通信状態を Composable が持つと、結果が再コンポジションのタイミングに依存し、ViewModel テストから画面契約を確認できなくなる。`SnackbarHostState` や権限の再確認キーのような画面限定の一時状態は `remember` で持ってよい。
- 一回限りの動作を state として持つと、再コンポジションや画面回転で再実行され、二重遷移や通知の重複になる。
- `core:ui` は共有先が増えるほど変更の波及範囲が広がる。汎用的に見えるという理由では選ばない。
- 表示文字列やテーマ値を直書きすると、ローカライズとテーマ切り替えの経路から外れる。
- ViewModel テストで UseCase 呼び出しだけを確認しても、画面が実際に何を出すかは検証できない。
- `app` の NavGraph、start destination、認証状態遷移は他フィーチャーの入口を変えるため、影響が単一モジュールに収まらない。

## 検証手順

1. UI contract が変わった場合は ViewModel テストで state/effect を確認する。
2. 単一 UI モジュール変更は `./gradlew :feature:<name>:ui:test` を実行する。
3. `app` の NavGraph、start destination、認証状態遷移に触れた場合は `./gradlew test` も検討する。

## 参考資料

このリポジトリでの書き方。対象の種別だけ読む。

- [references/screen.md](references/screen.md): Screen と Content の分割、effect の購読
- [references/viewmodel.md](references/viewmodel.md): ViewModel の骨格、Flow 購読、Job 管理
- [references/uistate.md](references/uistate.md): UI state の形
- [references/effect.md](references/effect.md): effect の形と公開方法

規約:

- [docs/implementation-rules.md](../../../docs/implementation-rules.md)
- [docs/android-conventions.md](../../../docs/android-conventions.md)
- [docs/testing-conventions.md](../../../docs/testing-conventions.md)
- [docs/architecture-decisions.md](../../../docs/architecture-decisions.md): 単方向データフローや provider 隔離を選んでいる理由
