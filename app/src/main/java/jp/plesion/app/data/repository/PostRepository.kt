package jp.plesion.app.data.repository

import jp.plesion.app.data.model.Comment
import jp.plesion.app.data.model.GeoPoint
import jp.plesion.app.data.model.Post
import kotlinx.coroutines.flow.Flow

interface PostRepository {
    /** タイムライン。新しい順。 */
    fun observeTimeline(): Flow<List<Post>>

    /** TODO(takuma): 1km 以内の検索。Firestore ではジオハッシュ等の工夫が必要（docs/ROADMAP.md 参照）。 */
    suspend fun getNearbyPosts(center: GeoPoint, radiusMeters: Double = 1_000.0): Result<List<Post>>

    suspend fun getPost(postId: String): Result<Post>

    suspend fun createPost(text: String, location: GeoPoint?): Result<Post>

    suspend fun deletePost(postId: String): Result<Unit>

    suspend fun getComments(postId: String): Result<List<Comment>>
}
