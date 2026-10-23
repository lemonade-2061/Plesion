#!/usr/bin/env bash
# 4週間計画（docs/ROADMAP.md）の Issue・ラベル・マイルストーンを GitHub に作成する。
# 使い方: gh auth login 済みの状態で  ./scripts/create-issues.sh
# GitHub Projects への登録（project スコープが必要）:
#   gh auth refresh -s project
#   gh project create --owner lemonade-2061 --title "Plesion 開発"     # 表示された番号を N とする
#   gh project link N --owner lemonade-2061 --repo lemonade-2061/Plesion
#   gh issue list --repo lemonade-2061/Plesion --state open --limit 100 --json url -q '.[].url' \
#     | xargs -n1 gh project item-add N --owner lemonade-2061 --url
# 同じタイトルの Issue / 同名ラベル・マイルストーンが既にあればスキップするので、再実行しても重複しない。
set -euo pipefail
REPO="${REPO:-lemonade-2061/Plesion}"

label() { gh label create "$1" --repo "$REPO" --color "$2" --description "$3" 2>/dev/null || true; }
label "priority: must" B60205 "必須機能"
label "priority: nice" FBCA04 "余裕があれば（追加機能）"
label "area: ui"       1D76DB "tanabe: Android UI"
label "area: feature"  0E8A16 "tajima: Android 機能"
label "area: backend"  5319E7 "takuma: Firebase / バックエンド"
label "area: team"     C5DEF5 "全員"

milestone() {
  gh api "repos/$REPO/milestones?state=all" -q ".[] | select(.title==\"$1\") | .number" | grep -q . \
    || gh api "repos/$REPO/milestones" -f title="$1" -f due_on="$2T14:59:59Z" -f description="$3" >/dev/null
}
# GitHub は due_on を日付単位で扱う（時刻部分は無視される）
milestone "Week 1: 画面設計・環境構築"         2026-11-01 "10/26〜11/1"
milestone "Week 2: ログイン・投稿・1km検索"    2026-11-08 "11/2〜11/8"
milestone "Week 3: 画像・コメント・交流機能"   2026-11-15 "11/9〜11/15"
milestone "Week 4: 結合テスト・修正・成果物"   2026-11-22 "11/16〜11/22"

EXISTING="$(gh issue list --repo "$REPO" --state all --limit 500 --json title -q '.[].title')"

# issue <タイトル> <マイルストーン> <期限> <ラベル(カンマ区切り)> <担当> <作業内容> <完了条件(改行区切り)>
issue() {
  local title="$1" ms="$2" due="$3" labels="$4" owner="$5" work="$6" done="$7"
  if grep -qxF "$title" <<<"$EXISTING"; then echo "skip: $title"; return; fi
  local body
  body="$(printf '## 担当\n%s\n\n## 期限\n%s\n\n## 作業内容\n%s\n\n## 完了条件\n%s\n\n目安: 3〜5時間/週の範囲で。終わらなければ翌週に回してよい（コメントで共有）。\n' \
    "$owner" "$due" "$work" "$(sed 's/^/- [ ] /' <<<"$done")")"
  gh issue create --repo "$REPO" --title "$title" --milestone "$ms" --label "$labels" --body "$body"
}

W1="Week 1: 画面設計・環境構築"; W2="Week 2: ログイン・投稿・1km検索"
W3="Week 3: 画像・コメント・交流機能"; W4="Week 4: 結合テスト・修正・成果物"

# ---- Week 1
issue "[全員] 開発環境セットアップとPRの練習" "$W1" "2026-11-01" "priority: must,area: team" "tanabe / tajima / takuma（各自）" \
"README の手順で Android Studio を準備し、アプリをエミュレータで起動する。README の分かりにくい点を直す小さな PR を1つ出す。" \
"$(printf '%s\n' 'エミュレータでログイン→ホームまで表示できた' 'ブランチ→PR→CI 成功→レビュー→マージを1回経験した')"
issue "[UI] ホーム・投稿カード・投稿詳細の画面ラフ" "$W1" "2026-11-01" "priority: must,area: ui" "tanabe" \
"ホーム（タイムライン）、投稿カード、投稿詳細の画面ラフを作る（手書き・Figma どちらでも可）。ボトムナビのアイコンを決める。" \
"$(printf '%s\n' 'ラフ画像をこの Issue に添付した' 'tajima・takuma に共有し、表示する項目が docs/DATA_CONTRACT.md と一致している')"
issue "[機能] ログイン・投稿作成・プロフィールの画面ラフ" "$W1" "2026-11-01" "priority: must,area: feature" "tajima" \
"ログイン、投稿作成、プロフィールの画面ラフを作る。投稿本文の文字数上限を決める。" \
"$(printf '%s\n' 'ラフ画像をこの Issue に添付した' '文字数上限を docs/DATA_CONTRACT.md に反映した')"
issue "[Backend] Firebaseプロジェクト作成と設定ファイルの共有方法決定" "$W1" "2026-11-01" "priority: must,area: backend" "takuma" \
"docs/FIREBASE_SETUP.md の手順1を実施。google-services.json をリポジトリ外で安全に共有する方法を決める。" \
"$(printf '%s\n' 'Firebase プロジェクトと Android アプリ（jp.plesion.app）が登録済み' 'メンバー全員の SHA-1 を登録した' 'google-services.json がコミットされていない')"
issue "[Backend] データ契約のレビューと確定" "$W1" "2026-11-01" "priority: must,area: backend" "takuma（レビュー: tanabe, tajima）" \
"docs/DATA_CONTRACT.md の投稿項目・匿名投稿ルール・1km 検索の分担を確認し、全員で合意する。" \
"$(printf '%s\n' '3人が合意したことをこの Issue にコメントした' '変更点を DATA_CONTRACT.md の変更履歴に記録した')"

# ---- Week 2
issue "[UI] タイムラインと投稿カードの実装（仮データ）" "$W2" "2026-11-08" "priority: must,area: ui" "tanabe" \
"feature/feed/ の HomeScreen を、ラフに沿った投稿カード一覧にする。仮データ（SampleData）で表示確認する。" \
"$(printf '%s\n' '投稿カードに投稿者名・本文・投稿時刻が表示される' '0件・読み込み中・エラーの表示を確認した' 'CI が成功している')"
issue "[UI] 投稿詳細画面と画面遷移の追加" "$W2" "2026-11-08" "priority: must,area: ui" "tanabe" \
"投稿カードのタップで投稿詳細画面へ遷移する。Routes / PlesionNavHost の変更は共通ファイルなので PR タイトルに [共通] を付ける。" \
"$(printf '%s\n' 'ホーム→詳細→戻るが動く' 'PostRepository.getPost を使って表示している')"
issue "[機能] ログイン画面のUI" "$W2" "2026-11-08" "priority: must,area: feature" "tajima" \
"LoginScreen をラフに沿って実装する。認証処理自体は takuma の実装待ちのため、仮ログインのままでよい。" \
"$(printf '%s\n' 'ローディング・エラー表示がある' 'Google ログインボタンの見た目がガイドラインに沿っている')"
issue "[機能] 投稿作成画面のUI" "$W2" "2026-11-08" "priority: must,area: feature" "tajima" \
"CreatePostScreen に文字数カウント・上限チェック・エラー表示を追加する。" \
"$(printf '%s\n' '空文字・上限超えで投稿できない' 'CreatePostViewModel の単体テストを追加した')"
issue "[Backend] Google認証の実装" "$W2" "2026-11-08" "priority: must,area: backend" "takuma" \
"Firebase Authentication（Google）で AuthRepository を実装し、AppContainer で差し替え可能にする。" \
"$(printf '%s\n' '実機/エミュレータで Google ログインできる' 'Firebase 未設定でも CI が成功する')"
issue "[Backend] Firestoreへの投稿保存と一覧取得" "$W2" "2026-11-08" "priority: must,area: backend" "takuma" \
"FirestorePostRepository の createPost / observeTimeline / getPost を実装する。位置の丸め（DATA_CONTRACT §4）を含む。" \
"$(printf '%s\n' '投稿が Firestore に保存され、タイムラインに表示される' 'Firebase の型が data/remote/ の外に出ていない')"
issue "[Backend] 1km以内検索の方式検証と実装" "$W2" "2026-11-08" "priority: must,area: backend" "takuma" \
"geohash による範囲検索＋距離での絞り込みで getNearbyPosts を実装する。LocationProvider（Fused Location）も実装する。" \
"$(printf '%s\n' '1km 境界の内外で正しく絞り込まれる（単体テスト）' '位置権限拒否時も落ちない')"

# ---- Week 3
issue "[UI] コメント一覧の表示" "$W3" "2026-11-15" "priority: must,area: ui" "tanabe" \
"投稿詳細画面にコメント一覧を表示する（PostRepository.getComments）。" \
"$(printf '%s\n' 'コメント0件・複数件の表示を確認した')"
issue "[機能] 画像添付（Photo Picker）" "$W3" "2026-11-15" "priority: must,area: feature" "tajima" \
"投稿作成で画像を選択・プレビューできるようにする。アップロードは takuma の Storage 実装を使う。" \
"$(printf '%s\n' '画像を選択して投稿できる' '枚数上限を超えて選べない')"
issue "[機能] 投稿の編集・削除" "$W3" "2026-11-15" "priority: must,area: feature" "tajima" \
"自分の投稿のみ編集・削除できる UI を作る。" \
"$(printf '%s\n' '他人・匿名投稿（自分以外）の編集削除ボタンが出ない' '削除前に確認ダイアログが出る')"
issue "[機能] プロフィール表示・編集" "$W3" "2026-11-15" "priority: nice,area: feature" "tajima" \
"プロフィール表示名・自己紹介の編集を実装する。" \
"$(printf '%s\n' '編集内容が保存・再表示される')"
issue "[Backend] 画像アップロードとコメント保存" "$W3" "2026-11-15" "priority: must,area: backend" "takuma" \
"Cloud Storage への画像アップロード、コメントの保存・取得、commentCount の更新を実装する。" \
"$(printf '%s\n' '画像付き投稿が表示できる' 'コメントが保存される')"
issue "[Backend] セキュリティルールと匿名投稿の保護" "$W3" "2026-11-15" "priority: must,area: backend" "takuma" \
"Firestore / Storage のセキュリティルールを書き、DATA_CONTRACT §4 の匿名投稿ルールを実装する。" \
"$(printf '%s\n' '他人の投稿を編集・削除できない' '匿名投稿の uid が他ユーザーから読めない（Emulator でテスト）')"
issue "[追加] 匿名投稿の切り替えUI" "$W3" "2026-11-15" "priority: nice,area: feature" "tajima" \
"投稿作成画面に「匿名で投稿」スイッチを追加する（Post に isAnonymous を追加する PR は [共通]）。" \
"$(printf '%s\n' '匿名投稿で名前・アイコンが表示されない')"

# ---- Week 4
issue "[全員] Fake実装から本番実装への切り替えと結合テスト" "$W4" "2026-11-22" "priority: must,area: team" "tanabe / tajima / takuma" \
"AppContainer を Firebase 実装に切り替え、ログイン→投稿→近くの投稿表示→コメントを通しで確認する。" \
"$(printf '%s\n' '通しの動作確認チェックリストを Issue にコメントした' '見つかったバグを Issue 化した')"
issue "[全員] READMEと成果物の整理" "$W4" "2026-11-22" "priority: must,area: team" "tanabe / tajima / takuma" \
"README の未実装機能を更新し、スクリーンショットを追加する。" \
"$(printf '%s\n' 'README が実際の機能と一致している')"
issue "[追加] プッシュ通知（FCM）" "$W4" "2026-11-22" "priority: nice,area: backend" "takuma" \
"コメントが付いたときの通知。時間に余裕がある場合のみ。" \
"$(printf '%s\n' '通知を受信できる' '通知権限（Android 13+）を拒否しても落ちない')"
