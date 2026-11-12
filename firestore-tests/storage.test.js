// storage.rules のテスト。Emulator で動かす（README.md 参照）。
// Storage のルールは Firestore の投稿を見るので、Firestore の Emulator も一緒に使う。
import { readFileSync } from 'node:fs';
import { after, beforeEach, before, describe, test } from 'node:test';
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from '@firebase/rules-unit-testing';
import { doc, setDoc, setLogLevel } from 'firebase/firestore';
import { deleteObject, getMetadata, ref, uploadBytes } from 'firebase/storage';

// 拒否されるたびに出る警告ログを止める（テストの結果だけを見やすくする）
setLogLevel('silent');

let env;

before(async () => {
  env = await initializeTestEnvironment({
    projectId: 'demo-plesion',
    firestore: { rules: readFileSync(new URL('../firestore.rules', import.meta.url), 'utf8') },
    storage: { rules: readFileSync(new URL('../storage.rules', import.meta.url), 'utf8') },
  });
});

after(async () => {
  await env.cleanup();
});

beforeEach(async () => {
  await env.clearFirestore();
  await env.clearStorage();
});

const alice = () => env.authenticatedContext('alice').storage();
const bob = () => env.authenticatedContext('bob').storage();
const guest = () => env.unauthenticatedContext().storage();

const jpeg = { contentType: 'image/jpeg' };
const smallImage = () => new Uint8Array([0xff, 0xd8, 0xff, 0xd9]);

function upload(storage, path = 'posts/p1/images/0.jpg', data = smallImage(), metadata = jpeg) {
  return uploadBytes(ref(storage, path), data, metadata);
}

// ルールを通さずに投稿（と匿名なら private/owner）を置く
async function seedPost({ authorId = 'alice', isAnonymous = false } = {}) {
  await env.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await setDoc(doc(db, 'posts', 'p1'), { authorId, isAnonymous, imageUrls: [] });
    if (isAnonymous) await setDoc(doc(db, 'posts', 'p1', 'private', 'owner'), { uid: 'alice' });
  });
}

// ルールを通さずに画像を置く
async function seedImage() {
  await env.withSecurityRulesDisabled(async (context) => {
    await upload(context.storage());
  });
}

describe('post images', () => {
  test('投稿を作る前なら、ログインしていれば JPEG をアップロードできる', async () => {
    await assertSucceeds(upload(alice()));
    await assertSucceeds(upload(alice(), 'posts/p1/images/3.jpg'));
  });

  test('未ログイン・JPEG 以外・2MB 超・5 枚目以降はアップロードできない', async () => {
    await assertFails(upload(guest()));
    await assertFails(upload(alice(), undefined, undefined, { contentType: 'image/png' }));
    await assertFails(upload(alice(), undefined, new Uint8Array(2 * 1024 * 1024 + 1)));
    await assertFails(upload(alice(), 'posts/p1/images/4.jpg'));
    await assertFails(upload(alice(), 'posts/p1/images/0.png'));
    await assertFails(upload(alice(), 'posts/p1/other.jpg'));
  });

  test('投稿ができた後は、追加も差し替えもできない', async () => {
    await seedPost();
    await assertFails(upload(alice(), 'posts/p1/images/1.jpg'));
    await seedImage();
    await assertFails(upload(alice()));
  });

  test('ログインしていれば読めるが、未ログインは読めない', async () => {
    await seedImage();
    await assertSucceeds(getMetadata(ref(bob(), 'posts/p1/images/0.jpg')));
    await assertFails(getMetadata(ref(guest(), 'posts/p1/images/0.jpg')));
  });

  test('名前つきの投稿の画像は、投稿者だけが消せる', async () => {
    await seedPost();
    await seedImage();
    await assertFails(deleteObject(ref(bob(), 'posts/p1/images/0.jpg')));
    await assertSucceeds(deleteObject(ref(alice(), 'posts/p1/images/0.jpg')));
  });

  test('匿名投稿の画像は、private/owner の本人だけが消せる', async () => {
    await seedPost({ authorId: '', isAnonymous: true });
    await seedImage();
    await assertFails(deleteObject(ref(bob(), 'posts/p1/images/0.jpg')));
    await assertSucceeds(deleteObject(ref(alice(), 'posts/p1/images/0.jpg')));
  });

  test('投稿の作成に失敗した後の画像は消せる（後始末）', async () => {
    await seedImage();
    await assertSucceeds(deleteObject(ref(alice(), 'posts/p1/images/0.jpg')));
  });
});
