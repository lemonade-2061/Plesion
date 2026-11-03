# Firebase 導入手順（担当: takuma）

> 現在は**未接続**です。アプリは `data/local/` の Fake 実装で動いています。

## 1. Firebase プロジェクトの作成

1. [Firebase コンソール](https://console.firebase.google.com/) でプロジェクトを作成。
2. Android アプリを追加。パッケージ名: `jp.plesion.app`。
3. Google ログインのため、**メンバー全員**のデバッグ用 SHA-1 を登録（§1.1）。
4. 全員分の SHA-1 を登録してから `google-services.json` をダウンロードし、`app/` に置く。
   - **このファイルはコミットしない**（`.gitignore` 済み）。メンバーへの共有は §1.2 の方法で行う。
   - SHA-1 を後から追加した場合は、ダウンロードし直して共有し直す。

### 1.1 SHA-1 の登録

デバッグビルドの署名鍵（`~/.android/debug.keystore`）は PC ごとに違うため、**PC ごとに** SHA-1 の登録が必要。
登録されていない PC でビルドしたアプリは、Google ログインだけが失敗する（`DEVELOPER_ERROR` など）。

1. 各メンバーが、プロジェクトのフォルダで次のコマンドを実行する。
   ```bash
   ./gradlew signingReport
   ```
2. 出力の `Variant: debug` にある `SHA1:` の値を takuma に送る（SHA-1 は秘密情報ではない）。
3. takuma が Firebase コンソールの「プロジェクトの設定 → マイアプリ → フィンガープリントを追加」で登録する。

PC の買い替えなどで `debug.keystore` が作り直されたときは、SHA-1 が変わるので登録し直す。

### 1.2 `google-services.json` の共有

- takuma がチームの**非公開の**チャットで、ファイルを直接送る。
- リポジトリ・Issue・PR・公開の場所には置かない。
- 受け取ったら `app/google-services.json` に置き、Gradle Sync する。
  ビルドのログに「`app/google-services.json がないため Firebase を無効にしてビルドします`」が出なければ配置できている。

## 2. Gradle への追加

google-services プラグインは追加済み（`gradle/libs.versions.toml` の `googleServices`）。
`app/google-services.json` があるときだけ適用するので、ファイルがない環境（CI など）でもビルドは通る。その場合 Firebase は使えない。

ライブラリは使う PR で `gradle/libs.versions.toml` に Firebase BoM 経由で追加し、`app/build.gradle.kts` の `dependencies` に追記する。
バージョンは追加時点の [公式リリースノート](https://firebase.google.com/support/release-notes/android) で確認すること（推測で書かない）。

使う予定のライブラリ:

- `firebase-auth`（+ Credential Manager / Google ID ライブラリ）
- `firebase-firestore`
- `firebase-storage`
- `firebase-messaging`
- `play-services-location`（Fused Location Provider）

## 3. 接続ポイント

| 何を | どこに | 置き換える Fake |
|---|---|---|
| 認証 | `data/remote/FirebaseAuthRepository.kt`（新規） | `FakeAuthRepository` |
| ユーザー | `data/remote/FirestoreUserRepository.kt`（新規） | `FakeUserRepository` |
| 投稿・コメント | `data/remote/FirestorePostRepository.kt`（新規） | `FakePostRepository` |
| 位置情報 | `core/location/FusedLocationProvider.kt`（新規） | `LocationProvider` の実装 |
| 差し替え | `di/AppContainer.kt` | — |

`AppContainer` で Fake ↔ 本番を切り替える。Firebase 側の準備が整うまで画面チームは Fake で開発を続けられる。

## 4. Firestore のデータ構造（案）

> 確定版の項目定義・匿名投稿のルールは [DATA_CONTRACT.md](DATA_CONTRACT.md) を正とする。

```text
users/{userId}           displayName, photoUrl, bio
posts/{postId}           authorId, authorName, text, imageUrls, location(GeoPoint), geohash, createdAt, commentCount
posts/{postId}/comments/{commentId}   authorId, authorName, text, createdAt
```

## 5. 1km 以内の検索

Firestore は「距離で絞り込む」クエリを直接持たないため、**geohash で範囲検索 → アプリ側で距離を計算して 1km 以内に絞る**方法が一般的。
公式ガイド「Geo queries」を参照し、方式を決めたら `PostRepository.getNearbyPosts` に実装する。

## 6. セキュリティルール

- 投稿の作成・編集・削除は本人のみ。
- 読み取りはログインユーザーのみ。
- ルールは Firebase Emulator Suite でテストしてから本番に反映する。
