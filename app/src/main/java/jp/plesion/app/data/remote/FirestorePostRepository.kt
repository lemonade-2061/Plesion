package jp.plesion.app.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import jp.plesion.app.core.location.GeoUtils
import jp.plesion.app.data.model.Comment
import jp.plesion.app.data.model.GeoPoint
import jp.plesion.app.data.model.Post
import jp.plesion.app.data.repository.PostRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.GeoPoint as FirestoreGeoPoint

/** Firestore の `posts` コレクションを使う実装。項目は docs/DATA_CONTRACT.md §2 に合わせる。 */
class FirestorePostRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) : PostRepository {
    private val posts get() = firestore.collection("posts")

    override fun observeTimeline(): Flow<List<Post>> = callbackFlow {
        val registration = posts.orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(TIMELINE_LIMIT)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) trySend(snapshot.documents.mapNotNull { it.toPost() })
            }
        awaitClose { registration.remove() }
    }

    // geohash の範囲で候補を取り、実際の距離で radiusMeters 以内に絞る
    override suspend fun getNearbyPosts(center: GeoPoint, radiusMeters: Double): Result<List<Post>> =
        runCatching {
            val snapshots = coroutineScope {
                GeoUtils.queryRanges(center, radiusMeters).map { range ->
                    async {
                        posts.orderBy("geohash")
                            .startAt(range.start)
                            .endAt(range.end)
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

    override suspend fun getPost(postId: String): Result<Post> = runCatching {
        val snapshot = posts.document(postId).get().await()
        if (!snapshot.exists()) throw NoSuchElementException("Post not found: $postId")
        snapshot.toPost() ?: error("Post is malformed: $postId")
    }

    // 位置は GeoUtils.round で丸めてから保存し、geohash も丸めた位置から計算する（DATA_CONTRACT §4）
    override suspend fun createPost(text: String, location: GeoPoint?): Result<Post> = runCatching {
        validatePostText(text)
        requireNotNull(location) { "Post location is required" }
        val user = auth.currentUser ?: error("Not signed in")
        val rounded = GeoUtils.round(location)
        val authorName = user.displayName.orEmpty()
        val document = posts.document()
        document.set(
            mapOf(
                "authorId" to user.uid,
                "authorName" to authorName,
                "text" to text,
                "imageUrls" to emptyList<String>(),
                "location" to FirestoreGeoPoint(rounded.latitude, rounded.longitude),
                "geohash" to GeoUtils.geohash(rounded),
                "createdAt" to FieldValue.serverTimestamp(),
                "commentCount" to 0,
            ),
        ).await()
        // createdAt はサーバー時刻が入るが、読み直さずに端末の時刻で返す（表示上の誤差は小さい）
        Post(
            id = document.id,
            authorId = user.uid,
            authorName = authorName,
            text = text,
            location = rounded,
            createdAt = System.currentTimeMillis(),
        )
    }

    // コメントのサブコレクションは投稿を消しても残るので、先に消してから投稿を消す
    override suspend fun deletePost(postId: String): Result<Unit> = runCatching {
        val post = posts.document(postId)
        val comments = post.collection("comments").get().await()
        // 1 回のバッチは 500 件まで。最後のバッチで投稿も消す
        val chunks = comments.documents.chunked(BATCH_LIMIT - 1)
        chunks.dropLast(1).forEach { chunk ->
            firestore.batch().apply { chunk.forEach { delete(it.reference) } }.commit().await()
        }
        firestore.batch().apply {
            chunks.lastOrNull()?.forEach { delete(it.reference) }
            delete(post)
        }.commit().await()
    }

    override suspend fun getComments(postId: String): Result<List<Comment>> = TODO()

    override suspend fun addComment(postId: String, text: String): Result<Comment> = TODO()
}

/** タイムラインで一度に購読する件数。全件を購読し続けないよう上限を設ける。 */
private const val TIMELINE_LIMIT = 50L

private const val BATCH_LIMIT = 500

/** 本文のルール（1〜[Post.MAX_TEXT_LENGTH] 文字、空白だけは不可）。セキュリティルールと合わせる。 */
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
        text = getString("text") ?: return null,
        imageUrls = (get("imageUrls") as? List<*>)?.filterIsInstance<String>().orEmpty(),
        location = GeoPoint(location.latitude, location.longitude),
        // 自分の書き込み直後はサーバー時刻がまだないので、端末側の見込みの時刻を使う
        createdAt = getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            ?.toDate()?.time ?: return null,
        commentCount = getLong("commentCount")?.toInt() ?: 0,
    )
}
