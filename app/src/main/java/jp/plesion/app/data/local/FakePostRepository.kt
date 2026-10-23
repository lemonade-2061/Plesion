package jp.plesion.app.data.local

import jp.plesion.app.data.model.Comment
import jp.plesion.app.data.model.GeoPoint
import jp.plesion.app.data.model.Post
import jp.plesion.app.data.repository.PostRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** 【仮実装】メモリ上だけで動く。アプリを再起動すると作成した投稿は消える。 */
class FakePostRepository : PostRepository {
    private val posts = MutableStateFlow(SampleData.posts)

    override fun observeTimeline(): Flow<List<Post>> =
        posts.map { list -> list.sortedByDescending { it.createdAt } }

    // 距離計算はしない。位置検索の本実装は remote 層で行う。
    override suspend fun getNearbyPosts(center: GeoPoint, radiusMeters: Double): Result<List<Post>> =
        Result.success(posts.value)

    override suspend fun getPost(postId: String): Result<Post> {
        val post = posts.value.find { it.id == postId }
            ?: return Result.failure(NoSuchElementException("Post not found: $postId"))
        return Result.success(post)
    }

    override suspend fun createPost(text: String, location: GeoPoint?): Result<Post> {
        if (text.isBlank()) return Result.failure(IllegalArgumentException("Post text is blank"))
        val post = Post(
            id = "${SampleData.ID_PREFIX}post-${System.currentTimeMillis()}",
            authorId = SampleData.me.id,
            authorName = SampleData.me.displayName,
            text = "${SampleData.TEXT_PREFIX} $text",
            location = location,
            createdAt = System.currentTimeMillis(),
        )
        posts.value = posts.value + post
        return Result.success(post)
    }

    override suspend fun deletePost(postId: String): Result<Unit> {
        posts.value = posts.value.filterNot { it.id == postId }
        return Result.success(Unit)
    }

    override suspend fun getComments(postId: String): Result<List<Comment>> =
        Result.success(SampleData.comments.filter { it.postId == postId })
}
