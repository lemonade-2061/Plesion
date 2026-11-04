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

## 本番への反映（takuma）

```bash
firebase login        # 初回だけ
firebase deploy --only firestore:rules
```

リポジトリのルートで実行する（`firebase.json` と `.firebaserc` がある場所）。
