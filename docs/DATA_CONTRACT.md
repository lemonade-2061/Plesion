# データ契約（Firebase ⇔ Android）

Android（tanabe / tajima）と Firebase（takuma）の間の「約束事」です。
**ここを変える場合は Issue で合意してから**、このファイル → `data/model/` → Firestore の順に更新します。

## 1. 基本ルール

- アプリ内で使う型は `data/model/` の Kotlin クラス。Firebase の型（`Timestamp`, `GeoPoint`, `DocumentSnapshot`）は `data/remote/` の中で変換し、外に出さない。
- 時刻はアプリ内ではエポックミリ秒（`Long`）、Firestore では `Timestamp`。
- ID は Firestore のドキュメント ID（自動採番）。仮データは `sample-` で始まるので本番 ID と衝突しない。
- フィールド名は Kotlin のプロパティ名と同じ camelCase。

## 2. 投稿データの共通項目

`posts/{postId}`

| フィールド | Firestore 型 | Kotlin（`Post`） | 必須 | 説明 |
|---|---|---|---|---|
| `authorId` | string | `authorId` | ○ | 投稿者の uid。**匿名投稿では空文字**（§4） |
| `authorName` | string | `authorName` | ○ | 表示名。匿名投稿では `"匿名"` |
| `isAnonymous` | bool | （未追加。匿名投稿 UI（#20）の PR で追加） | ○ | 匿名投稿か |
| `text` | string | `text` | ○ | 本文。1〜500 文字 |
| `imageUrls` | array\<string\> | `imageUrls` | − | Storage のダウンロード URL。最大 4 枚 |
| `location` | geopoint | `location: GeoPoint?` | ○ | 投稿位置（§4 の丸め後の値）。Kotlin では null 可だが、`createPost` は null なら失敗を返す |
| `geohash` | string | （remote 層のみ） | ○ | 1km 検索用。Android の画面側では使わない |
| `createdAt` | timestamp | `createdAt: Long` | ○ | サーバー時刻（`FieldValue.serverTimestamp()`） |
| `commentCount` | number | `commentCount` | ○ | 初期値 0 |

コメント `posts/{postId}/comments/{commentId}`: `authorId`, `authorName`, `text`, `createdAt`（`Comment` と同じ）。
ユーザー `users/{uid}`: `displayName`, `photoUrl`, `bio`（`User` と同じ）。

## 3. 1km 以内の位置検索の責務分担

| 処理 | 担当 | 場所 |
|---|---|---|
| 位置情報の権限リクエスト UI | tajima（投稿作成）/ tanabe（ホーム） | 各 `feature/` |
| 現在地の取得 | takuma | `core/location/LocationProvider` の実装 |
| geohash 計算・Firestore 範囲検索・距離で 1km に絞り込み | takuma | `PostRepository.getNearbyPosts` の実装（`data/remote/`） |
| 結果の表示・「近くに投稿がない」表示 | tanabe | `feature/feed/` |

- 画面と ViewModel は `getNearbyPosts(center)` を呼ぶだけ。geohash や距離計算を画面側に書かない。
- 権限が拒否された場合は位置なしで動作する（エラー画面にしない）。表示文言は担当画面の人が決める。

## 4. 匿名投稿時の情報保護

匿名投稿は「他のユーザーから投稿者を特定できない」ことを保証します。

1. **投稿ドキュメントに本人を特定できる情報を入れない。** `authorId = ""`, `authorName = "匿名"`, プロフィール画像なし。
   Firestore はドキュメント単位でしか読み取り制御できないため、「フィールドを隠す」ことはできない。
2. **本人確認用の uid は別の場所に保存する。** `posts/{postId}/private/owner` に `{ uid }` を置き、セキュリティルールで本人のみ読み取り可にする（編集・削除の権限確認に使う）。
3. **位置を丸める。** すべての投稿で、保存前に緯度経度を小数第3位（約100m）に丸める。自宅などの特定を防ぐため。
4. **ログに出さない。** uid・位置・本文を `Log` や Crashlytics に出力しない。
5. **仮データでも実在の人名・住所を使わない。**

## 5. 変更履歴

| 日付 | 内容 |
|---|---|
| 2026-10-23 | 初版 |
| 2026-10-30 | 本文の上限（500 文字）と画像の上限（4 枚）を確定。`location` は null 可の型のまま `createPost` で必須チェックすることに決定（#6） |
