# Firebase 導入手順（担当: takuma）

> 現在は**未接続**です。アプリは `data/local/` の Fake 実装で動いています。

## 1. Firebase プロジェクトの作成

1. [Firebase コンソール](https://console.firebase.google.com/) でプロジェクトを作成。
2. Android アプリを追加。パッケージ名: `jp.plesion.app`。
3. Google ログインのため、デバッグ用 SHA-1 を登録:
   ```bash
   ./gradlew signingReport
   ```
4. `google-services.json` をダウンロードし `app/` に置く。
   - **このファイルはコミットしない**（`.gitignore` 済み）。メンバーにはチャット等の安全な経路で共有する。

## 2. Gradle への追加

`gradle/libs.versions.toml` に Firebase BoM・google-services プラグインを追加し、`app/build.gradle.kts` の `plugins` と `dependencies` に追記する。
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
