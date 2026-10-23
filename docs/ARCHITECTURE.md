# アーキテクチャ

## 全体像

```text
Screen (Composable) ──observe──▶ ViewModel ──call──▶ Repository (interface)
        ▲                          │                       │
        └──── UiState (StateFlow) ─┘          ┌────────────┴────────────┐
                                              │                         │
                                     data/local/Fake*        data/remote/Firestore*（予定）
```

- **Screen**: 状態を表示し、ユーザー操作を ViewModel に伝えるだけ。ロジックを書かない。
- **ViewModel**: 画面の状態（`StateFlow`）を持ち、Repository を呼ぶ。Android の `Context` や Firebase を直接触らない。
- **Repository**: データの取得・保存の窓口。インターフェースなので、Fake と本番を差し替えられる。
- **AppContainer**: どの実装を使うかを1か所で決める（手動 DI）。

## なぜこの構成か

- **シングルモジュール**: 3〜4人・4週間の規模では、マルチモジュールはビルド設定の学習コストが利益を上回る。
- **UseCase 層なし**: Clean Architecture の UseCase は今は不要。ViewModel が肥大化したら導入を検討する。
- **手動 DI（Hilt なし）**: アノテーション処理の仕組みを理解しなくても読める。依存が増えて辛くなったら Hilt 導入を検討する。
- **feature/ ごとのフォルダ**: 担当者ごとに触るフォルダが分かれ、コンフリクトが起きにくい。

## 状態の表し方

- 一覧や詳細の読み込み: `core/ui/UiState<T>`（Loading / Success / Error）と `UiStateContent` を使う。
- フォーム（ログイン・投稿作成）: 画面専用の `data class XxxUiState` を作る。

## 画面を1つ追加する手順

1. `feature/<機能>/` に `XxxScreen.kt` と `XxxViewModel.kt` を作る。
2. `navigation/Routes.kt` にルートを追加。
3. `navigation/PlesionNavHost.kt` に `composable(Routes.XXX) { ... }` を追加（共通ファイルなので事前に共有）。

## Firebase の型を外に出さない

Firestore の `DocumentSnapshot` や `Timestamp` は `data/remote/` の中で `data/model/` の型に変換する。
画面や ViewModel が Firebase に依存しないので、Firebase なしでもテスト・プレビューできる。
