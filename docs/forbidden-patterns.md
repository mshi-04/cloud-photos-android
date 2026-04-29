# 禁止パターン

このリポジトリで避けるべきパターンと、各パターンの簡単な理由。
ルールは `AGENTS.md` で強制されます。このファイルはエッジケースでの判断を助けるために「なぜか」を説明します。

---

## プロダクションコードに android.util.Log

```kotlin
// 禁止
Log.d("MediaRepo", "Uploading file: $path")
```

**なぜ**：logcatは、ロック解除されたデバイスでadbアクセスを持つどのプロセスからでも読み取り可能です。
ログ呼び出しはトークン、ファイルパス、ユーザー識別子を公開する可能性があります。
このリポジトリには構造化ログの抽象化がありません。ログを省略するか、意図を持って導入してください。

---

## 一時的なデバッグログのコミット

適切な抽象化を使ったログであっても、デバッグ専用の一時的な文はコミットしてはなりません。

**なぜ**：デバッグログはlogcatのS/N比を下げ、内部状態を公開する可能性があり、マージされると
ほとんどの場合クリーンアップされません。

---

## UIがdata実装を直接呼び出す

```kotlin
// ViewModel / Compose で禁止
@Inject lateinit var uploadRepositoryImpl: UploadRepositoryImpl
@Inject lateinit var mediaDao: MediaDao
```

**なぜ**：`ui` は `domain` インターフェースと `core:ui` / `core:common` にのみ依存すべきです。
data層への直接依存はユースケースをバイパスし、テスト可能性を損ない、UIを永続化やSDKの詳細に結合させます。

---

## domain に Android / Compose / Room / WorkManager / プロバイダーSDK

```kotlin
// feature:*:domain で禁止
import androidx.room.Entity
import com.amplifyframework.auth.AuthException
import androidx.work.WorkManager
```

**なぜ**：`domain` はフレームワークフリーに保たれ、ビジネスロジックが純粋なJVMテストでテスト可能でなければなりません。
domainのフレームワーク依存は、Androidデバイスやエミュレータなしにユースケースをテストすることを不可能にします。

---

## UIへのDTO、エンティティ、プロバイダーレスポンスの返却

```kotlin
// 禁止
fun getItems(): List<MediaEntity>         // Room エンティティをUIへ
fun getAuthState(): AuthSignInResult      // Amplify 型をUIへ
```

**なぜ**：data層のモデルは境界を越えて漏れるべきでない永続化やSDKの関心事をエンコードしています。
スキーマやSDKレスポンスの形式の変更はUIを壊します。境界でマッパーを使用してください。

---

## RepositoryImpl へのビジネスロジックの蓄積

```kotlin
// 禁止
class UploadRepositoryImpl {
    fun upload(file: File) {
        if (file.size > MAX_SIZE) throw ...   // ビジネスルール — domainに属する
        if (!networkAvailable()) return       // オーケストレーション — ユースケースに属する
        ...
    }
}
```

**なぜ**：`RepositoryImpl` はデータソースとマッパーへの委譲のみを行うべきです。
ビジネスルールはユースケースに属し、そこでテストしやすく重複も発生しません。

---

## 暗黙の大規模リファクタリング

**なぜ**：集中したタスク中の広範なパッケージ移動、大規模リネーム、フィーチャー横断の再構築は、
差分を読みにくくしマージコンフリクトを引き起こします。タスクオーナーが意図しないものや
承認していないものも変更してしまいます。大きな構造変更はサイドエフェクトではなく、
明示的な提案として提示してください。

---

## 無関係なクリーンアップ

それ以外で変更していないファイルのフォーマット修正、変数リネーム、インポート整理。

**なぜ**：触れたファイルはすべて潜在的なマージコンフリクトとレビュー負担になります。
クリーンアップコミットは分離し、明示的で意図的であるべきです。

---

## ハードコードされたシークレット、エンドポイント、バケット名、クライアントID

```kotlin
// 禁止
private const val API_URL = "https://api.example.com"
private const val COGNITO_CLIENT_ID = "abc123"
```

**なぜ**：ハードコードされた値はバージョン履歴に残り、パブリックフォークで公開される可能性があり、
コード変更なしにローテーションできません。プロジェクトのフレーバーベースの設定メカニズムを使用してください。

---

## CancellationException の握り潰し

```kotlin
// 禁止
runCatching {
    someCoroutine()
}.onFailure { /* 何もしない */ }
```

**なぜ**：`CancellationException` はKotlinコルーチンが協調的なキャンセルを通知する方法です。
それを握り潰すと、コルーチンスコープのクリーンなキャンセルが阻害され、
コルーチンリークとキャンセルチェーンの破損を引き起こします。

必ず再スローすること：

```kotlin
}.onFailure { e ->
    if (e is CancellationException) throw e
    // その他のエラーをハンドリング
}
```

---

## 軽率なフィーチャー横断依存

```kotlin
// feature:media で禁止
import com.appvoyager.cloudphotos.feature.auth.domain.usecase.GetCurrentUserUseCase
```

**なぜ**：フィーチャーは独立してデプロイ・テスト可能であるべきです。フィーチャー間の直接インポートは、
フィーチャーのテスト、リファクタリング、または削除を困難にする結合を生み出します。
共有コントラクトは兄弟フィーチャーの内部ではなく `core:common` に属します。
