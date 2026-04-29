# Android規約

Android実装時の具体ルールです。配置判断は `docs/implementation-rules.md` を優先します。

## Compose

- ScreenはViewModelと接続する入口、Contentは可能な限りstatelessな描画関数にする。
- Composableはstateを受け取り、event callbackを呼ぶ。
- State hoistingを優先し、再利用部品は `value` と `onValueChange` または明示的なeventを受け取る。
- `remember` はUI一時状態に限定する。画面状態、通信状態、永続状態はViewModelやdomain/dataへ置く。
- UI stateは単一のdata classまたは既存contractで表す。
- 一回限りのNavigation、toast/snackbar、permission requestは既存effectパターンに合わせる。
- `stringResource()` を使い、表示文字列を直書きしない。
- Previewを追加する場合は既存Previewと `CloudPhotosTheme` の使い方に合わせる。
- Lazy系や画像/動画表示では、不要なstate readや重い変換をComposable内に置かない。

## ViewModel

- UI stateを公開し、UI eventを受け取る。
- UseCaseまたはdomain abstractionを呼ぶ。
- `viewModelScope` 内では想定される例外を処理し、`CancellationException` は再スローする。
- ViewModelへdomain ruleを移さない。
- UI formattingは置いてよいが、DTO/Entity/SDK model変換はdata層へ置く。
- Repository interfaceを直接呼ぶ場合は、UseCaseを省く理由が既存パターンと整合していること。

## Coroutines / Flow

- suspend関数はmain-safeにする。
- Dispatcherを直接ハードコードしない。既存のDispatcher注入パターンがあれば使う。
- `Dispatchers.IO` への切り替えは、ブロッキングI/OやSDK呼び出しを実際に持つ層で行う。
- `StateFlow` は画面状態、effect用Flowは一回限りの動作に使い分ける。
- Flowは収集側のライフサイクルを意識し、ViewModelの外で無期限収集を作らない。
- `CancellationException` はcatchしない。catchした場合は必ず再スローする。

## Hilt / DI

- Constructor injectionを優先する。
- interface binding、SDK client、Room、DataStore、外部生成が必要なものだけmoduleに置く。
- `app` はアプリ全体の組み立て、フィーチャー固有bindingはフィーチャー側の既存配置を優先する。
- `@InstallIn` とscopeはライフサイクルに合わせる。
- 無関係なbindingを巨大moduleへまとめない。

## Navigation

- トップレベルNavGraphは `app` に置く。
- 画面内部の状態、event、effectはフィーチャーに置く。
- route知識を無関係なUI componentへ広げない。
- 認証状態、start destination、back stackを変える場合は影響を報告する。

## WorkManager

WorkManagerは、アプリが画面外になっても、プロセス終了や端末再起動をまたいでも完了させたい作業に使います。

使う場面:

- メディアアップロード/削除
- サーバー同期
- 一定条件下で確実に実行したいバックグラウンド処理

使わない場面:

- 画面を離れたら止まってよい処理
- UI操作直後の短い非同期処理
- 正確な時刻に鳴らすアラーム

mediaでは既存のWorker/Scheduler構造を維持します。

- unique work名、enqueue policy、constraints、tag、input/output Dataを不用意に変えない。
- retry/backoffを変える場合は、SyncStatusとテストも確認する。
- long-running workとしてforeground化が必要になる変更は、通知設計も含めて扱う。

## ログ

- プロダクションコードに `android.util.Log` を直接残さない。
- token、user id、path、URL、bucket、署名付きURLをログへ出さない。
- 一時デバッグログをコミットしない。
- ログが必要な場合は、構造化ログ方針を別作業として扱う。

## Resource / UI

- 文字列はresourceへ置く。
- 色、typography、shapeはthemeを優先する。
- Material 3の既存componentを優先する。
- 共通UI componentは複数フィーチャーで実利用がある場合だけ `core:ui` に置く。
