---
name: implementation-review
description: 実装差分や PR・ブランチの変更内容をレビューする依頼で使う。Cloud Photos のレイヤー境界、責務配置、キャンセル伝播、認証・同期・DB の契約整合性、テスト不足を確認し、docs/review.md の形式で報告する。コードの新規実装やテスト追加そのものの依頼は対象外。
---

# 目的

Cloud Photos の実装差分がプロジェクトのアーキテクチャ・機能契約・実装規約に沿っているかを確認し、修正すべき問題と根拠を [`docs/review.md`](../../../docs/review.md) の形式で報告する。

# 条件別の参照先

- レビュー手順・比較範囲・指摘分類・報告形式の正本: [`docs/review.md`](../../../docs/review.md)（必須）
- 差分に含まれる領域に応じて参照する技術文書:
  - モジュール境界・依存方向・責務分担・DI: [`docs/architecture.md`](../../../docs/architecture.md)
  - ViewModel・Compose・coroutines・`CancellationException`・ログ・エラー処理: [`docs/implementation-guidelines.md`](../../../docs/implementation-guidelines.md)
  - 認証・サインアウト・退会・FCM デバイストークン・アカウント列挙防止: [`docs/authentication.md`](../../../docs/authentication.md)
  - アップロード・削除・リモート同期・Worker・Scheduler・`SyncStatus`: [`docs/media-sync.md`](../../../docs/media-sync.md)
  - Room `upload_records`・DAO・保存値互換性・DB version / migration: [`docs/data-model.md`](../../../docs/data-model.md)
  - テストの妥当性・不足確認: [`docs/testing.md`](../../../docs/testing.md)
  - ビルド設定・依存関係・CI/CD 変更: [`docs/development-setup.md`](../../../docs/development-setup.md)、[`docs/ci.md`](../../../docs/ci.md)

# 手順

1. [`docs/review.md`](../../../docs/review.md) の「比較範囲の確定」に従い、未コミット差分とブランチ差分（`develop...HEAD` など）のどちらを対象にするか確定する。
2. 変更ファイルの一覧と差分の目的を把握し、該当する領域の技術文書と周辺コード・テストを確認する。
3. [`docs/review.md`](../../../docs/review.md) の「主な確認観点」に沿って、バグ・契約違反・レイヤー境界違反・キャンセル伝播漏れ・状態不整合・テスト不足を洗い出す。
4. [`docs/review.md`](../../../docs/review.md) の「指摘の分類」「指摘の形式」「終了時の報告」に従ってレビュー結果を出力する。

# 注意事項

- 単なる個人の好みやスタイルではなく、不具合、契約違反、アーキテクチャ違反、保守性リスク、テスト不足を優先して指摘する。
- 指摘には必ず対象ファイル・行と、根拠となる実装契約または技術文書の箇所を添える。
- 問題が見つからない場合も、確認した比較範囲と未検証項目・残存リスクを明記する。
