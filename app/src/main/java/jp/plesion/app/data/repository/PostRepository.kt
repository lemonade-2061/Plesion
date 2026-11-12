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

    /** [isAnonymous] なら投稿者名を「匿名」にし、uid は本人しか読めない場所に保存する（DATA_CONTRACT §4）。 */
    suspend fun createPost(text: String, location: GeoPoint?, isAnonymous: Boolean = false): Result<Post>

    suspend fun deletePost(postId: String): Result<Unit>

    /**
     * ログイン中のユーザーの投稿か。編集・削除ボタンを出すかの判定に使う。
     * 匿名投稿は authorId が空なので、画面側で authorId と比べずにこれを使う。
     */
    suspend fun isMyPost(post: Post): Result<Boolean>

    /** 古い順。 */
    suspend fun getComments(postId: String): Result<List<Comment>>

    /** 投稿者はログイン中のユーザー。投稿の commentCount も 1 増やす。 */
    suspend fun addComment(postId: String, text: String): Result<Comment>
}
