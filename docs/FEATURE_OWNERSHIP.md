# 担当範囲

| 領域 | 担当 | ファイル / フォルダ |
|---|---|---|
| ホーム・タイムライン・投稿カード・投稿詳細 | tanabe | `feature/feed/`（投稿詳細は `feature/feed/detail/` 等に追加） |
| 画面遷移・テーマ | tanabe | `navigation/`, `core/ui/theme/` |
| ログイン画面 | tajima | `feature/auth/` |
| 投稿作成・画像添付・編集削除 | tajima | `feature/post/` |
| プロフィール | tajima | `feature/profile/` |
| Firebase・認証・Firestore・1km 検索・アクセス制御 | takuma | `data/remote/`, `core/location/`, Firestore ルール |
| 設定画面 | 未定（Week 3 以降に相談） | `feature/settings/` |

## 共通ファイル（変更前に関係者へ連絡）

| ファイル | 主な関係者 | 理由 |
|---|---|---|
| `navigation/Routes.kt`, `PlesionNavHost.kt` | tanabe（管理）＋ 全員 | 全画面が登録される |
| `data/model/*` | takuma（管理）＋ 全員 | Firestore のデータ構造と一致させる必要がある |
| `data/repository/*` | takuma（管理）＋ tajima | 画面と Firebase の契約 |
| `di/AppContainer.kt` | takuma | 実装の差し替え |
| `core/ui/`（UiState, StateViews） | tanabe | 全画面で使う |
| `gradle/libs.versions.toml`, `app/build.gradle.kts` | 全員 | ビルドが壊れると全員止まる |
| `AndroidManifest.xml` | 全員 | 権限追加など |

### ルール

1. 共通ファイルを変える PR は、タイトルに `[共通]` を付け、管理者をレビュアーに入れる。
2. インターフェース（`data/repository/`）を変えるときは、先に Issue で形を合意してから実装する。
3. 同じ共通ファイルを複数人が同時に変えそうなときは、先に出した方を優先してマージし、後の人が取り込む。
