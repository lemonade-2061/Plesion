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
| `isAnonymous` | bool | `isAnonymous` | ○ | 匿名投稿か。この項目がない古い投稿は `false` として扱う |
| `text` | string | `text` | ○ | 本文。1〜500 文字 |
| `imageUrls` | array\<string\> | `imageUrls` | − | Storage のダウンロード URL。最大 4 枚。画像は `posts/{postId}/images/{0〜3}.jpg`（§4） |
| `location` | geopoint | `location: GeoPoint?` | ○ | 投稿位置（§4 の丸め後の値）。Kotlin では null 可だが、`createPost` は null なら失敗を返す |
| `geohash` | string | （remote 層のみ） | ○ | 1km 検索用。Android の画面側では使わない |
| `createdAt` | timestamp | `createdAt: Long` | ○ | サーバー時刻（`FieldValue.serverTimestamp()`） |
| `commentCount` | number | `commentCount` | ○ | 初期値 0 |
| `editedAt` | timestamp | `editedAt: Long?` | − | 本文を編集した時刻（サーバー時刻）。編集していなければなし。編集できるのは投稿者が本文だけ（#16） |

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
- 全件を取るタイムラインは用意しない。投稿の一覧は `getNearbyPosts` だけ（#26）。
- セキュリティルールでは、投稿の一覧取得は**件数上限（`limit`）100 件以下のクエリだけ**許可する。1 件ずつの取得（投稿詳細）はログインしていれば可。
- 一覧取得は **geohash 順のクエリ**（近くの投稿）か、**自分の `authorId` で絞ったクエリ**（自分の投稿 `getMyPosts`）だけ許可する。他人の `authorId` で絞って、その人の投稿を場所に関係なく集めることはできない（#50）。
  これは `(authorId, geohash)` の複合インデックスがないことに頼っている。**`firestore.indexes.json` にこの組み合わせを追加しないこと。**
- **限界**: 閲覧者の現在地はルールから見えないため、「1km 以内の投稿しか読めない」ことまではサーバー側で強制できない（位置を偽ったり、範囲を変えて何度もクエリしたりすれば圏外の投稿も読める）。完全に防ぐには Cloud Functions 経由の検索（Blaze プラン）が必要で、今回の期間ではやらない。
- **限界**: 件数上限は geohash の範囲ごとにかかり、取得順は geohash 順（新しい順ではない）。投稿が密集した範囲で 100 件を超えると、新しい投稿が結果から漏れることがある（対策は #45）。
- 位置情報は必須。正確な位置（FINE）の権限がない場合（おおよその位置だけ許可された場合を含む）や現在地を取得できない場合は、閲覧・投稿を制限し、権限設定の案内を表示する。表示文言は担当画面の人が決める。
- 権限リクエストでは ACCESS_FINE_LOCATION と ACCESS_COARSE_LOCATION をまとめて要求する（Android 12 以降の仕様）。

## 4. 匿名投稿時の情報保護

匿名投稿は「他のユーザーから投稿者を特定できない」ことを保証します。

1. **投稿ドキュメントに本人を特定できる情報を入れない。** `authorId = ""`, `authorName = "匿名"`, プロフィール画像なし。
   Firestore はドキュメント単位でしか読み取り制御できないため、「フィールドを隠す」ことはできない。
2. **本人確認用の uid は別の場所に保存する。** `posts/{postId}/private/owner` に `{ uid }` を置き、セキュリティルールで本人のみ読み取り可にする（編集・削除の権限確認に使う）。
   - `private/owner` は匿名投稿だけに作る。投稿と同じバッチでしか作れず、投稿と同じバッチでしか消せない（#19）。
   - 自分の投稿かどうか（編集・削除ボタンを出すか）は、画面側で `authorId` と比べずに `PostRepository.isMyPost(post)` を使う。
3. **位置を丸める。** すべての投稿で、保存前に緯度経度を小数第3位（約100m）に丸める。自宅などの特定を防ぐため。
   検索用と公開用の位置は分けず、丸めた位置（`location` と、そこから計算した `geohash`）だけを保存する。丸める前の位置はどこにも保存しない（#27）。
4. **距離は段階で表示する。** 画面には「100m以内 / 300m以内 / 500m以内 / 1km以内」だけを出し、細かい距離は出さない。場所を変えて何度も見ることで投稿位置を絞り込まれにくくするため（#27）。
5. **画像の位置情報を消す。** 写真の EXIF には撮影場所（GPS）が正確に入っているので、`createPost` が端末で再エンコード（長い辺 1600px・JPEG）してからアップロードする。画面側で画像のバイト列を直接アップロードしない（#18）。
   Storage のパスには uid を入れない（ダウンロード URL にパスが含まれ、匿名投稿の投稿者が分かってしまうため）。
   - **限界**: 画像は投稿を作る前にアップロードするため、ログインしていれば未使用の postId に画像を置けてしまう（1 枚 2MB・4 枚まで）。完全に防ぐには Cloud Functions が必要で、今回は予算アラートで監視する。
6. **ログに出さない。** uid・位置・本文を `Log` や Crashlytics に出力しない。
7. **仮データでも実在の人名・住所を使わない。**

## 5. 変更履歴

| 日付 | 内容 |
|---|---|
| 2026-10-23 | 初版 |
| 2026-10-30 | 本文の上限（500 文字）と画像の上限（4 枚）を確定。`location` は null 可の型のまま `createPost` で必須チェックすることに決定（#6） |
| 2026-11-10 | タイムライン（全件取得）を廃止し、投稿の一覧取得を件数上限付きに制限（#26）。位置の扱いと距離の段階表示を確定（#27） |
| 2026-11-12 | `isAnonymous` を追加。匿名投稿の `private/owner` の作成・削除ルールと `isMyPost` を確定（#19） |
| 2026-11-12 | 画像の保存場所・アップロード手順・Storage のルールを確定（#18） |
| 2026-11-12 | 一覧取得を geohash 順か自分の投稿に限定し、`getMyPosts` を追加（#50） |
| 2026-11-12 | 本文の編集（`updatePost`、`editedAt`）を追加（#16） |
