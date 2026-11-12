package jp.plesion.app.data.remote

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.storageMetadata
import jp.plesion.app.core.image.ImageCompressor
import jp.plesion.app.core.location.GeoUtils
import jp.plesion.app.data.model.Comment
import jp.plesion.app.data.model.GeoPoint
import jp.plesion.app.data.model.Post
import jp.plesion.app.data.repository.PostRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.GeoPoint as FirestoreGeoPoint

/** Firestore の `posts` コレクションを使う実装。項目は docs/DATA_CONTRACT.md §2 に合わせる。 */
class FirestorePostRepository(
    private val imageCompressor: ImageCompressor,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance(),
) : PostRepository {
    private val posts get() = firestore.collection("posts")

    private val jpegMetadata = storageMetadata { contentType = "image/jpeg" }

    // パスに uid を入れない。ダウンロード URL にパスが含まれ、匿名投稿の投稿者が分かってしまうため（DATA_CONTRACT §4）
    private fun DocumentReference.imageRef(index: Int): StorageReference =
        storage.reference.child("posts/$id/images/$index.jpg")

    // geohash の範囲で候補を取り、実際の距離で radiusMeters 以内に絞る
    // セキュリティルールが件数上限のない一覧取得を拒否するので、範囲ごとに limit を付ける
    override suspend fun getNearbyPosts(center: GeoPoint, radiusMeters: Double): Result<List<Post>> =
        runCatching {
            val snapshots = coroutineScope {
                GeoUtils.queryRanges(center, radiusMeters).map { range ->
                    async {
                        posts.orderBy("geohash")
                            .startAt(range.start)
                            .endAt(range.end)
                            .limit(NEARBY_QUERY_LIMIT)
                            .get()
                            .await()
                    }
                }.awaitAll()
            }
            val candidates = snapshots
                .flatMap { it.documents }
                .distinctBy { it.id }
                .mapNotNull { it.toPost() }
            filterNearby(candidates, center, radiusMeters)
        }

    // 名前つきは authorId で、匿名は private/owner のコレクショングループで探す（どちらもルールが自分の uid での絞り込みを求める）
    // 必要なインデックスは firestore.indexes.json
    override suspend fun getMyPosts(): Result<List<Post>> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Not signed in")
        coroutineScope {
            val named = async {
                posts.whereEqualTo("authorId", uid)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(MY_POSTS_LIMIT)
                    .get()
                    .await()
                    .documents
                    .mapNotNull { it.toPost() }
            }
            val anonymous = async {
                firestore.collectionGroup("private")
                    .whereEqualTo("uid", uid)
                    .limit(MY_POSTS_LIMIT)
                    .get()
                    .await()
                    .documents
                    .mapNotNull { it.reference.parent.parent }
                    .map { post -> async { post.get().await().takeIf { it.exists() }?.toPost() } }
                    .awaitAll()
                    .filterNotNull()
            }
            (named.await() + anonymous.await()).sortedByDescending { it.createdAt }
        }
    }

    override suspend fun getPost(postId: String): Result<Post> = runCatching {
        val snapshot = posts.document(postId).get().await()
        if (!snapshot.exists()) throw NoSuchElementException("Post not found: $postId")
        snapshot.toPost() ?: error("Post is malformed: $postId")
    }

    // 位置は GeoUtils.round で丸めてから保存し、geohash も丸めた位置から計算する（DATA_CONTRACT §4）
    // 匿名投稿は投稿に uid を入れず、private/owner に置く。セキュリティルールが同じバッチで作ることを求める
    // 画像は投稿より先にアップロードする（Storage のルールが、投稿がまだないときだけ追加を認めるため）
    override suspend fun createPost(
        text: String,
        location: GeoPoint?,
        isAnonymous: Boolean,
        imageUris: List<Uri>,
    ): Result<Post> = runCatching {
        validatePostText(text)
        requireNotNull(location) { "Post location is required" }
        require(imageUris.size <= Post.MAX_IMAGES) { "Too many images" }
        val user = auth.currentUser ?: error("Not signed in")
        val rounded = GeoUtils.round(location)
        val authorId = if (isAnonymous) "" else user.uid
        val authorName = if (isAnonymous) Post.ANONYMOUS_NAME else user.displayName.orEmpty()
        val document = posts.document()
        val uploaded = mutableListOf<StorageReference>()
        try {
            val imageUrls = imageUris.mapIndexed { index, uri ->
                val bytes = imageCompressor.compress(uri).getOrThrow()
                val ref = document.imageRef(index)
                ref.putBytes(bytes, jpegMetadata).await()
                uploaded += ref
                ref.downloadUrl.await().toString()
            }
            firestore.batch().apply {
                set(
                    document,
                    mapOf(
                        "authorId" to authorId,
                        "authorName" to authorName,
                        "isAnonymous" to isAnonymous,
                        "text" to text,
                        "imageUrls" to imageUrls,
                        "location" to FirestoreGeoPoint(rounded.latitude, rounded.longitude),
                        "geohash" to GeoUtils.geohash(rounded),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "commentCount" to 0,
                    ),
                )
                if (isAnonymous) set(document.owner(), mapOf("uid" to user.uid))
            }.commit().await()
            // createdAt はサーバー時刻が入るが、読み直さずに端末の時刻で返す（表示上の誤差は小さい）
            Post(
                id = document.id,
                authorId = authorId,
                authorName = authorName,
                isAnonymous = isAnonymous,
                text = text,
                imageUrls = imageUrls,
                location = rounded,
                createdAt = System.currentTimeMillis(),
            )
        } catch (e: Exception) {
            // 投稿を作れなかったときは、アップロード済みの画像を残さない（失敗しても投稿の失敗だけを返す）
            uploaded.forEach { runCatching { it.delete().await() } }
            throw e
        }
    }

    // コメントのサブコレクションは投稿を消しても残るので、先に消してから投稿を消す
    // private/owner は投稿と同じバッチでしか消せない（セキュリティルール）
    // 画像は Storage のルールが投稿の持ち主を確かめるので、投稿を消す前に消す
    override suspend fun deletePost(postId: String): Result<Unit> = runCatching {
        val post = posts.document(postId)
        val snapshot = post.get().await()
        val isAnonymous = snapshot.getBoolean("isAnonymous") == true
        val imageCount = (snapshot.get("imageUrls") as? List<*>)?.size ?: 0
        repeat(imageCount) { index ->
            try {
                post.imageRef(index).delete().await()
            } catch (e: StorageException) {
                // 途中で失敗して消し直す場合など、すでにないものは無視する
                if (e.errorCode != StorageException.ERROR_OBJECT_NOT_FOUND) throw e
            }
        }
        val comments = post.collection("comments").get().await()
        // 1 回のバッチは 500 件まで。最後のバッチで投稿と private/owner も消す
        val chunks = comments.documents.chunked(BATCH_LIMIT - 2)
        chunks.dropLast(1).forEach { chunk ->
            firestore.batch().apply { chunk.forEach { delete(it.reference) } }.commit().await()
        }
        firestore.batch().apply {
            chunks.lastOrNull()?.forEach { delete(it.reference) }
            if (isAnonymous) delete(post.owner())
            delete(post)
        }.commit().await()
    }

    // 匿名投稿の private/owner は本人しか読めないので、読めなければ自分の投稿ではない
    override suspend fun isMyPost(post: Post): Result<Boolean> = runCatching {
        val uid = auth.currentUser?.uid ?: return@runCatching false
        if (!post.isAnonymous) return@runCatching post.authorId == uid
        try {
            posts.document(post.id).owner().get().await().getString("uid") == uid
        } catch (e: FirebaseFirestoreException) {
            if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) false else throw e
        }
    }

    override suspend fun getComments(postId: String): Result<List<Comment>> = runCatching {
        posts.document(postId).collection("comments")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .get()
            .await()
            .documents
            .mapNotNull { it.toComment(postId) }
    }

    // コメントの追加と commentCount の更新は同じバッチで行う（セキュリティルールもそれを前提にしている）
    override suspend fun addComment(postId: String, text: String): Result<Comment> = runCatching {
        validatePostText(text)
        val user = auth.currentUser ?: error("Not signed in")
        val post = posts.document(postId)
        if (!post.get().await().exists()) throw NoSuchElementException("Post not found: $postId")
        val authorName = user.displayName.orEmpty()
        val comment = post.collection("comments").document()
        firestore.batch().apply {
            set(
                comment,
                mapOf(
                    "authorId" to user.uid,
                    "authorName" to authorName,
                    "text" to text,
                    "createdAt" to FieldValue.serverTimestamp(),
                ),
            )
            update(post, "commentCount", FieldValue.increment(1))
        }.commit().await()
        Comment(
            id = comment.id,
            postId = postId,
            authorId = user.uid,
            authorName = authorName,
            text = text,
            createdAt = System.currentTimeMillis(),
        )
    }
}

/** geohash の範囲 1 つあたりの取得件数。firestore.rules の一覧取得の上限と合わせる。 */
private const val NEARBY_QUERY_LIMIT = 100L

/** 自分の投稿の取得件数（名前つき・匿名それぞれ）。firestore.rules の一覧取得の上限と合わせる。 */
private const val MY_POSTS_LIMIT = 100L

private const val BATCH_LIMIT = 500

/** 匿名投稿の本人確認用ドキュメント（DATA_CONTRACT §4）。 */
private fun DocumentReference.owner(): DocumentReference = collection("private").document("owner")

/** 投稿・コメントの本文のルール（1〜[Post.MAX_TEXT_LENGTH] 文字、空白だけは不可）。セキュリティルールと合わせる。 */
internal fun validatePostText(text: String) {
    require(text.isNotBlank()) { "Post text is blank" }
    require(text.length <= Post.MAX_TEXT_LENGTH) { "Post text is too long" }
}

/** geohash の範囲は円より広いので、距離で絞り込んで新しい順に並べる。位置のない投稿は除く。 */
internal fun filterNearby(posts: List<Post>, center: GeoPoint, radiusMeters: Double): List<Post> =
    posts
        .filter { post -> post.location?.let { GeoUtils.distanceMeters(center, it) <= radiusMeters } ?: false }
        .sortedByDescending { it.createdAt }

/** 必須項目が欠けているドキュメントは null（表示しない）。 */
private fun DocumentSnapshot.toPost(): Post? {
    val location = getGeoPoint("location") ?: return null
    return Post(
        id = id,
        authorId = getString("authorId") ?: return null,
        authorName = getString("authorName") ?: return null,
        // この項目を追加する前の投稿にはないので、ないときは名前つきとして扱う
        isAnonymous = getBoolean("isAnonymous") ?: false,
        text = getString("text") ?: return null,
        imageUrls = (get("imageUrls") as? List<*>)?.filterIsInstance<String>().orEmpty(),
        location = GeoPoint(location.latitude, location.longitude),
        // 自分の書き込み直後はサーバー時刻がまだないので、端末側の見込みの時刻を使う
        createdAt = getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            ?.toDate()?.time ?: return null,
        commentCount = getLong("commentCount")?.toInt() ?: 0,
    )
}

private fun DocumentSnapshot.toComment(postId: String): Comment? = Comment(
    id = id,
    postId = postId,
    authorId = getString("authorId") ?: return null,
    authorName = getString("authorName") ?: return null,
    text = getString("text") ?: return null,
    createdAt = getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        ?.toDate()?.time ?: return null,
)
