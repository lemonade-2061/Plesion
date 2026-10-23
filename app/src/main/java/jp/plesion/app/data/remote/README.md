# data/remote

Firebase（Firestore / Storage / Auth）と通信する実装クラスを置く場所です。現在は空です。

- 例: `FirestorePostRepository : PostRepository`
- Firebase の型（`DocumentSnapshot`, `Timestamp` など）はこのパッケージの中で `data/model` の型に変換し、外に出さないこと。
- 実装後、`di/AppContainer.kt` の Fake 実装を差し替える。

担当: takuma（詳細は `docs/FIREBASE_SETUP.md`）
