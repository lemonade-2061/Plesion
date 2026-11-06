# Firestore セキュリティルールのテスト

`../firestore.rules` を Firebase Emulator で検証する。ルールを変えたら、本番に反映する前に必ず通す。

## 必要なもの

- Node.js（LTS）
- Firebase CLI（`npm install -g firebase-tools`）
- Java 21（Emulator が使う。Android の開発で入れたもので OK）

## 実行

```bash
cd firestore-tests
npm install
npm test
```

`demo-plesion` という仮のプロジェクトで Emulator を立てるので、本番のデータには触れない。

## Emulator UI で中身を見る

`npm test` はテストが終わると Emulator を止めるので、UI で見るときは立ち上げっぱなしにする。

```bash
# リポジトリのルートで
firebase emulators:start --only firestore --project demo-plesion
# 別のターミナルで
cd firestore-tests
FIRESTORE_EMULATOR_HOST=127.0.0.1:8080 node --test
```

http://127.0.0.1:4000 の Firestore タブでデータ、Requests タブでルールの許可・拒否が見られる。テストは毎回データを消すので、終わった後はほぼ空になる。

## 本番への反映（takuma）

```bash
firebase login        # 初回だけ
firebase deploy --only firestore:rules
```

リポジトリのルートで実行する（`firebase.json` と `.firebaserc` がある場所）。
