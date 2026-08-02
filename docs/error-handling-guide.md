# エラーハンドリングガイド

例外、Result型、provider error mapping、coroutine cancellationの扱いです。

## 原則

- `CancellationException` を握り潰さない。
- 想定される失敗はdomain result/errorで表す。
- provider固有の例外はdata層でdomain errorへ変換する。
- UIはdomain errorまたはUI stateだけを見る。
- 同じAPIでResultを返しつつ、通常失敗を例外でも投げる形にしない。

## CancellationException

coroutineのキャンセルは `CancellationException` で伝播します。
catchした場合は必ず再スローします。

```kotlin
try {
    operation()
} catch (e: CancellationException) {
    throw e
} catch (e: IOException) {
    // expected recoverable error
}
```

generic catchでも同じです。

```kotlin
try {
    operation()
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    // map or handle
}
```

## runCatching

`runCatching` は `CancellationException` も捕まえます。

```kotlin
runCatching {
    operation()
}.onFailure { e ->
    if (e is CancellationException) throw e
}
```

ネストした場合は、それぞれの `onFailure` で再スローします。

```kotlin
runCatching {
    outer()
}.onFailure { outer ->
    if (outer is CancellationException) throw outer

    runCatching {
        cleanup()
    }.onFailure { inner ->
        if (inner is CancellationException) throw inner
    }
}
```

## Result型と例外の使い分け

Result型を使う:

- domain上の予期される失敗。
- UIが成功/失敗をstateへ反映する。
- data/domain境界を越える。
- Workerがretry/failure/successを判断する。

例外を使う:

- プログラミングエラー。
- 契約違反。
- 回復不能な初期化失敗。

## Provider mapping

data層でprovider例外をdomain errorへ変換します。

```kotlin
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    UploadResult.Error(UploadErrorMapper.map(e))
}
```

domain/UIへ漏らさないもの:

- Amplify exception
- Cognito exception
- Firebase exception
- Room exception
- HTTP client固有型
- SDK response model

## Auth

- Cognito/Amplifyエラーは `feature:auth:data` のmapperで変換する。
- アカウント列挙を避けるために統合されたエラーは不用意に分解しない。
- UIはproviderの失敗理由ではなく、domain/UI向けの状態を見る。

## Media Worker

| 条件 | 動作 |
|---|---|
| `CancellationException` | 再スロー |
| 一時的エラー | 状態を維持して `Result.retry()` |
| 永続的エラー | `ERROR` へ更新しretryしない |
| 削除時の既存孤立許容ケース | 既存仕様に従い処理継続 |

分類を変更する場合は [docs/media-upload-flow.md](media-upload-flow.md) とテストを更新します。

## テスト観点

- `CancellationException` は `rethrows` で確認する。
- provider例外がdomain errorへ変換されることを確認する。
- Workerは一時/永続エラーで戻り値と状態更新を確認する。
