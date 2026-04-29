# ビルドと環境

`AGENTS.md` から参照されます（すべてのルールの情報源）。
情報源の優先順位：`AGENTS.md` → フィーチャーローカルパターン → `CLAUDE.md` → `.agent/skills/*`。
このファイルと `AGENTS.md` が矛盾する場合は `AGENTS.md` を優先すること。

---

## ビルドと環境のルール

このプロジェクトはビルドフレーバーと必須の環境プロパティを使用します。
フレーバー付きビルドを壊さないよう注意してください。

各フレーバーは以下の必須プロパティを定義しなければなりません：

- `COGNITO_CLIENT_ID`
- `API_BASE_URL`
- `S3_BUCKET_NAME`

### プロパティの設定場所

プロパティは以下の優先順位で解決されます：

1. **`local.properties`**（ローカル開発用 — このファイルは絶対にコミットしない）：
   ```properties
   DEV_COGNITO_CLIENT_ID=xxxxx
   DEV_API_BASE_URL=https://dev.example.com/
   DEV_S3_BUCKET_NAME=my-dev-bucket
   PROD_COGNITO_CLIENT_ID=yyyyy
   PROD_API_BASE_URL=https://api.example.com/
   PROD_S3_BUCKET_NAME=my-prod-bucket
   ```

2. **Gradleプロジェクトプロパティ**（例：コマンドラインで `-PDEV_COGNITO_CLIENT_ID=xxxxx`、または `gradle.properties` 経由）。

3. **CI環境**：`DEV_*` / `PROD_*` の値を **Gradleプロジェクトプロパティとして** 渡します（`findProperty` が解決するもの）。同等の2つの方法：
    - コマンドライン：`-PDEV_COGNITO_CLIENT_ID=xxxxx`
    - Gradleプロパティにマッピングされた環境変数：
      `ORG_GRADLE_PROJECT_DEV_COGNITO_CLIENT_ID=xxxxx`
      （GradleはOSのプレフィックス `ORG_GRADLE_PROJECT_*` の環境変数を自動的にプロジェクトプロパティにマッピングする）

   ルートの `build.gradle.kts` はまず `local.properties` を読み、次に `findProperty`（Gradleプロジェクトプロパティ）にフォールバックするため、上記のどちらの方法もCIで機能する。

> **キー命名**：フレーバープレフィックスでベース名を接頭辞付けする — `DEV_` または `PROD_`。
> 例：`DEV_COGNITO_CLIENT_ID`、`PROD_API_BASE_URL`。

ルール：

- Kotlinソースにシークレット、エンドポイント、クライアントID、バケット名をハードコードしない。
- 意図した設定メカニズム外に環境固有の値をコミットしない。
- `app/build.gradle.kts`、フレーバーロジック、マニフェスト設定、CIダミーシークレットの動作に触れる際は注意する。
- フレーバー固有の値は意図したプロパティまたは設定フローに配置する — Kotlinソースに散在させない。
- Gradleコンベンションに関するタスク以外は `build-logic` を変更しない。

## 依存ルール

- リポジトリですでに使用されている既存のライブラリとパターンを優先する。
- 明確に必要でない限り、新しいライブラリを追加しない。
- 依存関係の追加が避けられない場合は理由を説明し、スコープを最小限に保つ。
- できる限りアプリレベルの広範な追加よりモジュールローカルな依存を優先する。
- 依存関係の変更が `app`、`core:*`、または複数のフィーチャーモジュールにまたがる場合は、より広い検証スコープとして扱う（`./gradlew test` を実行する）。
