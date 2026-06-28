---
name: implementation-review
description: 実装差分レビューに使う。現在ブランチや未コミット差分について、レイヤー境界、責務配置、エラーハンドリング、テスト不足、media 同期整合性、Gradle/DI 影響を確認するレビュー作業で使用する。
---

# Implementation Review

## レビュー手順

1. `git diff` と必要に応じて `git diff --cached` を確認し、変更を UI、domain、data、DB、auth、media sync、build/CI、test に分類する。
2. 変更された production code と対応する test を対で読み、実装意図と検証範囲が一致しているか確認する。
3. レイヤー境界を確認し、UI から DAO/SDK/Repository 実装/Worker を呼んでいないか、domain に Android/Room/WorkManager/SDK 型が入っていないかを見る。
4. Data/Worker の差分では、provider model/Entity/例外が漏れていないか、`CancellationException` が再スローされるかを確認する。
5. media sync の差分では、retry、状態遷移、`SyncStatus`、DB/API/S3 の整合性、WorkManager 契約の変更有無を確認する。
6. Gradle、DI、flavor、依存追加の差分では、変更先が最小か、設定変更に見合う検証があるかを見る。
7. findings は重大度順に `file:line` 付きで書く。問題がなければ findings なしと明記し、残るテストギャップだけを書く。

## トリアージ

- Critical: マージブロック。クラッシュ、データ損失、認証/権限不備、secret漏洩、media同期の整合性破壊、migration破壊、CIで確実に失敗する問題など、リリース前に必ず直すべき指摘。
- Suggestion: マージブロックではないが、保守性、責務分離、テスト網羅、エラー処理、性能、将来の変更容易性を明確に改善する指摘。
- Nitpick: 動作や設計への影響が小さい表記、命名、整形、軽微な読みやすさの指摘。対応任意として扱う。

## 出力手順

1. findings を先頭に置き、各 finding に `Critical`、`Suggestion`、`Nitpick` のいずれかを付ける。
2. open question があれば findings の後に置く。
3. 変更概要や確認した検証は最後に短く添える。

## 参考資料

- [AGENTS.md](../../../AGENTS.md)
- [docs/implementation-rules.md](../../../docs/implementation-rules.md)
- [docs/error-handling-guide.md](../../../docs/error-handling-guide.md)
- [docs/verification-policy.md](../../../docs/verification-policy.md)
- [docs/media-upload-flow.md](../../../docs/media-upload-flow.md)
