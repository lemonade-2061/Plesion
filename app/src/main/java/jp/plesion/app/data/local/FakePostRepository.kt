package jp.plesion.app.data.local

import jp.plesion.app.data.model.Comment
import jp.plesion.app.data.model.GeoPoint
import jp.plesion.app.data.model.Post
import jp.plesion.app.data.remote.filterNearby
import jp.plesion.app.data.repository.PostRepository
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.UUID

/** 【仮実装】メモリ上だけで動く。アプリを再起動すると作成した投稿は消える。 */
class FakePostRepository : PostRepository {
    private val posts = MutableStateFlow(SampleData.posts)
    private val comments = MutableStateFlow(SampleData.comments)
    /** Firestore の private/owner の代わり。自分の匿名投稿の ID。 */
    private val myAnonymousPostIds = mutableSetOf<String>()

    // 画面の動作確認用に、Firestore 実装と同じ絞り込み（距離・位置なし除外・新しい順）だけ再現する
    override suspend fun getNearbyPosts(center: GeoPoint, radiusMeters: Double): Result<List<Post>> =
        Result.success(filterNearby(posts.value, center, radiusMeters))

    override suspend fun getPost(postId: String): Result<Post> {
        val post = posts.value.find { it.id == postId }
            ?: return Result.failure(NoSuchElementException("Post not found: $postId"))
        return Result.success(post)
    }

    override suspend fun createPost(text: String, location: GeoPoint?, isAnonymous: Boolean): Result<Post> {
        if (text.isBlank()) return Result.failure(IllegalArgumentException("Post text is blank"))
        val post = Post(
            id = "${SampleData.ID_PREFIX}post-${UUID.randomUUID()}",
            authorId = if (isAnonymous) "" else SampleData.me.id,
            authorName = if (isAnonymous) Post.ANONYMOUS_NAME else SampleData.me.displayName,
            isAnonymous = isAnonymous,
            text = "${SampleData.TEXT_PREFIX} $text",
            location = location,
            createdAt = System.currentTimeMillis(),
        )
        if (isAnonymous) myAnonymousPostIds += post.id
        posts.value = posts.value + post
        return Result.success(post)
    }

    override suspend fun deletePost(postId: String): Result<Unit> {
        posts.value = posts.value.filterNot { it.id == postId }
        myAnonymousPostIds -= postId
        return Result.success(Unit)
    }

    override suspend fun isMyPost(post: Post): Result<Boolean> = Result.success(
        if (post.isAnonymous) post.id in myAnonymousPostIds else post.authorId == SampleData.me.id,
    )

    override suspend fun getComments(postId: String): Result<List<Comment>> =
        Result.success(comments.value.filter { it.postId == postId }.sortedBy { it.createdAt })

    override suspend fun addComment(postId: String, text: String): Result<Comment> {
        if (text.isBlank()) return Result.failure(IllegalArgumentException("Comment text is blank"))
        if (posts.value.none { it.id == postId }) {
            return Result.failure(NoSuchElementException("Post not found: $postId"))
        }
        val comment = Comment(
            id = "${SampleData.ID_PREFIX}comment-${UUID.randomUUID()}",
            postId = postId,
            authorId = SampleData.me.id,
            authorName = SampleData.me.displayName,
            text = "${SampleData.TEXT_PREFIX} $text",
            createdAt = System.currentTimeMillis(),
        )
        comments.value = comments.value + comment
        posts.value = posts.value.map {
            if (it.id == postId) it.copy(commentCount = it.commentCount + 1) else it
        }
        return Result.success(comment)
    }
}
