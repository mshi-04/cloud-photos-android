# 実装ルール

`AGENTS.md` から参照されます（すべてのルールの情報源）。
情報源の優先順位：`AGENTS.md` → フィーチャーローカルパターン → `CLAUDE.md` → `.agent/skills/*`。
このファイルと `AGENTS.md` が矛盾する場合は `AGENTS.md` を優先すること。

---

## ユースケース

- `suspend operator fun invoke()` を優先する。
- 各ユースケースは単一の目的に留める。
- ユースケースはドメイン処理をオーケストレーションし、Android/フレームワークの関心事を持たない。

## リポジトリ実装

- リポジトリ実装はデータソースとマッパーへの委譲のみを行う。
- ロジックが本質的にデータの合成/変換に関するものでない限り、`RepositoryImpl` にビジネスロジックを蓄積しない。

## エラーハンドリング

- エラーマッピングは専用のマッパーオブジェクト経由でdata層に属する。
- コルーチンフローや `runCatching` の使用では、`CancellationException` を必ず再スローする。
- キャンセルを握り潰さない。
- パターンと例は `docs/error-handling-guide.md` を参照。

## 値オブジェクト

- バリデーション済みドメイン概念には `private constructor` を持つ `@JvmInline value class` を優先する。
- `companion object { fun of(raw: ...) }` を通じてインスタンス化する。
- `of()` 内で `require()` を使用してバリデーションする。
- 適切な場合は、バリデーション前に文字列入力をトリムする。
- 確立された値オブジェクトパターンが存在するバリデーション済みドメイン概念に対して生プリミティブを使用しない。
- 値オブジェクトは対応する `feature:<name>:domain` モジュールの `valueobject/` パッケージに配置する
  （例：`feature/auth/domain/src/main/kotlin/.../auth/valueobject/`）。

## フィーチャー固有のルール

フィーチャーローカルのガードレールはそれぞれの `feature/<name>/AGENTS.md` ファイルに記載されています。
そのフィーチャーに触れる前に関連ファイルを読むこと：

- `feature/auth/AGENTS.md` — Cognito変換、auth値オブジェクト、authステップ分岐
- `feature/media/AGENTS.md` — アップロード/削除スケジューリング、同期ステータス、ワーカー/スケジューラ分離、設定ロジック
