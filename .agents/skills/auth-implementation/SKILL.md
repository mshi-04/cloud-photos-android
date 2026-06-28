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

## 検証手順

1. domain 契約変更は `./gradlew :feature:auth:domain:test` を実行する。
2. mapper、repository、data source 変更は `./gradlew :feature:auth:data:test` を実行する。
3. ViewModel や画面契約変更は `./gradlew :feature:auth:ui:test` を実行する。

## 参考資料

- [docs/implementation-rules.md](../../../docs/implementation-rules.md)
- [docs/error-handling-guide.md](../../../docs/error-handling-guide.md)
- [docs/android-conventions.md](../../../docs/android-conventions.md)
