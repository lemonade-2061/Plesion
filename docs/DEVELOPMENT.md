# 開発の進め方

## 毎回の流れ

```bash
git switch main
git pull origin main
git switch -c feature/<内容>      # 例: feature/home-timeline
# ... 実装 ...
./gradlew assembleDebug testDebugUnitTest lintDebug
git add <変更したファイル>
git commit -m "Add timeline list to home screen"
git push -u origin feature/<内容>
# GitHub で PR を作成（テンプレートに沿って記入）
```

## ルール

- `main` へ直接 push しない。マージは PR のレビュー承認後。
- PR は小さく。迷ったら分ける。
- 他の人の担当フォルダを変更する場合は、事前にその人に連絡する。
- 共通ファイル（[FEATURE_OWNERSHIP.md](FEATURE_OWNERSHIP.md) 参照）は先に Issue / チャットで相談。
- ライブラリを追加するときは `gradle/libs.versions.toml` に追記し、PR に理由を書く。
- 仮データを追加するときは `SampleData` を使い、ID は `sample-`、本文は `[サンプル]` で始める。
- 秘密情報（`google-services.json`、API キー、`.jks`）はコミットしない。

## コンフリクトしたら

1. `git pull origin main`（または `git merge origin/main`）で取り込む。
2. Android Studio の Merge 画面で解決。分からなければ相手の担当者と画面共有して解決する。
3. 解決後に必ず `./gradlew assembleDebug` を通す。

## テスト

- 単体テスト: `app/src/test/`。ViewModel は Fake Repository を渡してテストする（`CreatePostViewModelTest` を参考に）。
- UI テスト: `app/src/androidTest/`。エミュレータが必要（`./gradlew connectedDebugAndroidTest`）。

## コミットメッセージ

英語の命令形または日本語で、何をしたかが分かる1行を書く。例: `Add post card component` / `投稿カードを追加`。
