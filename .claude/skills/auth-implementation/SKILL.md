---
name: auth-implementation
description: 認証機能の設計・実装・修正に使う。Cognito/Amplify Auth、AuthRepository、AuthDataSource、AuthErrorMapper、AuthResult、ログイン、サインアップ、確認コード、パスワードリセット、サインアウト、退会、FCM token cleanup を触る作業で使用する。
---

# Auth Implementation

## 実装手順

1. 変更が auth UI、domain contract、data provider 連携のどこに入るか決める。
2. UI 変更では既存 ViewModel の入力 validation、loading state、field error、effect emission の流れを確認してから screen/state/effect を更新する。
3. Domain 変更では `AuthRepository`、`AuthResult`、`AuthError`、request、value object を確認し、UI/data に漏れない安定した契約を先に決める。
4. Data 変更では `AuthRepositoryImpl`、`AuthDataSourceImpl`、`AuthErrorMapper`、`AuthSignInStepMapper` を確認し、Cognito/Amplify の型と例外を mapper 境界で domain へ変換する。
5. サインアウトや退会を変更する場合は、FCM token cleanup、backend deletion、Cognito deletion の順序と失敗時の戻り値を明示してから実装する。
6. callback bridge、`runCatching`、cleanup 処理を触る場合は、`CancellationException` の再スロー経路を実装に含める。
7. アカウント列挙に関わるエラー表現を変える場合は、UI 表示、domain error、provider mapping の全経路を確認する。

## 注意点

- Cognito/Amplify の型と例外を data から出すと、認証基盤の差し替えが UI まで波及する。
- アカウント列挙を避けるために統合されているエラーを分解すると、メールアドレスが登録済みかどうかを画面から推測できてしまう。
- 退会は FCM token 削除、backend データ削除、Cognito アカウント削除の順序に意味がある。途中失敗時の戻り値を曖昧にすると、消え残りが利用者から見えない。
- callback bridge や cleanup の `runCatching` で `CancellationException` を握り潰すと、画面を離れた後も処理が続く。
- validation、UseCase 実行、UI effect を同じ関数へ混ぜると、入力エラーと通信エラーの区別が画面から失われる。

## 検証手順

1. domain 契約変更は `./gradlew :feature:auth:domain:test` を実行する。
2. mapper、repository、data source 変更は `./gradlew :feature:auth:data:test` を実行する。
3. ViewModel や画面契約変更は `./gradlew :feature:auth:ui:test` を実行する。

## 参考資料

- [docs/implementation-rules.md](../../../docs/implementation-rules.md)
- [docs/error-handling-guide.md](../../../docs/error-handling-guide.md)
- [docs/android-conventions.md](../../../docs/android-conventions.md)
- [docs/architecture-decisions.md](../../../docs/architecture-decisions.md): value object と provider 隔離を選んでいる理由
