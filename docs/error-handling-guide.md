# エラーハンドリングガイド

このドキュメントはこのリポジトリで使用されるエラーハンドリングパターンを説明します。
ルールは `AGENTS.md` にあります。このファイルはそれらを正しく適用する方法を説明します。

---

## CancellationException は必ず再スローしなければならない

Kotlinコルーチンは `CancellationException` によって協調的なキャンセルを通知します。
それを握り潰すと、コルーチンスコープのクリーンなキャンセルが阻害され、コルーチンリークが発生し、
構造化された同時実行が壊れます。

**`Exception` または `Throwable` を処理するすべての `catch` ブロックは、`CancellationException` を再スローしなければなりません。**

### try/catch の場合

```kotlin
try {
    someCoroutine()
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    // ハンドリング
}
```

### runCatching の場合

`runCatching` は `CancellationException` を含むすべての `Throwable` をキャッチします。
`onFailure` 内で必ず再スローすること：

```kotlin
runCatching {
    someCoroutine()
}.onFailure { e ->
    if (e is CancellationException) throw e
    // その他のエラーをハンドリング
}
```

### ネストされた runCatching

各ネストされた `runCatching` ブロックは独立して再スローしなければならない：

```kotlin
runCatching {
    outer()
}.onFailure { e ->
    if (e is CancellationException) throw e
    runCatching {
        cleanup()
    }.onFailure { inner ->
        if (inner is CancellationException) throw inner
    }
}
```

内側の再スローを忘れるのはよくあるミスです。外側の再スローは、ネストされたブロック内で
握り潰されたキャンセルを保護しません。

---

## エラーマッピングはdata層に属する

ドメインのユースケースとドメインモデルは、プロバイダー固有のエラー型
（Amplify例外、Cognitoエラー、Room例外など）を参照してはなりません。

### パターン

1. データソースがプロバイダー固有の例外をキャッチする。
2. マッパーオブジェクトがそれらをドメイン向けのエラー型に変換する。
3. リポジトリ実装がドメインエラーを返す。

```kotlin
// data層
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    UploadResult.Error(UploadErrorMapper.map(e))  // ドメイン向けエラー
}

// domain層 — プロバイダー型は見えない
sealed class UploadError {
    data object NetworkError : UploadError()
    data object StorageError : UploadError()
    data object Unknown : UploadError()
}
```

---

## 例外よりシールドリザルト型を優先する

`media` フィーチャーはドメイン境界での操作結果を表現するために `UploadResult<T>` を使用します：

```kotlin
sealed class UploadResult<out T> {
    data class Success<T>(val value: T) : UploadResult<T>()
    data class Error(val error: UploadError) : UploadResult<Nothing>()
}
```

以下の場合にシールドリザルト型を例外よりも優先する：

- 呼び出し元が成功と失敗の両方のパスを処理しなければならない
- エラーがドメインレベルで予期された結果である（プログラミングエラーではない）
- 操作がdata/domain境界を越える

同じ操作に対してシールドリザルトを返しながら例外もスローするのは避けること。

---

## ワーカーにおける永続的エラーと一時的エラー

`UploadMediaWorker` と `DeleteMediaWorker` はリトライ動作を決定するためにエラーを分類します。
この分類は `isPermanentFailure()` などのプライベートヘルパーに置かれます。

| 条件                                     | 動作                                                     |
|------------------------------------------|----------------------------------------------------------|
| `CancellationException`                  | 即座に再スロー                                            |
| 永続的なエラー（例：HTTP 4xx）           | レコードを `ERROR` としてマーク、リトライしない            |
| 一時的なエラー（例：ネットワーク、HTTP 5xx） | リトライフラグを設定、最後に `Result.retry()` を返す   |

この分類を変更する場合は、`docs/media-upload-flow.md` に記載されたSyncStatus遷移との
整合性を検証すること。

---

## 認証エラーのマッピング

認証プロバイダーの例外（Cognito/Amplify）は `feature/auth/data` のマッパーで変換しなければなりません。
auth固有のガイダンスは `.agent/skills/android-auth-error/SKILL.md` を参照。

Cognito固有のエラー型をdomainやUIに返してはならない。
アカウント列挙を避けるためにプロバイダーエラーを意図的に統合しているマッピングは保持すること。
