# Review

この文書は実装差分をレビューする際の比較範囲の確定、確認観点、指摘の分類・形式、および終了時の報告手順をまとめます。

## 比較範囲の確定

- ユーザーが base branch、commit、merge-base、対象ファイルを指定した場合はその比較基準を使う。
- 比較基準が未指定の場合は、未コミット差分（`git diff` / `git diff --cached`）があるか、または現在のブランチと base（原則 `develop`）とのコミット差分かを先に確認して確定する。
- 対象を限定したレビューでは、確認した範囲と確認していない範囲を分けて報告する。

## 主な確認観点

変更された本番コードと対応するテストを対で読み、変更行の外側への波及も含めて確認する。

- **レイヤー境界と依存方向（[`docs/architecture.md`](architecture.md)）**:
  - `ui -> domain <- data` の方向が守られているか。
  - UI から DAO、SDK、Repository 実装、Worker を直接参照していないか。
  - `domain` に Android framework、Room、WorkManager、Amplify / Firebase の型が漏れていないか。
  - DTO / Room Entity / SDK model が domain や UI state に漏れていないか。
- **エラーハンドリングとキャンセル伝播（[`docs/implementation-guidelines.md`](implementation-guidelines.md)）**:
  - `try / catch` や `runCatching`（cleanup のネストを含む）で `CancellationException` が確実に再スローされているか。
  - provider 例外が data 層の Mapper で domain error に変換されているか。
- **機能契約の整合性**:
  - 認証（[`docs/authentication.md`](authentication.md)）: アカウント列挙を防ぐエラー統合、サインアウト・退会の順序（FCM cleanup → backend 削除 → Cognito 削除）、FCM デバイストークン登録 Worker の契約が維持されているか。
  - メディア同期（[`docs/media-sync.md`](media-sync.md)）: S3 転送・ローカル保存・API 登録の順序、一時失敗（`retry`）と永続失敗（`ERROR`）の振り分け、`completeUpload()` の Worker 実行単位 1 回呼び出し、リモート同期での pending 保護、WorkManager の unique work 契約が崩れていないか。
  - DB（[`docs/data-model.md`](data-model.md)）: Entity / DAO query / `UploadRecordEntityMapper` / `SyncStatus` 文字列の対応、および schema 変更時の DB `version` と migration 判断が揃っているか。
- **ビルド・DI・コメント・テスト（[`docs/development-setup.md`](development-setup.md)、[`docs/testing.md`](testing.md)）**:
  - 新規クラス追加時に `:app` の Hilt Module 更新漏れがないか。
  - 依存追加が最小モジュールになっているか、環境値や secret の直書きがないか。
  - 字面から読み取れる説明コメントや不要なログが残っていないか。
  - 変更した振る舞いに対して適切なテスト（画面契約としての state / effect、Worker の状態更新、例外再スローなど）があるか。

## 指摘の分類

| ラベル | 意味 |
| --- | --- |
| **Critical** | マージ前に必ず修正が必要。クラッシュ、データ損失、認証・権限の不備、secret 漏洩、メディア同期や DB migration の整合性破壊、CI で確実に失敗する問題、重大な依存方向の破壊など |
| **Suggestion** | 修正を推奨。責務分離、保守性、エラー処理、テスト網羅、変更容易性を明確に改善する指摘 |
| **Nitpick** | 動作や設計への影響が小さい命名、表記、軽微な読みやすさの改善（対応任意） |

方針とのわずかな差異だけで機械的に Critical にせず、実際の影響に基づいて分類する。問題がない場合に無理に指摘を作らない。

## 指摘の形式

- 重要度が高い順に上から通し番号を振る。
- 各指摘の先頭に `[Critical]` / `[Suggestion]` / `[Nitpick]` を付け、対象ファイルと行番号（`path/to/File.kt:line`）、問題による影響・理由、具体的な修正方針を含める。
- 指摘がない場合は「指摘事項なし」と明記する。

例:

```markdown
1. [Critical] `feature/media/data/.../UploadMediaWorker.kt:118` — `runCatching` の `onFailure` で `CancellationException` を再スローしていないため、Worker キャンセルが一時失敗として扱われる。先頭で `if (e is CancellationException) throw e` を追加する。
2. [Suggestion] `feature/auth/ui/.../LoginViewModelTest.kt:85` — `signInUseCase` の呼び出し確認のみで、成功時の `NavigateToHome` effect が検証されていない。`viewModel.effect.test` で発火を確認する。
```

## 終了時の報告

指摘の有無にかかわらず、最後に次を報告する。

- Critical / Suggestion / Nitpick の各件数
- レビューした比較基準と対象範囲（未確認の範囲がある場合はその明示）
- 実行したテスト・静的解析などの検証結果、および未実施の検証と理由
- 残るリスクまたはテストギャップ
- 確認範囲に応じたマージ可否判断（差分全体を確認した場合のみ PR 全体のマージ可否を示し、部分レビューや静的確認のみの場合は確認範囲内の判断として報告する）
