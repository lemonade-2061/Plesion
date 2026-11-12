// firestore.rules のテスト。Emulator で動かす（README.md 参照）。
import { readFileSync } from 'node:fs';
import { after, beforeEach, before, describe, test } from 'node:test';
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from '@firebase/rules-unit-testing';
import {
  GeoPoint,
  Timestamp,
  deleteDoc,
  collection,
  doc,
  getDoc,
  getDocs,
  increment,
  limit,
  orderBy,
  query,
  serverTimestamp,
  setDoc,
  setLogLevel,
  updateDoc,
  writeBatch,
} from 'firebase/firestore';

// 拒否されるたびに出る警告ログを止める（テストの結果だけを見やすくする）
setLogLevel('silent');

let env;

before(async () => {
  env = await initializeTestEnvironment({
    projectId: 'demo-plesion',
    firestore: { rules: readFileSync(new URL('../firestore.rules', import.meta.url), 'utf8') },
  });
});

after(async () => {
  await env.cleanup();
});

beforeEach(async () => {
  await env.clearFirestore();
});

const alice = () => env.authenticatedContext('alice').firestore();
const bob = () => env.authenticatedContext('bob').firestore();
const guest = () => env.unauthenticatedContext().firestore();

function newPost(overrides = {}) {
  return {
    authorId: 'alice',
    authorName: 'Alice',
    isAnonymous: false,
    text: 'hello',
    imageUrls: [],
    location: new GeoPoint(35.681, 139.767),
    geohash: 'xn76urx',
    createdAt: serverTimestamp(),
    commentCount: 0,
    ...overrides,
  };
}

// ルールを通さずに既存の投稿を置く
async function seedPost(id = 'p1', overrides = {}) {
  await env.withSecurityRulesDisabled(async (context) => {
    await setDoc(doc(context.firestore(), 'posts', id), {
      ...newPost(),
      createdAt: Timestamp.now(),
      ...overrides,
    });
  });
}

function addCommentBatch(db, postId, commentId, overrides = {}) {
  const batch = writeBatch(db);
  batch.set(doc(db, 'posts', postId, 'comments', commentId), {
    authorId: 'bob',
    authorName: 'Bob',
    text: 'nice',
    createdAt: serverTimestamp(),
    ...overrides,
  });
  batch.update(doc(db, 'posts', postId), { commentCount: increment(1) });
  return batch.commit();
}

describe('posts', () => {
  test('未ログインは読めない・作れない', async () => {
    await seedPost();
    await assertFails(getDoc(doc(guest(), 'posts', 'p1')));
    await assertFails(setDoc(doc(guest(), 'posts', 'p2'), newPost()));
  });

  test('ログインしていれば読める', async () => {
    await seedPost();
    await assertSucceeds(getDoc(doc(bob(), 'posts', 'p1')));
  });

  test('一覧は件数上限 100 件以下のクエリだけ取れる', async () => {
    await seedPost();
    const posts = collection(bob(), 'posts');
    await assertSucceeds(getDocs(query(posts, orderBy('geohash'), limit(100))));
    await assertFails(getDocs(query(posts, orderBy('geohash'), limit(101))));
    await assertFails(getDocs(query(posts, orderBy('createdAt', 'desc'))));
    await assertFails(getDocs(query(collection(guest(), 'posts'), limit(10))));
  });

  test('本人の名前で作れる', async () => {
    await assertSucceeds(setDoc(doc(alice(), 'posts', 'p1'), newPost()));
  });

  test('他人の名前では作れない', async () => {
    await assertFails(setDoc(doc(bob(), 'posts', 'p1'), newPost()));
  });

  test('本文が空・空白だけ・500 文字超は作れない', async () => {
    await assertFails(setDoc(doc(alice(), 'posts', 'p1'), newPost({ text: '' })));
    await assertFails(setDoc(doc(alice(), 'posts', 'p1'), newPost({ text: '   ' })));
    await assertFails(setDoc(doc(alice(), 'posts', 'p1'), newPost({ text: 'a'.repeat(501) })));
    await assertSucceeds(setDoc(doc(alice(), 'posts', 'p1'), newPost({ text: 'a'.repeat(500) })));
  });

  test('createdAt はサーバー時刻、commentCount は 0、位置は必須', async () => {
    await assertFails(setDoc(doc(alice(), 'posts', 'p1'), newPost({ createdAt: Timestamp.now() })));
    await assertFails(setDoc(doc(alice(), 'posts', 'p1'), newPost({ commentCount: 5 })));
    const { location, ...withoutLocation } = newPost();
    await assertFails(setDoc(doc(alice(), 'posts', 'p1'), withoutLocation));
  });

  test('画像は 4 枚まで、決められた項目以外は入れられない', async () => {
    await assertFails(setDoc(doc(alice(), 'posts', 'p1'), newPost({ imageUrls: ['a', 'b', 'c', 'd', 'e'] })));
    await assertFails(setDoc(doc(alice(), 'posts', 'p1'), newPost({ isAdmin: true })));
  });

  test('本文は誰も編集できない', async () => {
    await seedPost();
    await assertFails(updateDoc(doc(alice(), 'posts', 'p1'), { text: 'edited' }));
    await assertFails(updateDoc(doc(bob(), 'posts', 'p1'), { text: 'edited' }));
  });

  test('commentCount は 1 ずつしか増やせない', async () => {
    await seedPost();
    await assertFails(updateDoc(doc(bob(), 'posts', 'p1'), { commentCount: 10 }));
    await assertFails(updateDoc(doc(bob(), 'posts', 'p1'), { commentCount: increment(-1) }));
  });

  test('投稿者だけが削除できる', async () => {
    await seedPost();
    await assertFails(deleteDoc(doc(bob(), 'posts', 'p1')));
    await assertSucceeds(deleteDoc(doc(alice(), 'posts', 'p1')));
  });
});

// 匿名投稿と private/owner を 1 回のバッチで作る
function createAnonymousBatch(db, postId, { post = {}, ownerUid = 'alice', withOwner = true } = {}) {
  const batch = writeBatch(db);
  batch.set(doc(db, 'posts', postId), newPost({ authorId: '', authorName: '匿名', isAnonymous: true, ...post }));
  if (withOwner) batch.set(doc(db, 'posts', postId, 'private', 'owner'), { uid: ownerUid });
  return batch.commit();
}

// ルールを通さずに alice の匿名投稿を置く
async function seedAnonymousPost(id = 'p1') {
  await seedPost(id, { authorId: '', authorName: '匿名', isAnonymous: true });
  await env.withSecurityRulesDisabled(async (context) => {
    await setDoc(doc(context.firestore(), 'posts', id, 'private', 'owner'), { uid: 'alice' });
  });
}

function deleteAnonymousBatch(db, postId) {
  const batch = writeBatch(db);
  batch.delete(doc(db, 'posts', postId, 'private', 'owner'));
  batch.delete(doc(db, 'posts', postId));
  return batch.commit();
}

describe('anonymous posts', () => {
  test('private/owner と同じバッチなら匿名で作れる', async () => {
    await assertSucceeds(createAnonymousBatch(alice(), 'p1'));
  });

  test('uid・名前を入れたり、owner なし・他人の owner では匿名で作れない', async () => {
    await assertFails(createAnonymousBatch(alice(), 'p1', { post: { authorId: 'alice' } }));
    await assertFails(createAnonymousBatch(alice(), 'p1', { post: { authorName: 'Alice' } }));
    await assertFails(createAnonymousBatch(alice(), 'p1', { withOwner: false }));
    await assertFails(createAnonymousBatch(alice(), 'p1', { ownerUid: 'bob' }));
  });

  test('isAnonymous がない・名前つきの投稿に owner は作れない', async () => {
    const { isAnonymous, ...withoutFlag } = newPost();
    await assertFails(setDoc(doc(alice(), 'posts', 'p1'), withoutFlag));
    const db = alice();
    const batch = writeBatch(db);
    batch.set(doc(db, 'posts', 'p1'), newPost());
    batch.set(doc(db, 'posts', 'p1', 'private', 'owner'), { uid: 'alice' });
    await assertFails(batch.commit());
  });

  test('既存の投稿に後から owner を付けられない', async () => {
    await seedPost('p1', { authorId: '', authorName: '匿名', isAnonymous: true });
    await assertFails(setDoc(doc(bob(), 'posts', 'p1', 'private', 'owner'), { uid: 'bob' }));
  });

  test('owner は本人しか読めない', async () => {
    await seedAnonymousPost();
    await assertSucceeds(getDoc(doc(alice(), 'posts', 'p1', 'private', 'owner')));
    await assertFails(getDoc(doc(bob(), 'posts', 'p1', 'private', 'owner')));
    await assertFails(getDoc(doc(guest(), 'posts', 'p1', 'private', 'owner')));
  });

  test('本人だけが owner ごと削除でき、owner だけは消せない', async () => {
    await seedAnonymousPost();
    await assertFails(deleteAnonymousBatch(bob(), 'p1'));
    await assertFails(deleteDoc(doc(alice(), 'posts', 'p1', 'private', 'owner')));
    await assertSucceeds(deleteAnonymousBatch(alice(), 'p1'));
  });

  test('本人は他人のコメントごと匿名投稿を消せる', async () => {
    await seedAnonymousPost();
    await addCommentBatch(bob(), 'p1', 'c1');
    await assertFails(deleteDoc(doc(bob(), 'posts', 'p1', 'comments', 'c1')));
    const db = alice();
    const batch = writeBatch(db);
    batch.delete(doc(db, 'posts', 'p1', 'comments', 'c1'));
    batch.delete(doc(db, 'posts', 'p1', 'private', 'owner'));
    batch.delete(doc(db, 'posts', 'p1'));
    await assertSucceeds(batch.commit());
  });
});

describe('comments', () => {
  test('commentCount を同時に増やせば作れる', async () => {
    await seedPost();
    await assertSucceeds(addCommentBatch(bob(), 'p1', 'c1'));
  });

  test('commentCount を増やさないと作れない', async () => {
    await seedPost();
    await assertFails(setDoc(doc(bob(), 'posts', 'p1', 'comments', 'c1'), {
      authorId: 'bob',
      authorName: 'Bob',
      text: 'nice',
      createdAt: serverTimestamp(),
    }));
  });

  test('他人の名前・空の本文・存在しない投稿には作れない', async () => {
    await seedPost();
    await assertFails(addCommentBatch(bob(), 'p1', 'c1', { authorId: 'alice' }));
    await assertFails(addCommentBatch(bob(), 'p1', 'c1', { text: ' ' }));
    await assertFails(addCommentBatch(bob(), 'missing', 'c1'));
  });

  test('未ログインは読めない', async () => {
    await seedPost();
    await addCommentBatch(bob(), 'p1', 'c1');
    await assertFails(getDoc(doc(guest(), 'posts', 'p1', 'comments', 'c1')));
    await assertSucceeds(getDoc(doc(alice(), 'posts', 'p1', 'comments', 'c1')));
  });

  test('編集はできず、削除は投稿者だけ', async () => {
    await seedPost();
    await addCommentBatch(bob(), 'p1', 'c1');
    await assertFails(updateDoc(doc(bob(), 'posts', 'p1', 'comments', 'c1'), { text: 'edited' }));
    await assertFails(deleteDoc(doc(bob(), 'posts', 'p1', 'comments', 'c1')));
    await assertSucceeds(deleteDoc(doc(alice(), 'posts', 'p1', 'comments', 'c1')));
  });

  test('投稿者は他人のコメントごと投稿を 1 回のバッチで消せる', async () => {
    await seedPost();
    await addCommentBatch(bob(), 'p1', 'c1');
    const db = alice();
    const batch = writeBatch(db);
    batch.delete(doc(db, 'posts', 'p1', 'comments', 'c1'));
    batch.delete(doc(db, 'posts', 'p1'));
    await assertSucceeds(batch.commit());
  });
});

describe('users', () => {
  const profile = { displayName: 'Alice', photoUrl: null, bio: '' };

  test('自分のプロフィールだけ作成・更新できる', async () => {
    await assertSucceeds(setDoc(doc(alice(), 'users', 'alice'), profile));
    await assertSucceeds(setDoc(doc(alice(), 'users', 'alice'), { ...profile, bio: 'hi' }));
    await assertFails(setDoc(doc(bob(), 'users', 'alice'), profile));
  });

  test('決められた項目・長さ以外は保存できない', async () => {
    await assertFails(setDoc(doc(alice(), 'users', 'alice'), { ...profile, role: 'admin' }));
    await assertFails(setDoc(doc(alice(), 'users', 'alice'), { ...profile, bio: 'a'.repeat(201) }));
  });

  test('ログインしていれば読めるが、未ログインは読めない', async () => {
    await setDoc(doc(alice(), 'users', 'alice'), profile);
    await assertSucceeds(getDoc(doc(bob(), 'users', 'alice')));
    await assertFails(getDoc(doc(guest(), 'users', 'alice')));
  });

  test('削除はできない', async () => {
    await setDoc(doc(alice(), 'users', 'alice'), profile);
    await assertFails(deleteDoc(doc(alice(), 'users', 'alice')));
  });
});
