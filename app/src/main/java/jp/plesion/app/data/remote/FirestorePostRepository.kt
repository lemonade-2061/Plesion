package jp.plesion.app.data.remote

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import jp.plesion.app.core.location.GeoUtils
import jp.plesion.app.data.model.Comment
import jp.plesion.app.data.model.GeoPoint
import jp.plesion.app.data.model.Post
import jp.plesion.app.data.repository.PostRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

/**
 * Firestore の `posts` コレクションを使う実装。
 * 現在は getNearbyPosts のみ実装済み。AppContainer への差し替えは Firebase 接続後に行う。
 */
class FirestorePostRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : PostRepository {
    private val posts get() = firestore.collection("posts")

    override fun observeTimeline(): Flow<List<Post>> = TODO()

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

    override suspend fun getPost(postId: String): Result<Post> = TODO()

    // 保存時は GeoUtils.round で丸めた位置を location に、その geohash を geohash に入れる
    override suspend fun createPost(text: String, location: GeoPoint?): Result<Post> = TODO()

    override suspend fun deletePost(postId: String): Result<Unit> = TODO()

    override suspend fun getComments(postId: String): Result<List<Comment>> = TODO()
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
        // serverTimestamp の書き込み直後は null になりうるので見送る
        createdAt = getTimestamp("createdAt")?.toDate()?.time ?: return null,
        commentCount = getLong("commentCount")?.toInt() ?: 0,
    )
}
