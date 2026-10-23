# Plesion

近くにいる人（半径1km以内）の投稿を見たり、自分の投稿を共有したりできる、位置情報ベースの Android 向け SNS アプリです。

> **現在の状態:** 開発基盤のみ。画面は雛形で、データはすべて**仮データ（`[サンプル]` と表示）**です。Firebase には接続していません。

## 採用技術

| 分類 | 技術 |
|---|---|
| 言語 | Kotlin 2.2.21 |
| UI | Jetpack Compose（BOM 2025.12.01）/ Material 3 |
| 設計 | MVVM + Repository |
| 画面遷移 | Navigation Compose 2.9.6 |
| 非同期 | Kotlin Coroutines / Flow |
| DI | 手動 DI（`di/AppContainer.kt`） |
| ビルド | Gradle 8.13（Kotlin DSL）/ Android Gradle Plugin 8.13.2 |
| SDK | minSdk 26 / compileSdk・targetSdk 36 |
| バックエンド（予定） | Firebase Authentication（Google）/ Cloud Firestore / Cloud Storage / Cloud Messaging |
| 位置情報（予定） | Fused Location Provider |

バージョンは `gradle/libs.versions.toml` で一元管理しています。

## 開発環境のセットアップ

1. **Android Studio** をインストール（2025.x 以降推奨）。
2. SDK Manager で **Android SDK Platform 36** をインストール。
3. リポジトリを取得:
   ```bash
   git clone https://github.com/lemonade-2061/Plesion.git
   cd Plesion
   ```
4. Android Studio で `Plesion` フォルダを **Open** → Gradle Sync が自動で走ります。
5. Gradle の JDK は Android Studio 同梱の JDK（JBR 21）を使ってください（Settings → Build Tools → Gradle → Gradle JDK）。
   - ⚠️ JDK 25 など新しすぎる JDK では Gradle 8.13 が動きません。

## ビルド・起動手順

- Android Studio: 上部の ▶ (Run 'app') を押す。エミュレータ（例: Pixel, API 34 以上）か実機で起動します。
- コマンドライン（macOS 例）:
  ```bash
  export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
  ./gradlew assembleDebug        # APK を作る
  ./gradlew installDebug         # 接続中の端末にインストール
  ./gradlew testDebugUnitTest    # 単体テスト
  ./gradlew lintDebug            # 静的チェック
  ```

起動すると「サンプルユーザーでログイン（仮）」→ ホーム / 投稿 / プロフィール（→ 設定）を行き来できます。

## ディレクトリ構成

```text
app/src/main/java/jp/plesion/app/
├── MainActivity.kt          … 起動点
├── PlesionApplication.kt    … AppContainer を作る（Firebase 初期化もここ）
├── PlesionApp.kt            … テーマ + ナビゲーションのルート
├── navigation/              … ルート定義と NavHost（共通ファイル）
├── feature/                 … 画面ごとのフォルダ（Screen + ViewModel）
│   ├── auth/                … ログイン
│   ├── feed/                … ホーム・タイムライン
│   ├── post/                … 投稿作成
│   ├── profile/             … プロフィール
│   └── settings/            … 設定
├── data/
│   ├── model/               … Post, User, Comment, GeoPoint
│   ├── repository/          … Repository インターフェース（共通ファイル）
│   ├── local/               … 仮データと Fake 実装
│   └── remote/              … Firebase 実装（これから）
├── core/
│   ├── ui/                  … テーマ・UiState・ローディング/エラー表示
│   └── location/            … 位置情報取得の窓口
└── di/AppContainer.kt       … 依存関係の組み立て（共通ファイル）
```

詳しくは [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)。

## 担当領域

| メンバー | 領域 | 主なフォルダ |
|---|---|---|
| tanabe | Android UI（ホーム・タイムライン・投稿カード・投稿詳細・画面遷移） | `feature/feed/`, `navigation/`, `core/ui/theme/` |
| tajima | Android 機能（ログイン画面・投稿作成・画像添付・編集削除・プロフィール） | `feature/auth/`, `feature/post/`, `feature/profile/` |
| takuma | バックエンド（Firebase・Google 認証・Firestore・1km 検索・アクセス制御） | `data/remote/`, `core/location/`, Firebase コンソール |

**共通ファイル**（`navigation/`, `data/model/`, `data/repository/`, `di/`, `core/ui/` の共通部品, `gradle/libs.versions.toml`, `app/build.gradle.kts`）を変更するときは、**PR を出す前にチャットで関係者に一声かける**こと。詳細は [docs/FEATURE_OWNERSHIP.md](docs/FEATURE_OWNERSHIP.md)。

## ブランチ・PR 運用ルール

- `main` には直接 push しない。必ずブランチ → PR → レビュー1人以上 → マージ。
- ブランチ名: `feature/<内容>`, `fix/<内容>`, `chore/<内容>`（例: `feature/home-timeline`）。
- 1つの PR は小さく（目安 300 行以内）。1 PR = 1 つの目的。
- PR を出す前に `./gradlew assembleDebug testDebugUnitTest` が通ることを確認する。
- 作業前に `git pull origin main` で最新化し、こまめに main を取り込む。
- `google-services.json`・API キー・署名鍵は**絶対にコミットしない**（`.gitignore` 済み）。

詳しくは [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md)。

## Firebase 接続前にできること

- 画面の UI 実装（仮データで表示確認できます）
- ViewModel の状態管理と単体テスト（Fake Repository を使う）
- 画面遷移の追加
- データモデル・Repository インターフェースの相談・調整

Fake 実装は `data/local/` にあり、`di/AppContainer.kt` で本番実装に差し替えます。接続手順は [docs/FIREBASE_SETUP.md](docs/FIREBASE_SETUP.md)。

## 未実装機能

- Firebase 接続（Auth / Firestore / Storage / FCM）
- Google ログイン（現在は仮ログインボタン）
- 位置情報の取得と 1km 以内の検索
- 投稿の保存（現在はメモリ上のみ。再起動で消える）
- 画像添付、投稿編集・削除 UI、投稿詳細画面、コメント UI
- 通知
- アプリアイコン、正式なデザイン

スケジュールは [docs/ROADMAP.md](docs/ROADMAP.md)。
