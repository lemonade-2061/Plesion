package jp.plesion.app.data.repository

import jp.plesion.app.data.model.Comment
import jp.plesion.app.data.model.GeoPoint
import jp.plesion.app.data.model.Post

interface PostRepository {
    /**
     * [center] から [radiusMeters] 以内の投稿。新しい順。
     * 全件を取るタイムラインは位置情報の公開範囲を超えるので用意しない（DATA_CONTRACT §3）。
     */
    suspend fun getNearbyPosts(center: GeoPoint, radiusMeters: Double = 1_000.0): Result<List<Post>>

    suspend fun getPost(postId: String): Result<Post>

    suspend fun createPost(text: String, location: GeoPoint?): Result<Post>

    suspend fun deletePost(postId: String): Result<Unit>

    /** 古い順。 */
    suspend fun getComments(postId: String): Result<List<Comment>>

    /** 投稿者はログイン中のユーザー。投稿の commentCount も 1 増やす。 */
    suspend fun addComment(postId: String, text: String): Result<Comment>
}
